package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorNetworking;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client-only payload handlers for authoritative schematic preview downloads. */
public final class ConstructorClientNetworking {
    private ConstructorClientNetworking() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(ConstructorNetworking.PreviewBegin.TYPE, (payload, context) -> ClientSchematicPreviewCache.begin(payload));
        event.register(ConstructorNetworking.PreviewChunk.TYPE, (payload, context) -> ClientSchematicPreviewCache.chunk(payload));
        event.register(ConstructorNetworking.PreviewFinish.TYPE, (payload, context) -> ClientSchematicPreviewCache.finish(payload));
        event.register(ConstructorNetworking.PreviewError.TYPE, (payload, context) -> ClientSchematicPreviewCache.error(payload));
    }
}
