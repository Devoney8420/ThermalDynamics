package cofh.thermal.dynamics.test;

import cofh.thermal.dynamics.common.block.DuctBlock;
import cofh.thermal.dynamics.common.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.common.grid.fluid.FluidGridStorage;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cofh.lib.util.constants.BlockStatePropertiesCoFH.FACING_ALL;
import static cofh.lib.util.constants.NBTTags.TAG_CAPACITY;
import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

@Mod("thermal_dynamics_tests")
public final class DynamicsBlockStateTests {

    public DynamicsBlockStateTests(IEventBus bus) {
        bus.addListener(DynamicsBlockStateTests::gather);
    }

    private static void gather(GatherDataEvent event) {
        event.addProvider(new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput output) {
                return event.getLookupProvider().thenAccept(registries -> {
                    try {
                        verifyBlocks(event.getResourceManager(PackType.CLIENT_RESOURCES),
                                event.getResourceManager(PackType.SERVER_DATA), registries);
                        verifyFluidStorage(registries);
                        verifyGridSave(registries);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            }

            @Override
            public String getName() {
                return "Thermal Dynamics block states and serialization";
            }
        });
    }

    private static void verifyBlocks(ResourceManager assets, ResourceManager data, HolderLookup.Provider registries) throws IOException {
        Map<String, BlockEntityType<?>> blocks = Map.of(
                ID_ENERGY_DUCT, ENERGY_DUCT_BLOCK_ENTITY.get(),
                ID_FLUID_DUCT, FLUID_DUCT_BLOCK_ENTITY.get(),
                ID_FLUID_DUCT_WINDOWED, FLUID_DUCT_WINDOWED_BLOCK_ENTITY.get(),
                ID_ITEM_BUFFER, ITEM_BUFFER_BLOCK_ENTITY.get());
        DynamicOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        int states = 0;
        for (var entry : blocks.entrySet()) {
            String name = entry.getKey();
            var block = BuiltInRegistries.BLOCK.get(thermal(name));
            var type = entry.getValue();
            var variants = readJson(assets, "blockstates/" + name).getAsJsonObject().getAsJsonObject("variants");
            boolean duct = block instanceof DuctBlock;
            require(duct == !name.equals(ID_ITEM_BUFFER), "Unexpected block type for " + name);
            var possibleStates = block.getStateDefinition().getPossibleStates();
            require(possibleStates.size() == (duct ? 2 : 6), "Unexpected state count for " + name);
            if (duct) {
                require(!block.defaultBlockState().getValue(DuctBlock.WATERLOGGED), name + " should default to dry");
            } else {
                require(block.defaultBlockState().getValue(FACING_ALL) == Direction.UP, "Item buffer should default to facing up");
            }
            for (BlockState state : possibleStates) {
                if (duct) {
                    boolean flooded = state.getValue(DuctBlock.WATERLOGGED);
                    require(flooded ? state.getFluidState().isSourceOfType(Fluids.WATER) : state.getFluidState().isEmpty(),
                            "Incorrect fluid state for " + state);
                }
                String variant = duct ? "" : "facing=" + state.getValue(FACING_ALL).getSerializedName();
                require(variants.has(variant), "Missing model variant for " + state);
                var entity = type.create(BlockPos.ZERO, state);
                require(entity != null && entity.getType() == type && type.isValid(state),
                        "Wrong block entity for " + state);
                var saved = entity.saveWithoutMetadata(registries);
                entity.loadWithComponents(saved, registries);
                require(saved.equals(entity.saveWithoutMetadata(registries)), "Block entity data did not round-trip for " + state);
                ++states;
            }
            LootTable.DIRECT_CODEC.parse(ops, readJson(data, "loot_table/blocks/" + name))
                    .getOrThrow(error -> new AssertionError(name + " loot table: " + error));
        }
        List<String> recipes = List.of("energy_duct_4", "fluid_duct_4", "fluid_duct_windowed_4", "item_buffer",
                "energy_limiter_attachment_2", "filter_attachment_2", "servo_attachment_2", "turbo_servo_attachment_2");
        for (String recipe : recipes) {
            Recipe.CODEC.parse(ops, readJson(data, "recipe/" + recipe))
                    .getOrThrow(error -> new AssertionError(recipe + ": " + error));
        }
        System.out.println("Verified " + blocks.size() + " Dynamics blocks, " + states + " block states, and " + recipes.size() + " recipes.");
    }

    private static void verifyFluidStorage(HolderLookup.Provider registries) {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("FluidName", "minecraft:water");
        legacy.putInt("Amount", 400);
        legacy.putInt(TAG_CAPACITY, 1000);
        FluidGridStorage storage = new FluidGridStorage(1000);
        storage.deserializeNBT(registries, legacy);
        require(storage.getFluid().is(Fluids.WATER.getFluidType()) && storage.getFluid().getAmount() == 400,
                "Legacy fluid did not load");

        CompoundTag marker = new CompoundTag();
        marker.putString("test", "dynamics");
        FluidStack markedWater = new FluidStack(Fluids.WATER, 600);
        markedWater.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        require(storage.fill(markedWater, FluidAction.SIMULATE) == 0, "Incompatible fluid components were accepted");
        storage.setFluid(FluidStack.EMPTY);
        require(storage.fill(markedWater, FluidAction.EXECUTE) == 600, "Fluid fill did not move the expected amount");
        FluidGridStorage reloaded = new FluidGridStorage(1000);
        reloaded.deserializeNBT(registries, storage.serializeNBT(registries));
        require(reloaded.getFluid().getAmount() == 600 && FluidStack.isSameFluidSameComponents(markedWater, reloaded.getFluid()),
                "Fluid amount or components failed to survive serialization");
        FluidStack drained = reloaded.drain(200, FluidAction.EXECUTE);
        require(drained.getAmount() == 200 && FluidStack.isSameFluidSameComponents(markedWater, drained)
                && reloaded.getFluid().getAmount() == 400, "Drain lost fluid or components");
        System.out.println("Verified legacy fluid data and component-preserving fill, save, and drain.");
    }

    private static void verifyGridSave(HolderLookup.Provider registries) {
        BlockPos start = new BlockPos(16, 5, -3);
        BlockPos end = start.east();
        CompoundTag legacyPosition = new CompoundTag();
        legacyPosition.putInt("X", start.getX());
        legacyPosition.putInt("Y", start.getY());
        legacyPosition.putInt("Z", start.getZ());
        CompoundTag startNode = new CompoundTag();
        startNode.put("pos", legacyPosition);
        CompoundTag endNode = new CompoundTag();
        endNode.put("pos", NbtUtils.writeBlockPos(end));
        ListTag nodes = new ListTag();
        nodes.add(startNode);
        nodes.add(endNode);
        CompoundTag edge = new CompoundTag();
        edge.put("U", legacyPosition.copy());
        edge.put("V", NbtUtils.writeBlockPos(end));
        ListTag edges = new ListTag();
        edges.add(edge);
        CompoundTag incompleteEdge = new CompoundTag();
        incompleteEdge.put("U", legacyPosition.copy());
        edges.add(incompleteEdge);
        CompoundTag saved = new CompoundTag();
        saved.put("nodes", nodes);
        saved.put("edges", edges);

        var grid = new EnergyGrid(UUID.randomUUID(), null);
        grid.deserializeNBT(registries, saved);
        require(grid.getNodes().containsKey(start) && grid.getNodes().containsKey(end) && grid.isConnectedTo(start, end),
                "Legacy grid lost nodes or an edge");
        var reloaded = new EnergyGrid(UUID.randomUUID(), null);
        reloaded.deserializeNBT(registries, grid.serializeNBT(registries));
        require(reloaded.getNodes().containsKey(start) && reloaded.getNodes().containsKey(end) && reloaded.isConnectedTo(start, end),
                "Grid lost nodes or an edge on round-trip");
        System.out.println("Verified legacy grid coordinates and grid connectivity after save and reload.");
    }

    private static JsonElement readJson(ResourceManager resources, String path) throws IOException {
        try (var reader = resources.getResourceOrThrow(thermal(path + ".json")).openAsReader()) {
            return GsonHelper.parse(reader);
        }
    }

    private static ResourceLocation thermal(String path) {
        return ResourceLocation.fromNamespaceAndPath("thermal", path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
