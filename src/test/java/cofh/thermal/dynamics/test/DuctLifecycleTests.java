package cofh.thermal.dynamics.test;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.common.attachment.FluidServoAttachment;
import cofh.thermal.dynamics.common.block.entity.duct.DuctBlockEntity;
import cofh.thermal.dynamics.common.inventory.attachment.FluidServoAttachmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

@GameTestHolder("thermal_dynamics_tests")
@PrefixGameTestTemplate(false)
public final class DuctLifecycleTests {

    private static final BlockPos FIRST = new BlockPos(0, 1, 1);
    private static final BlockPos MIDDLE = FIRST.east();
    private static final BlockPos LAST = MIDDLE.east();

    @GameTest(template = "fluid_tanks")
    public static void gridlessDuctsCanBeBroken(GameTestHelper test) {

        var level = test.getLevel();
        var grids = IGridContainer.getGrid(level);
        for (var name : new String[]{ID_ENERGY_DUCT, ID_FLUID_DUCT, ID_FLUID_DUCT_WINDOWED}) {
            test.setBlock(FIRST, ductBlock(name));
            var old = duct(test, FIRST);
            grids.onDuctRemoved(old);
            old.setGrid(null);
            test.assertTrue(old.getGrid() == null, "Test duct must have no registered grid");

            placeNextTo(test, MIDDLE, FIRST, name);
            test.assertTrue(duct(test, MIDDLE).getGrid() != null, "A gridless neighbor prevented registration");
            var oldPos = test.absolutePos(FIRST);
            test.assertTrue(level.destroyBlock(oldPos, false), "Could not break gridless " + name);
            test.assertTrue(level.getBlockState(oldPos).isAir() && level.getBlockEntity(oldPos) == null && old.isRemoved(),
                    "Breaking a gridless duct left a block entity behind");

            placeNextTo(test, FIRST, MIDDLE, name);
            test.assertTrue(duct(test, FIRST).getGrid() == duct(test, MIDDLE).getGrid(), "Replacement duct failed to connect");
            level.destroyBlock(oldPos, false);
            level.destroyBlock(test.absolutePos(MIDDLE), false);
        }
        test.succeed();
    }

    @GameTest(template = "fluid_tanks")
    public static void breakingDuctSplitsGridAndReplacementRejoinsIt(GameTestHelper test) {

        var level = test.getLevel();
        for (var name : new String[]{ID_ENERGY_DUCT, ID_FLUID_DUCT, ID_FLUID_DUCT_WINDOWED}) {
            test.setBlock(FIRST, ductBlock(name));
            placeNextTo(test, MIDDLE, FIRST, name);
            placeNextTo(test, LAST, MIDDLE, name);
            var first = duct(test, FIRST);
            var last = duct(test, LAST);
            test.assertTrue(first.getGrid() != null && first.getGrid() == last.getGrid(), "Three ducts must share a grid");

            var old = duct(test, MIDDLE);
            var middlePos = test.absolutePos(MIDDLE);
            test.assertTrue(level.destroyBlock(middlePos, false), "Could not break the middle duct");
            test.assertTrue(level.getBlockState(middlePos).isAir() && level.getBlockEntity(middlePos) == null && old.isRemoved(),
                    "Breaking the middle duct left a block entity behind");
            test.assertTrue(first.getGrid() != null && last.getGrid() != null && first.getGrid() != last.getGrid(),
                    "Breaking the middle duct did not split the grid");

            placeNextTo(test, MIDDLE, FIRST, name);
            var middle = duct(test, MIDDLE);
            test.assertTrue(first.getGrid() == middle.getGrid() && last.getGrid() == middle.getGrid(),
                    "The replacement duct did not reconnect both sides");
            for (var pos : new BlockPos[]{FIRST, MIDDLE, LAST}) {
                level.destroyBlock(test.absolutePos(pos), false);
            }
        }
        test.succeed();
    }

    @GameTest(template = "fluid_tanks")
    public static void servoMenuReadsAttachmentFromDuctUpdate(GameTestHelper test) {

        var level = test.getLevel();
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        for (var name : new String[]{ID_FLUID_DUCT, ID_FLUID_DUCT_WINDOWED}) {
            var block = ductBlock(name);
            test.setBlock(FIRST, block);
            test.setBlock(LAST, block);
            var source = duct(test, FIRST);
            var destination = duct(test, LAST);
            test.assertTrue(source.attemptAttachmentInstall(Direction.UP, player, SERVO), "Could not install a servo");

            destination.handleUpdateTag(source.getUpdatePacket().getTag(), level.registryAccess());
            var menu = new FluidServoAttachmentMenu(1, level, test.absolutePos(LAST), Direction.UP, player.getInventory(), player);
            test.assertTrue(menu.attachment instanceof FluidServoAttachment && menu.getFilterSize() > 0,
                    "Servo update did not initialize the menu");
            level.destroyBlock(test.absolutePos(FIRST), false);
            level.destroyBlock(test.absolutePos(LAST), false);
        }
        test.succeed();
    }

    private static Block ductBlock(String name) {

        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thermal", name));
    }

    private static DuctBlockEntity<?, ?> duct(GameTestHelper test, BlockPos pos) {

        return (DuctBlockEntity<?, ?>) test.getLevel().getBlockEntity(test.absolutePos(pos));
    }

    private static void placeNextTo(GameTestHelper test, BlockPos target, BlockPos anchor, String name) {

        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ductBlock(name));
        var hand = InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, stack);
        var from = test.absolutePos(anchor);
        var to = test.absolutePos(target);
        var side = Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
        var hit = new BlockHitResult(Vec3.atCenterOf(from).add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5), side, from, false);
        test.assertTrue(stack.useOn(new UseOnContext(player, hand, hit)).consumesAction(), "Could not place " + name + " from item");
        test.assertTrue(test.getLevel().getBlockState(to).is(ductBlock(name)), "Duct was placed in the wrong position");
    }
}
