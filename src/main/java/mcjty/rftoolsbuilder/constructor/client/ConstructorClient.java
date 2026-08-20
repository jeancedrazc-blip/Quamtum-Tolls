package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = ConstructorBootstrap.MOD_ID, value = Dist.CLIENT)
public final class ConstructorClient {
    private ConstructorClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ConstructorBootstrap.CONSTRUCTOR_BLOCK_ENTITY.get(), ConstructorBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ConstructorBootstrap.CONSTRUCTOR_MENU.get(), ConstructorScreen::new);
        event.register(ConstructorBootstrap.SCHEMATIC_TABLE_MENU.get(), SchematicTableScreen::new);
        SchematicPlacementHandler.install();
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        SchematicPlacementHandler.registerKeys(event);
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        ConstructorClientNetworking.register(event);
    }
}
