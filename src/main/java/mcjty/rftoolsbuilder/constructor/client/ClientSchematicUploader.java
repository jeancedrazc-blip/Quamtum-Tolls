package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorNetworking;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicUploadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.ClientPacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

public final class ClientSchematicUploader {
    private ClientSchematicUploader() {}

    public static void upload(BlockPos tablePos, SchematicFolderIndex.Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> prepare(entry)).whenComplete((prepared, error) -> minecraft.execute(() -> {
            if (error != null || prepared == null) {
                message("Could not read schematic: " + (error == null ? "unknown error" : safeMessage(error)));
                return;
            }
            try {
                ClientPacketDistributor.sendToServer(new ConstructorNetworking.BeginUpload(
                        tablePos, entry.fileName(), entry.format().id(), prepared.bytes.length, prepared.sha256));

                for (int offset = 0; offset < prepared.bytes.length; offset += SchematicUploadManager.MAX_CHUNK_BYTES) {
                    int end = Math.min(prepared.bytes.length, offset + SchematicUploadManager.MAX_CHUNK_BYTES);
                    byte[] chunk = Arrays.copyOfRange(prepared.bytes, offset, end);
                    ClientPacketDistributor.sendToServer(new ConstructorNetworking.UploadChunk(tablePos, chunk));
                }
                ClientPacketDistributor.sendToServer(new ConstructorNetworking.FinishUpload(tablePos));
            } catch (RuntimeException exception) {
                ClientPacketDistributor.sendToServer(new ConstructorNetworking.CancelUpload(tablePos));
                message("Could not send schematic: " + safeMessage(exception));
            }
        }));
    }

    private static Prepared prepare(SchematicFolderIndex.Entry entry) {
        if (entry == null) throw new IllegalArgumentException("No schematic selected");
        Path path = SchematicFolderIndex.resolve(entry.fileName());
        if (path == null || !Files.isRegularFile(path)) throw new IllegalArgumentException("Schematic file is missing");
        try {
            long size = Files.size(path);
            if (size <= 0 || size > SchematicUploadManager.MAX_FILE_BYTES) {
                throw new IllegalArgumentException("Schematic exceeds the 32 MiB limit");
            }
            byte[] bytes = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new Prepared(bytes, HexFormat.of().formatHex(digest.digest(bytes)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void message(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(message), false);
        }
    }

    private static String safeMessage(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private record Prepared(byte[] bytes, String sha256) {}
}
