package mcjty.rftoolsbuilder.constructor;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ConstructorNetworking {
    private ConstructorNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToServer(BeginUpload.TYPE, BeginUpload.STREAM_CODEC, ConstructorNetworking::handleBegin);
        registrar.playToServer(UploadChunk.TYPE, UploadChunk.STREAM_CODEC, ConstructorNetworking::handleChunk);
        registrar.playToServer(FinishUpload.TYPE, FinishUpload.STREAM_CODEC, ConstructorNetworking::handleFinish);
        registrar.playToServer(CancelUpload.TYPE, CancelUpload.STREAM_CODEC, ConstructorNetworking::handleCancel);
        registrar.playToServer(SyncDeployment.TYPE, SyncDeployment.STREAM_CODEC, ConstructorNetworking::handleDeployment);
        registrar.playToServer(RequestPreview.TYPE, RequestPreview.STREAM_CODEC, ConstructorNetworking::handlePreviewRequest);
        registrar.playToServer(CancelPreview.TYPE, CancelPreview.STREAM_CODEC, ConstructorNetworking::handlePreviewCancel);

        // Handlers are intentionally client-only and are attached through
        // RegisterClientPayloadHandlersEvent so dedicated servers never load
        // Minecraft client classes.
        registrar.playToClient(PreviewBegin.TYPE, PreviewBegin.STREAM_CODEC);
        registrar.playToClient(PreviewChunk.TYPE, PreviewChunk.STREAM_CODEC);
        registrar.playToClient(PreviewFinish.TYPE, PreviewFinish.STREAM_CODEC);
        registrar.playToClient(PreviewError.TYPE, PreviewError.STREAM_CODEC);
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        return context.player() instanceof ServerPlayer player ? player : null;
    }

    private static void handleBegin(BeginUpload payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicUploadManager.begin(player, payload.tablePos(), payload.fileName(), payload.format(), payload.size(), payload.sha256());
    }

    private static void handleChunk(UploadChunk payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicUploadManager.chunk(player, payload.tablePos(), payload.data());
    }

    private static void handleFinish(FinishUpload payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicUploadManager.finish(player, payload.tablePos());
    }

    private static void handleCancel(CancelUpload payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicUploadManager.cancel(player, payload.tablePos(), false, "Upload cancelled");
    }

    private static void handleDeployment(SyncDeployment payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player == null) return;
        int slot = payload.slot();
        if (slot < 0 || slot >= 9 || slot != player.getInventory().getSelectedSlot()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(stack) || !SchematicCardItem.hasBounds(stack)) return;
        String expectedHash = SchematicCardItem.sha256(stack);
        if (!expectedHash.isBlank() && !expectedHash.equals(payload.sha256())) return;
        SchematicCardItem.setDeployment(stack, payload.anchor(), payload.rotation(), payload.mirror(), payload.deployed());
        player.getInventory().setChanged();
    }

    private static void handlePreviewRequest(RequestPreview payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicPreviewTransferManager.request(player, payload.slot(), payload.sha256());
    }

    private static void handlePreviewCancel(CancelPreview payload, IPayloadContext context) {
        ServerPlayer player = serverPlayer(context);
        if (player != null) SchematicPreviewTransferManager.cancel(player, payload.sha256());
    }

    public record BeginUpload(BlockPos tablePos, String fileName, String format, long size, String sha256) implements CustomPacketPayload {
        public static final Type<BeginUpload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_upload_begin"));
        public static final StreamCodec<ByteBuf, BeginUpload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BeginUpload::tablePos,
                ByteBufCodecs.stringUtf8(512), BeginUpload::fileName,
                ByteBufCodecs.stringUtf8(32), BeginUpload::format,
                ByteBufCodecs.VAR_LONG, BeginUpload::size,
                ByteBufCodecs.stringUtf8(64), BeginUpload::sha256,
                BeginUpload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UploadChunk(BlockPos tablePos, byte[] data) implements CustomPacketPayload {
        public static final Type<UploadChunk> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_upload_chunk"));
        public static final StreamCodec<ByteBuf, UploadChunk> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, UploadChunk::tablePos,
                ByteBufCodecs.byteArray(SchematicUploadManager.MAX_CHUNK_BYTES), UploadChunk::data,
                UploadChunk::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FinishUpload(BlockPos tablePos) implements CustomPacketPayload {
        public static final Type<FinishUpload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_upload_finish"));
        public static final StreamCodec<ByteBuf, FinishUpload> STREAM_CODEC = BlockPos.STREAM_CODEC.map(FinishUpload::new, FinishUpload::tablePos);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CancelUpload(BlockPos tablePos) implements CustomPacketPayload {
        public static final Type<CancelUpload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_upload_cancel"));
        public static final StreamCodec<ByteBuf, CancelUpload> STREAM_CODEC = BlockPos.STREAM_CODEC.map(CancelUpload::new, CancelUpload::tablePos);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncDeployment(int slot, BlockPos anchor, int rotation, int mirror, boolean deployed, String sha256) implements CustomPacketPayload {
        public static final Type<SyncDeployment> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_deployment"));
        public static final StreamCodec<ByteBuf, SyncDeployment> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncDeployment::slot,
                BlockPos.STREAM_CODEC, SyncDeployment::anchor,
                ByteBufCodecs.VAR_INT, SyncDeployment::rotation,
                ByteBufCodecs.VAR_INT, SyncDeployment::mirror,
                ByteBufCodecs.BOOL, SyncDeployment::deployed,
                ByteBufCodecs.stringUtf8(64), SyncDeployment::sha256,
                SyncDeployment::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestPreview(int slot, String sha256) implements CustomPacketPayload {
        public static final Type<RequestPreview> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_request"));
        public static final StreamCodec<ByteBuf, RequestPreview> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RequestPreview::slot,
                ByteBufCodecs.stringUtf8(64), RequestPreview::sha256,
                RequestPreview::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CancelPreview(String sha256) implements CustomPacketPayload {
        public static final Type<CancelPreview> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_cancel"));
        public static final StreamCodec<ByteBuf, CancelPreview> STREAM_CODEC = ByteBufCodecs.stringUtf8(64).map(CancelPreview::new, CancelPreview::sha256);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PreviewBegin(String sha256, String format, long size) implements CustomPacketPayload {
        public static final Type<PreviewBegin> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_begin"));
        public static final StreamCodec<ByteBuf, PreviewBegin> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), PreviewBegin::sha256,
                ByteBufCodecs.stringUtf8(32), PreviewBegin::format,
                ByteBufCodecs.VAR_LONG, PreviewBegin::size,
                PreviewBegin::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PreviewChunk(String sha256, byte[] data) implements CustomPacketPayload {
        public static final Type<PreviewChunk> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_chunk"));
        public static final StreamCodec<ByteBuf, PreviewChunk> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), PreviewChunk::sha256,
                ByteBufCodecs.byteArray(SchematicPreviewTransferManager.CHUNK_BYTES), PreviewChunk::data,
                PreviewChunk::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PreviewFinish(String sha256) implements CustomPacketPayload {
        public static final Type<PreviewFinish> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_finish"));
        public static final StreamCodec<ByteBuf, PreviewFinish> STREAM_CODEC = ByteBufCodecs.stringUtf8(64).map(PreviewFinish::new, PreviewFinish::sha256);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PreviewError(String sha256, String message) implements CustomPacketPayload {
        public static final Type<PreviewError> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_preview_error"));
        public static final StreamCodec<ByteBuf, PreviewError> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), PreviewError::sha256,
                ByteBufCodecs.stringUtf8(256), PreviewError::message,
                PreviewError::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
