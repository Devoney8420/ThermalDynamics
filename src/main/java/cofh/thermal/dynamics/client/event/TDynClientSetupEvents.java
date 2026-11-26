package cofh.thermal.dynamics.client.event;

import cofh.thermal.dynamics.client.DebugRenderer;
import cofh.thermal.dynamics.client.gui.ItemBufferScreen;
import cofh.thermal.dynamics.client.gui.attachment.EnergyLimiterAttachmentScreen;
import cofh.thermal.dynamics.client.gui.attachment.FluidFilterAttachmentScreen;
import cofh.thermal.dynamics.client.gui.attachment.FluidServoAttachmentScreen;
import cofh.thermal.dynamics.client.gui.attachment.FluidTurboServoAttachmentScreen;
import cofh.thermal.dynamics.client.model.DuctModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.dynamics.init.registries.TDynContainers.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

@EventBusSubscriber(value = Dist.CLIENT, modid = ID_THERMAL_DYNAMICS)
public class TDynClientSetupEvents {

    private TDynClientSetupEvents() {

    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {

        event.enqueueWork(() -> {
            RenderType cutout = RenderType.cutout();
            ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_ENERGY_DUCT), cutout);
            ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_FLUID_DUCT), cutout);
            ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_FLUID_DUCT_WINDOWED), cutout);
        });
        DebugRenderer.register();
    }

    @SubscribeEvent
    public static void registerGuiFactories(RegisterMenuScreensEvent event) {

        event.register(ITEM_BUFFER_CONTAINER.get(), ItemBufferScreen::new);

        event.register(ENERGY_LIMITER_ATTACHMENT_CONTAINER.get(), EnergyLimiterAttachmentScreen::new);
        event.register(FLUID_FILTER_ATTACHMENT_CONTAINER.get(), FluidFilterAttachmentScreen::new);
        event.register(FLUID_SERVO_ATTACHMENT_CONTAINER.get(), FluidServoAttachmentScreen::new);
        event.register(FLUID_TURBO_SERVO_ATTACHMENT_CONTAINER.get(), FluidTurboServoAttachmentScreen::new);
    }

    @SubscribeEvent
    public static void registerModels(final RegisterGeometryLoaders event) {

        event.register(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "duct"), new DuctModel.Loader());
    }

    @SubscribeEvent
    public static void postStitch(TextureAtlasStitchedEvent event) {

        DuctModel.clearCaches();
    }

}
