package mcjty.rftoolsbuilder.constructor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rate-limited server -> client schematic transfer used when a written card is
 * held by a client that no longer has the original local file.
 */
public final class SchematicPreviewTransferManager {
    public static final int CHUNK_BYTES = 24 * 1024;
    private static final int CHUNKS_PER_TICK = 4;

    private static final class Session {
        final UUID playerId;
        final String hash;
        final String format;
        final long totalBytes;
        final InputStream input;
        long sent;

        Session(UUID playerId, String hash, String format, long totalBytes, InputStream input) {
            this.playerId = playerId;
            this.hash = hash;
            this.format = format;
            this.totalBytes = totalBytes;
            this.input = input;
        }
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private SchematicPreviewTransferManager() {}

    public static void request(ServerPlayer player, int slot, String requestedHash) {
        cancel(player.getUUID(), null);

        if (slot < 0 || slot >= 9 || slot != player.getInventory().getSelectedSlot()) {
            fail(player, requestedHash, "Schematic card is not in the selected hotbar slot");
            return;
        }
        ItemStack card = player.getInventory().getItem(slot);
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card) || !SchematicCardItem.hasBounds(card)) {
            fail(player, requestedHash, "Selected item is not a written schematic card");
            return;
        }

        String hash = normalizeHash(SchematicCardItem.sha256(card));
        String requested = normalizeHash(requestedHash);
        if (hash == null || requested == null || !hash.equals(requested)) {
            fail(player, requestedHash, "Schematic card checksum does not match request");
            return;
        }

        SchematicFolderIndex.Format format = SchematicFolderIndex.Format.fromId(SchematicCardItem.sourceType(card));
        if (format == null) {
            fail(player, hash, "Schematic card format is unsupported");
            return;
        }

        Path path = SchematicFolderIndex.resolve(SchematicCardItem.sourceFile(card));
        Path internalRoot = SchematicFolderIndex.directory().resolve("quantumtools_uploaded").toAbsolutePath().normalize();
        if (path == null) {
            fail(player, hash, "Schematic file is unavailable on server");
            return;
        }
        path = path.toAbsolutePath().normalize();
        if (!path.startsWith(internalRoot) || !Files.isRegularFile(path)) {
            fail(player, hash, "Schematic card does not reference authoritative server storage");
            return;
        }

        try {
            long size = Files.size(path);
            if (size <= 0 || size > SchematicUploadManager.MAX_FILE_BYTES) {
                throw new IOException("Schematic is outside the 32 MiB transfer limit");
            }
            InputStream input = new BufferedInputStream(Files.newInputStream(path));
            Session session = new Session(player.getUUID(), hash, format.id(), size, input);
            ACTIVE.put(player.getUUID(), session);
            PacketDistributor.sendToPlayer(player, new ConstructorNetworking.PreviewBegin(hash, format.id(), size));
        } catch (IOException exception) {
            fail(player, hash, exception.getMessage() == null ? "Could not open schematic on server" : exception.getMessage());
        }
    }

    public static void cancel(ServerPlayer player, String hash) {
        cancel(player.getUUID(), normalizeHash(hash));
    }

    private static void cancel(UUID playerId, String hash) {
        Session active = ACTIVE.get(playerId);
        if (active == null || (hash != null && !hash.equals(active.hash))) return;
        ACTIVE.remove(playerId);
        close(active);
    }

    public static void serverTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();

        for (Session session : ACTIVE.values().toArray(Session[]::new)) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null || player.hasDisconnected()) {
                cancel(session.playerId, session.hash);
                continue;
            }

            try {
                for (int i = 0; i < CHUNKS_PER_TICK; i++) {
                    long remaining = session.totalBytes - session.sent;
                    if (remaining <= 0) {
                        finish(player, session);
                        break;
                    }
                    int wanted = (int) Math.min(CHUNK_BYTES, remaining);
                    byte[] buffer = session.input.readNBytes(wanted);
                    if (buffer.length <= 0) throw new IOException("Schematic stream ended before declared size");
                    session.sent += buffer.length;
                    PacketDistributor.sendToPlayer(player, new ConstructorNetworking.PreviewChunk(session.hash, buffer));
                }
            } catch (IOException exception) {
                ACTIVE.remove(session.playerId);
                close(session);
                fail(player, session.hash, exception.getMessage() == null ? "Schematic transfer failed" : exception.getMessage());
            }
        }
    }

    private static void finish(ServerPlayer player, Session session) {
        ACTIVE.remove(session.playerId);
        close(session);
        PacketDistributor.sendToPlayer(player, new ConstructorNetworking.PreviewFinish(session.hash));
    }

    private static void fail(ServerPlayer player, String hash, String message) {
        String safeHash = normalizeHash(hash);
        PacketDistributor.sendToPlayer(player, new ConstructorNetworking.PreviewError(safeHash == null ? "" : safeHash, message == null ? "Preview transfer failed" : message));
    }

    private static String normalizeHash(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        return normalized.matches("[0-9a-f]{64}") ? normalized : null;
    }

    private static void close(Session session) {
        if (session == null) return;
        try { session.input.close(); } catch (IOException ignored) {}
    }
}
