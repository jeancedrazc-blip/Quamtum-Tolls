package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorNetworking;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicPreviewTransferManager;
import mcjty.rftoolsbuilder.constructor.SchematicUploadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Client cache for authoritative schematic copies downloaded from the server. */
public final class ClientSchematicPreviewCache {
    private static String activeHash = "";
    private static SchematicFolderIndex.Format activeFormat;
    private static long totalBytes;
    private static long receivedBytes;
    private static Path tempPath;
    private static Path finalPath;
    private static OutputStream output;
    private static MessageDigest digest;
    private static String requestedHash = "";
    private static int requestSlot = -1;

    private ClientSchematicPreviewCache() {}

    public static boolean isDownloading(String hash) {
        return hash != null && !hash.isBlank() && (hash.equals(activeHash) || hash.equals(requestedHash));
    }

    public static long receivedBytes() { return receivedBytes; }
    public static long totalBytes() { return totalBytes; }

    public static void request(int selectedSlot, String hash) {
        String normalized = normalizeHash(hash);
        if (normalized == null || selectedSlot < 0 || selectedSlot >= 9) return;
        if (normalized.equals(requestedHash) || normalized.equals(activeHash)) return;
        cancelOutstanding();
        requestedHash = normalized;
        requestSlot = selectedSlot;
        ClientPacketDistributor.sendToServer(new ConstructorNetworking.RequestPreview(selectedSlot, normalized));
    }

    public static void cancelOutstanding() {
        String hash = !activeHash.isBlank() ? activeHash : requestedHash;
        if (!hash.isBlank()) ClientPacketDistributor.sendToServer(new ConstructorNetworking.CancelPreview(hash));
        reset(true);
    }

    public static void begin(ConstructorNetworking.PreviewBegin payload) {
        String hash = normalizeHash(payload.sha256());
        SchematicFolderIndex.Format format = SchematicFolderIndex.Format.fromId(payload.format());
        if (hash == null || format == null || payload.size() <= 0 || payload.size() > SchematicUploadManager.MAX_FILE_BYTES) {
            error(new ConstructorNetworking.PreviewError(hash == null ? "" : hash, "Invalid preview transfer metadata"));
            return;
        }
        if (!requestedHash.isBlank() && !requestedHash.equals(hash)) return;

        reset(false);
        requestedHash = hash;
        activeHash = hash;
        activeFormat = format;
        totalBytes = payload.size();
        receivedBytes = 0;

        try {
            Path cache = SchematicFolderIndex.cacheDirectory();
            Files.createDirectories(cache);
            finalPath = cache.resolve(hash + format.extension()).normalize();
            tempPath = cache.resolve("." + hash + format.extension() + ".part").normalize();
            if (!finalPath.startsWith(cache) || !tempPath.startsWith(cache)) throw new IOException("Invalid preview cache path");
            Files.deleteIfExists(tempPath);
            output = Files.newOutputStream(tempPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            digest = MessageDigest.getInstance("SHA-256");
        } catch (IOException | NoSuchAlgorithmException exception) {
            failLocal(hash, "Could not create schematic preview cache: " + safeMessage(exception));
        }
    }

    public static void chunk(ConstructorNetworking.PreviewChunk payload) {
        if (output == null || !payload.sha256().equals(activeHash)) return;
        byte[] data = payload.data();
        if (data == null || data.length == 0 || data.length > SchematicPreviewTransferManager.CHUNK_BYTES
                || receivedBytes + data.length > totalBytes) {
            failLocal(activeHash, "Invalid schematic preview chunk");
            return;
        }
        try {
            output.write(data);
            digest.update(data);
            receivedBytes += data.length;
        } catch (IOException exception) {
            failLocal(activeHash, "Could not write schematic preview cache: " + safeMessage(exception));
        }
    }

    public static void finish(ConstructorNetworking.PreviewFinish payload) {
        if (output == null || !payload.sha256().equals(activeHash)) return;
        String completedHash = activeHash;
        SchematicFolderIndex.Format completedFormat = activeFormat;
        Path completedPath = finalPath;
        try {
            output.close();
            output = null;
            if (receivedBytes != totalBytes) throw new IOException("Preview transfer ended before all bytes arrived");
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equals(completedHash)) throw new IOException("Preview SHA-256 mismatch");
            Files.move(tempPath, completedPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            reset(false);
            SchematicPlacementHandler.onPreviewCacheReady(completedHash, completedFormat, completedPath);
        } catch (IOException exception) {
            failLocal(completedHash, "Schematic preview rejected: " + safeMessage(exception));
        }
    }

    public static void error(ConstructorNetworking.PreviewError payload) {
        String hash = normalizeHash(payload.sha256());
        if (hash != null && !activeHash.isBlank() && !hash.equals(activeHash)) return;
        reset(true);
        message(payload.message().isBlank() ? "Schematic preview transfer failed" : payload.message());
        SchematicPlacementHandler.onPreviewCacheFailed(hash == null ? "" : hash);
    }

    /** Return cache path only when it exists; integrity is checked by the async loader before use. */
    public static Path cachePath(String hash, SchematicFolderIndex.Format format) {
        String normalized = normalizeHash(hash);
        if (normalized == null || format == null) return null;
        Path cache = SchematicFolderIndex.cacheDirectory();
        Path path = cache.resolve(normalized + format.extension()).normalize();
        return path.startsWith(cache) && Files.isRegularFile(path) ? path : null;
    }

    public static boolean verifySha256(Path path, String expectedHash) {
        String expected = normalizeHash(expectedHash);
        if (path == null || expected == null || !Files.isRegularFile(path)) return false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) md.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest()).equals(expected);
        } catch (IOException | NoSuchAlgorithmException exception) {
            return false;
        }
    }

    /** Convert an absolute cache file into the entry path accepted by the universal loader. */
    public static SchematicFolderIndex.Entry cacheEntry(Path path, SchematicFolderIndex.Format format) {
        if (path == null || format == null) return null;
        try {
            Path root = SchematicFolderIndex.directory();
            Path absolute = path.toAbsolutePath().normalize();
            if (!absolute.startsWith(root)) return null;
            String relative = root.relativize(absolute).toString().replace('\\', '/');
            return new SchematicFolderIndex.Entry(relative, format);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void failLocal(String hash, String reason) {
        if (hash != null && !hash.isBlank()) ClientPacketDistributor.sendToServer(new ConstructorNetworking.CancelPreview(hash));
        reset(true);
        message(reason);
        SchematicPlacementHandler.onPreviewCacheFailed(hash == null ? "" : hash);
    }

    private static void reset(boolean deleteTemp) {
        if (output != null) {
            try { output.close(); } catch (IOException ignored) {}
        }
        output = null;
        digest = null;
        if (deleteTemp && tempPath != null) {
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
        }
        activeHash = "";
        activeFormat = null;
        totalBytes = 0;
        receivedBytes = 0;
        tempPath = null;
        finalPath = null;
        requestedHash = "";
        requestSlot = -1;
    }

    private static String normalizeHash(String hash) {
        if (hash == null) return null;
        String value = hash.trim().toLowerCase();
        return value.matches("[0-9a-f]{64}") ? value : null;
    }

    private static String safeMessage(Exception exception) {
        String value = exception.getMessage();
        return value == null || value.isBlank() ? exception.getClass().getSimpleName() : value;
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.getChat().addClientSystemMessage(Component.literal(text));
    }
}
