package cofh.thermal.dynamics;

import cofh.lib.util.DeferredRegisterCoFH;
import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.DuctBlockEntity;
import cofh.thermal.dynamics.common.event.GridEvents;
import cofh.thermal.dynamics.common.network.PacketHandler;
import cofh.thermal.dynamics.init.registries.*;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cofh.lib.util.FlagManager.setFlag;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_GRID_TYPE;
import static cofh.thermal.lib.util.ThermalFlags.FLAG_XP_STORAGE_AUGMENT;
import static cofh.thermal.lib.util.ThermalIDs.ID_DEVICE_COLLECTOR;
import static cofh.thermal.lib.util.ThermalIDs.ID_DEVICE_NULLIFIER;
import static cofh.thermal.dynamics.util.TDynUtil.uncheckedCast;

@Mod (ID_THERMAL_DYNAMICS)
public class ThermalDynamics {

    public static final Logger LOG = LogManager.getLogger(ID_THERMAL_DYNAMICS);

    public static final ResourceLocation GRID_REGISTRY_LOC = ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, ID_GRID_TYPE);
    public static final DeferredRegisterCoFH<IGridType<?>> GRIDS = DeferredRegisterCoFH.create(GRID_REGISTRY_LOC, ID_THERMAL_DYNAMICS);

    public static final Registry<IGridType<?>> GRID_TYPE_REGISTRY = GRIDS.makeRegistry(e -> e.sync(false));

    public ThermalDynamics(ModContainer modContainer, IEventBus modEventBus) {

        setFeatureFlags();

        modEventBus.addListener(this::capSetup);

        modEventBus.addListener(PacketHandler::registerNetworking);

        GRIDS.register(modEventBus);

        TDynBlocks.register();
        TDynItems.register();
        TDynGrids.register();

        TDynContainers.register();
        TDynBlockEntities.register();

        GridEvents.register();
    }

    private void setFeatureFlags() {

        setFlag(ID_DEVICE_COLLECTOR, true);
        setFlag(ID_DEVICE_NULLIFIER, true);

        setFlag(FLAG_XP_STORAGE_AUGMENT, true);
    }

    // region INITIALIZATION
    private void capSetup(RegisterCapabilitiesEvent event) {

        event.registerBlockEntity(TDynApi.GRID_HOST_CAPABILITY, ENERGY_DUCT_BLOCK_ENTITY.get(), (tile, ctx) -> tile);
        event.registerBlockEntity(TDynApi.GRID_HOST_CAPABILITY, FLUID_DUCT_BLOCK_ENTITY.get(), (tile, ctx) -> tile);
        event.registerBlockEntity(TDynApi.GRID_HOST_CAPABILITY, FLUID_DUCT_WINDOWED_BLOCK_ENTITY.get(), (tile, ctx) -> tile);

        registerPassthroughCapability(event, Capabilities.EnergyStorage.BLOCK, uncheckedCast(ENERGY_DUCT_BLOCK_ENTITY.get()));
        registerPassthroughCapability(event, Capabilities.FluidHandler.BLOCK, uncheckedCast(FLUID_DUCT_BLOCK_ENTITY.get()));
        registerPassthroughCapability(event, Capabilities.FluidHandler.BLOCK, uncheckedCast(FLUID_DUCT_WINDOWED_BLOCK_ENTITY.get()));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_BUFFER_BLOCK_ENTITY.get(), ItemBufferBlockEntity::getItemHandler);
    }

    private <T> void registerPassthroughCapability(RegisterCapabilitiesEvent event, BlockCapability<T, Direction> cap, BlockEntityType<DuctBlockEntity<?, ?>> type) {

        event.registerBlockEntity(cap, type, (tile, ctx) -> tile.getCapability(cap, ctx));
    }
    // endregion

}
