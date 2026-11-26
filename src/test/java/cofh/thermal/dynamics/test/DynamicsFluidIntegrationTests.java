package cofh.thermal.dynamics.test;

import cofh.thermal.dynamics.common.attachment.FluidServoAttachment;
import cofh.thermal.dynamics.common.attachment.FluidTurboServoAttachment;
import cofh.thermal.dynamics.common.attachment.IAttachment;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static cofh.thermal.dynamics.init.registries.TDynIDs.SERVO;
import static cofh.thermal.dynamics.init.registries.TDynIDs.TURBO_SERVO;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

@GameTestHolder("thermal_dynamics_tests")
@PrefixGameTestTemplate(false)
public final class DynamicsFluidIntegrationTests {

    private static final int SOURCE_VOLUME = 4_000;
    private static final ResourceLocation MEKANISM_TANK = ResourceLocation.fromNamespaceAndPath("mekanism", "basic_fluid_tank");
    private static final ResourceLocation PNEUMATICCRAFT_TANK = ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "small_tank");
    private static final Direction SOURCE_SIDE = Direction.WEST;
    private static final Direction RECEIVER_SIDE = Direction.EAST;
    private static final BlockPos SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos INLET = SOURCE.east();
    private static final BlockPos OUTLET = INLET.east();
    private static final BlockPos RECEIVER = OUTLET.east();

    @GameTest(template = "fluid_tanks", timeoutTicks = 160)
    public static void regularServoTransfersBetweenMekanismTanks(GameTestHelper test) {
        checkTransfer(test, MEKANISM_TANK, MEKANISM_TANK, SERVO, FluidServoAttachment.class, 100, 950);
    }

    @GameTest(template = "fluid_tanks", timeoutTicks = 160)
    public static void turboServoTransfersBetweenMekanismTanks(GameTestHelper test) {
        checkTransfer(test, MEKANISM_TANK, MEKANISM_TANK, TURBO_SERVO, FluidTurboServoAttachment.class, 1_000, SOURCE_VOLUME);
    }

    @GameTest(template = "fluid_tanks", timeoutTicks = 160)
    public static void regularServoTransfersFromPneumaticCraftToMekanism(GameTestHelper test) {
        checkTransfer(test, PNEUMATICCRAFT_TANK, MEKANISM_TANK, SERVO, FluidServoAttachment.class, 100, 950);
    }

    @GameTest(template = "fluid_tanks", timeoutTicks = 160)
    public static void turboServoTransfersFromMekanismToPneumaticCraft(GameTestHelper test) {
        checkTransfer(test, MEKANISM_TANK, PNEUMATICCRAFT_TANK, TURBO_SERVO, FluidTurboServoAttachment.class, 1_000, SOURCE_VOLUME);
    }

    private static void checkTransfer(GameTestHelper test, ResourceLocation sourceId, ResourceLocation receiverId, String attachmentId,
            Class<? extends IAttachment> attachmentClass, int earlyMin, int earlyMax) {
        var sourceTank = BuiltInRegistries.BLOCK.get(sourceId);
        var receiverTank = BuiltInRegistries.BLOCK.get(receiverId);
        test.assertTrue(sourceTank != Blocks.AIR && receiverTank != Blocks.AIR, "Missing test tank " + sourceId + " or " + receiverId);
        test.setBlock(SOURCE, sourceTank);
        test.setBlock(RECEIVER, receiverTank);
        test.setBlock(INLET, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thermal", "fluid_duct")));
        test.setBlock(OUTLET, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thermal", "fluid_duct_windowed")));
        var inlet = (FluidDuctBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(INLET));
        var outlet = (FluidDuctBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(OUTLET));
        test.assertTrue(inlet != null && outlet != null && inlet.getGrid() != null && inlet.getGrid() == outlet.getGrid(),
                "The opaque and windowed ducts did not join the same fluid grid");
        var source = handler(test, SOURCE, SOURCE_SIDE.getOpposite());
        var receiver = handler(test, RECEIVER, RECEIVER_SIDE.getOpposite());
        test.assertValueEqual(source.fill(new FluidStack(Fluids.WATER, SOURCE_VOLUME), EXECUTE), SOURCE_VOLUME, "source fill");
        test.assertTrue(source.drain(1, SIMULATE).is(Fluids.WATER.getFluidType()), "Source face cannot export water");
        test.assertValueEqual(receiver.fill(new FluidStack(Fluids.WATER, 1), SIMULATE), 1, "receiver face accepts water");
        handler(test, INLET, SOURCE_SIDE);
        handler(test, OUTLET, RECEIVER_SIDE);

        test.runAfterDelay(8, () -> {
            test.assertValueEqual(receiver.getFluidInTank(0).getAmount(), 0, "receiver before servo installation");
            test.assertValueEqual(source.getFluidInTank(0).getAmount(), SOURCE_VOLUME, "source before servo installation");
            var player = test.makeMockPlayer(GameType.CREATIVE);
            test.assertTrue(inlet.attemptAttachmentInstall(SOURCE_SIDE, player, attachmentId), "Attachment did not install");
            test.assertTrue(attachmentClass.isInstance(inlet.getAttachment(SOURCE_SIDE)), "Wrong attachment was installed");
            test.runAfterDelay(8, () -> {
                int received = receiver.getFluidInTank(0).getAmount();
                test.assertTrue(received >= earlyMin && received <= earlyMax,
                        attachmentId + " delivered " + received + " mB after eight ticks, source=" + source.getFluidInTank(0).getAmount()
                                + ", grid=" + inlet.getGrid().getFluidAmount() + ", source node=" + inlet.getNode().getConnections()
                                + ", receiver node=" + outlet.getNode().getConnections()
                                + ", source connection=" + inlet.getConnectionType(SOURCE_SIDE)
                                + ", receiver connection=" + outlet.getConnectionType(RECEIVER_SIDE));
                System.out.println(attachmentId + " delivered " + received + " mB in eight ticks from " + sourceId + " to " + receiverId);
            });
            test.runAfterDelay(110, () -> {
                var stored = receiver.getFluidInTank(0);
                test.assertTrue(stored.is(Fluids.WATER.getFluidType()), "Receiver did not get water");
                test.assertValueEqual(stored.getAmount(), SOURCE_VOLUME, "receiver volume");
                test.assertTrue(source.getFluidInTank(0).isEmpty() && inlet.getGrid().getFluid().isEmpty(),
                        "Water remains in the source or the ducts");
                test.succeed();
            });
        });
    }

    private static IFluidHandler handler(GameTestHelper test, BlockPos pos, Direction face) {
        var handler = test.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, test.absolutePos(pos), face);
        test.assertTrue(handler != null && handler.getTanks() > 0, "Missing fluid capability at " + pos + " on " + face);
        return handler;
    }
}
