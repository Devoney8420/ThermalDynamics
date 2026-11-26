package cofh.thermal.dynamics.common.block.entity.duct;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.thermal.dynamics.api.grid.IGridHostLuminous;
import cofh.thermal.dynamics.api.grid.IGridHostUpdateable;
import cofh.thermal.dynamics.common.grid.fluid.FluidGrid;
import cofh.lib.util.helpers.StackData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.constants.NBTTags.TAG_RENDER_FLUID;
import static cofh.thermal.core.client.ThermalTextures.BLANK_TEXTURE;
import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.FLUID_DUCT_WINDOWED_BLOCK_ENTITY;

public class FluidDuctWindowedBlockEntity extends FluidDuctBlockEntity implements IGridHostUpdateable, IGridHostLuminous, IPacketHandlerTile {

    FluidStack renderFluid = FluidStack.EMPTY;

    public FluidDuctWindowedBlockEntity(BlockPos pos, BlockState state) {

        super(FLUID_DUCT_WINDOWED_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void update() {

        TileStatePacket.sendToClient(this);
    }

    @Override
    public int getLightValue() {

        return FluidHelper.luminosity(renderFluid);
    }

    @Nonnull
    @Override
    public ModelData getModelData() {

        modelData.setFill(renderFluid.isEmpty() ? BLANK_TEXTURE : RenderHelper.getFluidTexture(renderFluid).contents().name());
        modelData.setFillColor(FluidHelper.color(renderFluid));
        return super.getModelData();
    }

    // region NBT
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {

        if (!renderFluid.isEmpty()) {
            tag.put(TAG_RENDER_FLUID, StackData.writeFluid(registries, renderFluid, new CompoundTag()));
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);

        renderFluid = StackData.readFluid(registries, tag.getCompound(TAG_RENDER_FLUID));
    }
    // endregion

    // region NETWORK
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    // STATE
    @Override
    public RegistryFriendlyByteBuf getStatePacket(RegistryFriendlyByteBuf buffer) {

        FluidGrid grid = getGrid();
        renderFluid = grid == null ? FluidStack.EMPTY : grid.getRenderFluid();
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, renderFluid);

        super.getStatePacket(buffer);

        return buffer;
    }

    @Override
    public void handleStatePacket(RegistryFriendlyByteBuf buffer) {

        int prevLight = getLightValue();
        renderFluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer);

        if (prevLight != getLightValue()) {
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
        super.handleStatePacket(buffer);
    }
    // endregion
}
