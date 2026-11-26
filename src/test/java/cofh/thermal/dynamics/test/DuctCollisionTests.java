package cofh.thermal.dynamics.test;

import cofh.lib.api.item.IColorableItem;
import cofh.thermal.dynamics.common.block.DuctBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

@GameTestHolder("thermal_dynamics_tests")
@PrefixGameTestTemplate(false)
public final class DuctCollisionTests {

    private static final double DUCT_TOP = 11.5 / 16;

    @GameTest(template = "fluid_tanks")
    public static void playerCanLandAndWalkOnFluxducts(GameTestHelper test) {
        assertWalkable(test, ID_ENERGY_DUCT, false);
        test.succeed();
    }

    @GameTest(template = "fluid_tanks")
    public static void playerCanLandAndWalkOnWaterloggedFluidDucts(GameTestHelper test) {
        assertWalkable(test, ID_FLUID_DUCT_WINDOWED, true);
        test.succeed();
    }

    @GameTest(template = "fluid_tanks")
    public static void uninitializedFluxductDoesNotCreateInvisibleArms(GameTestHelper test) {
        var duct = (DuctBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thermal", ID_ENERGY_DUCT));
        var state = duct.defaultBlockState();
        var tile = duct.newBlockEntity(BlockPos.ZERO, state);
        var chunkBeforeAttachment = new BlockGetter() {
            @Override
            public BlockEntity getBlockEntity(BlockPos pos) {
                return tile;
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return state;
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return state.getFluidState();
            }

            @Override
            public int getHeight() {
                return test.getLevel().getHeight();
            }

            @Override
            public int getMinBuildHeight() {
                return test.getLevel().getMinBuildHeight();
            }
        };
        var collision = state.getCollisionShape(chunkBeforeAttachment, BlockPos.ZERO);
        test.assertValueEqual(collision.max(Direction.Axis.Y), DUCT_TOP, "fluxduct collision height before topology is available");
        test.succeed();
    }

    @GameTest(template = "fluid_tanks")
    public static void satchelIconColorsStayOpaque(GameTestHelper test) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thermal", "satchel"));
        var stack = new ItemStack(item);
        var colorable = (IColorableItem) item;
        test.assertValueEqual(FastColor.ARGB32.alpha(colorable.getColor(stack, 0)), 255, "undyed satchel opacity");
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x3355AA, true));
        test.assertValueEqual(colorable.getColor(stack, 0), 0xFF3355AA, "dyed satchel color and opacity");
        test.assertValueEqual(colorable.getColor(stack, 1), 0xFFFFFFFF, "untinted clasp opacity");
        test.succeed();
    }

    private static void assertWalkable(GameTestHelper test, String name, boolean waterlogged) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thermal", name));
        var state = block.defaultBlockState().setValue(DuctBlock.WATERLOGGED, waterlogged);
        BlockPos start = new BlockPos(0, 1, 1);
        test.setBlock(start, state);
        test.setBlock(start.east(), state);
        BlockPos absolute = test.absolutePos(start);
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 1.25, absolute.getZ() + 0.5);
        player.move(MoverType.SELF, new Vec3(0, -1.5, 0));
        test.assertTrue(player.onGround() && Math.abs(player.getY() - absolute.getY() - DUCT_TOP) < 1e-6,
                "Player fell through " + name + " or landed at the wrong height: " + (player.getY() - absolute.getY()));
        player.move(MoverType.SELF, new Vec3(1, -0.1, 0));
        test.assertTrue(player.onGround() && Math.abs(player.getX() - absolute.getX() - 1.5) < 1e-6,
                "Player could not walk across the junction between " + name + " blocks");
    }
}
