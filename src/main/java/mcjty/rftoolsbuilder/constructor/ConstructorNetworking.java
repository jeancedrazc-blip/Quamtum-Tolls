package mcjty.rftoolsbuilder.constructor;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ConstructorNetworking {
    private ConstructorNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(BeginUpload.TYPE, BeginUpload.STREAM_CODEC, ConstructorNetworking::handleBegin);
        registrar.playToServer(UploadChunk.TYPE, UploadChunk.STREAM_CODEC, ConstructorNetworking::handleChunk);
        registrar.playToServer(FinishUpload.TYPE, FinishUpload.STREAM_CODEC, ConstructorNetworking::handleFinish);
        registrar.playToServer(CancelUpload.TYPE, CancelUpload.STREAM_CODEC, ConstructorNetworking::handleCancel);
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

    public record BeginUpload(BlockPos tablePos, String fileName, String format, long size, String sha256) implements CustomPacketPayload {
        public static final Type<BeginUpload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ConstructorBootstrap.MOD_ID, "schematic_upload_begin"));
        public static final StreamCodec<ByteBuf, BeginUpload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BeginUpload::tablePos,
                ByteBufCodecs.stringUtf8(260), BeginUpload::fileName,
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
}
