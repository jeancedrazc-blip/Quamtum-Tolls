package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntityEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full format-neutral loader used by the table preview and Constructor.
 * SchematicPlanLoader remains the block decoder; this layer forces a full-air
 * decode first so source bounds are not lost, normalizes BlockEntity payloads,
 * and imports schematic entities into the same coordinate space.
 */
public final class UniversalSchematicLoader {
    private static final long MAX_NBT_BYTES = 0x20000000L;

    private UniversalSchematicLoader() {}

    public static ConstructionPlan load(SchematicFolderIndex.Entry entry, boolean includeAir) throws IOException {
        if (entry == null) throw new IOException("No schematic selected");

        // Full-air decode preserves the declared cuboid even when the outer layers are empty.
        ConstructionPlan decoded = SchematicPlanLoader.load(entry, true);
        CompoundTag root = readCompressed(entry.fileName());
        List<ConstructionEntityEntry> entities = readEntities(entry.format(), root, decoded);
        Map<BlockPos, CompoundTag> supplementalBlockEntities = readSupplementalBlockEntities(entry.format(), root);

        ArrayList<ConstructionEntry> blocks = new ArrayList<>(decoded.blockCount());
        for (ConstructionEntry entryBlock : decoded.entries()) {
            if (!includeAir && entryBlock.sourceState().isAir()) continue;
            CompoundTag data = entryBlock.blockEntityDataCopy();
            if (data == null || data.isEmpty()) data = supplementalBlockEntities.get(entryBlock.relativePos());
            data = normalizeBlockEntityData(entry.format(), data);
            blocks.add(new ConstructionEntry(entryBlock.relativePos(), entryBlock.sourceState(), data));
        }

        return new ConstructionPlan(blocks, entities, BlockPos.ZERO, decoded.sizeX(), decoded.sizeY(), decoded.sizeZ());
    }

    public static ConstructionPlan loadCard(ItemStack card, boolean includeAir) throws IOException {
        String fileName = SchematicCardItem.sourceFile(card);
        String sourceType = SchematicCardItem.sourceType(card);
        if (fileName.isBlank()) throw new IOException("Schematic Card is empty");
        SchematicFolderIndex.Format format = SchematicFolderIndex.Format.fromId(sourceType);
        if (format == null) format = SchematicFolderIndex.Format.fromFileName(fileName);
        if (format == null) throw new IOException("Unsupported schematic type: " + sourceType);
        return load(new SchematicFolderIndex.Entry(fileName, format), includeAir);
    }

    private static CompoundTag readCompressed(String fileName) throws IOException {
        Path path = SchematicFolderIndex.resolve(fileName);
        if (path == null || !Files.isRegularFile(path)) throw new IOException("Missing schematic: " + fileName);
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            CompoundTag root = NbtIo.readCompressed(stream, NbtAccounter.create(MAX_NBT_BYTES));
            if (root == null) throw new IOException("Invalid schematic NBT: " + fileName);
            return root;
        }
    }

    private static List<ConstructionEntityEntry> readEntities(SchematicFolderIndex.Format format, CompoundTag root,
                                                               ConstructionPlan decoded) {
        return switch (format) {
            case VANILLA_NBT -> readVanillaEntities(unwrapVanilla(root));
            case SPONGE_SCHEM -> readSpongeEntities(unwrapSponge(root));
            case LITEMATICA -> readLitematicEntities(root);
            case LEGACY_SCHEMATIC -> readLegacyEntities(root);
        };
    }

    private static CompoundTag unwrapVanilla(CompoundTag root) {
        if (!root.getListOrEmpty("palette").isEmpty()) return root;
        CompoundTag nested = root.getCompoundOrEmpty("Schematic");
        return nested.isEmpty() ? root : nested;
    }

    private static CompoundTag unwrapSponge(CompoundTag root) {
        CompoundTag nested = root.getCompoundOrEmpty("Schematic");
        return nested.isEmpty() ? root : nested;
    }

    private static List<ConstructionEntityEntry> readVanillaEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag list = root.getListOrEmpty("entities");
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof CompoundTag wrapper)) continue;
            Vec3 pos = vecFromList(wrapper.getListOrEmpty("pos"));
            if (pos == null) continue;
            CompoundTag data = wrapper.getCompoundOrEmpty("nbt").copy();
            if (data.isEmpty()) continue;
            result.add(new ConstructionEntityEntry(pos, sanitizeEntitySource(data)));
        }
        return result;
    }

    private static List<ConstructionEntityEntry> readSpongeEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag list = root.getListOrEmpty("Entities");
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof CompoundTag wrapper)) continue;
            Vec3 pos = vecFromList(wrapper.getListOrEmpty("Pos"));
            if (pos == null) continue;
            CompoundTag data = wrapper.getCompoundOrEmpty("Data").copy();
            if (data.isEmpty()) data = wrapper.copy();
            String id = wrapper.getString("Id").orElse(wrapper.getString("id").orElse(""));
            if (!id.isBlank() && data.getString("id").orElse("").isBlank()) data.putString("id", id);
            data.remove("Pos");
            data.remove("Data");
            data.remove("Id");
            if (!data.getString("id").orElse("").isBlank()) result.add(new ConstructionEntityEntry(pos, sanitizeEntitySource(data)));
        }
        return result;
    }

    private static List<ConstructionEntityEntry> readLegacyEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag list = root.getListOrEmpty("Entities");
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof CompoundTag data)) continue;
            Vec3 pos = vecFromList(data.getListOrEmpty("Pos"));
            if (pos == null || data.getString("id").orElse("").isBlank()) continue;
            result.add(new ConstructionEntityEntry(pos, sanitizeEntitySource(data)));
        }
        return result;
    }

    private static List<ConstructionEntityEntry> readLitematicEntities(CompoundTag root) {
        CompoundTag regions = root.getCompoundOrEmpty("Regions");
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        if (regions.isEmpty()) return result;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (String name : regions.keySet()) {
            CompoundTag region = regions.getCompoundOrEmpty(name);
            BlockPos start = compoundPos(region.getCompoundOrEmpty("Position"));
            BlockPos signed = compoundPos(region.getCompoundOrEmpty("Size"));
            int endX = start.getX() + Integer.signum(signed.getX()) * Math.max(0, Math.abs(signed.getX()) - 1);
            int endY = start.getY() + Integer.signum(signed.getY()) * Math.max(0, Math.abs(signed.getY()) - 1);
            int endZ = start.getZ() + Integer.signum(signed.getZ()) * Math.max(0, Math.abs(signed.getZ()) - 1);
            minX = Math.min(minX, Math.min(start.getX(), endX));
            minY = Math.min(minY, Math.min(start.getY(), endY));
            minZ = Math.min(minZ, Math.min(start.getZ(), endZ));
        }
        if (minX == Integer.MAX_VALUE) minX = minY = minZ = 0;

        for (String name : regions.keySet()) {
            CompoundTag region = regions.getCompoundOrEmpty(name);
            BlockPos regionPos = compoundPos(region.getCompoundOrEmpty("Position"));
            BlockPos signed = compoundPos(region.getCompoundOrEmpty("Size"));
            int signX = signed.getX() < 0 ? -1 : 1;
            int signY = signed.getY() < 0 ? -1 : 1;
            int signZ = signed.getZ() < 0 ? -1 : 1;
            ListTag list = region.getListOrEmpty("Entities");
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof CompoundTag data)) continue;
                Vec3 local = vecFromList(data.getListOrEmpty("Pos"));
                if (local == null || data.getString("id").orElse("").isBlank()) continue;
                Vec3 absolute = new Vec3(
                        regionPos.getX() + local.x * signX - minX,
                        regionPos.getY() + local.y * signY - minY,
                        regionPos.getZ() + local.z * signZ - minZ
                );
                result.add(new ConstructionEntityEntry(absolute, sanitizeEntitySource(data)));
            }
        }
        return result;
    }

    private static Map<BlockPos, CompoundTag> readSupplementalBlockEntities(SchematicFolderIndex.Format format, CompoundTag root) {
        if (format != SchematicFolderIndex.Format.LEGACY_SCHEMATIC) return Map.of();
        HashMap<BlockPos, CompoundTag> result = new HashMap<>();
        ListTag list = root.getListOrEmpty("TileEntities");
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof CompoundTag data)) continue;
            BlockPos pos = new BlockPos(number(data, "x", Integer.MIN_VALUE), number(data, "y", Integer.MIN_VALUE), number(data, "z", Integer.MIN_VALUE));
            if (pos.getX() == Integer.MIN_VALUE || pos.getY() == Integer.MIN_VALUE || pos.getZ() == Integer.MIN_VALUE) continue;
            result.put(pos, data.copy());
        }
        return result;
    }

    private static CompoundTag normalizeBlockEntityData(SchematicFolderIndex.Format format, CompoundTag raw) {
        if (raw == null || raw.isEmpty()) return null;
        CompoundTag data;
        if (format == SchematicFolderIndex.Format.SPONGE_SCHEM) {
            CompoundTag nested = raw.getCompoundOrEmpty("Data");
            data = nested.isEmpty() ? raw.copy() : nested.copy();
            String id = raw.getString("Id").orElse(raw.getString("id").orElse(""));
            if (!id.isBlank() && data.getString("id").orElse("").isBlank()) data.putString("id", id);
            data.remove("Data");
            data.remove("Id");
            data.remove("Pos");
        } else data = raw.copy();
        return data.isEmpty() ? null : data;
    }

    private static CompoundTag sanitizeEntitySource(CompoundTag raw) {
        CompoundTag data = raw.copy();
        // A pasted entity must receive a new runtime identity and a transformed position.
        data.remove("UUID");
        data.remove("UUIDMost");
        data.remove("UUIDLeast");
        data.remove("Pos");
        data.remove("Motion");
        return data;
    }

    private static Vec3 vecFromList(ListTag list) {
        if (list.size() < 3) return null;
        Double x = numeric(list.get(0));
        Double y = numeric(list.get(1));
        Double z = numeric(list.get(2));
        return x == null || y == null || z == null ? null : new Vec3(x, y, z);
    }

    private static Double numeric(Tag tag) {
        return tag instanceof NumericTag n ? n.doubleValue() : null;
    }

    private static int number(CompoundTag tag, String key, int fallback) {
        Tag value = tag.get(key);
        return value instanceof NumericTag numeric ? numeric.intValue() : fallback;
    }

    private static BlockPos compoundPos(CompoundTag tag) {
        return new BlockPos(number(tag, "x", 0), number(tag, "y", 0), number(tag, "z", 0));
    }
}
