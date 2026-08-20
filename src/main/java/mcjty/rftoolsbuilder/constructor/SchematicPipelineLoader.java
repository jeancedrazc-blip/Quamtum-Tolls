package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntityEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
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
import java.util.List;

/**
 * Complete format-neutral schematic loader used by the pipeline.
 *
 * SchematicPlanLoader remains the block adapter. This layer additionally
 * normalizes entity records from every supported format into the exact same
 * coordinate space. Keeping this concern separate makes the entity import
 * rules auditable without weakening the mature block decoders.
 */
public final class SchematicPipelineLoader {
    private static final long MAX_NBT_BYTES = 0x20000000L;

    private SchematicPipelineLoader() {}

    public static ConstructionPlan load(SchematicFolderIndex.Entry entry, boolean includeAir) throws IOException {
        ConstructionPlan blocks = SchematicPlanLoader.load(entry, includeAir);
        List<ConstructionEntityEntry> entities = loadEntities(entry);
        if (entities.isEmpty()) return blocks;
        return new ConstructionPlan(blocks.entries(), entities);
    }

    public static ConstructionPlan load(SchematicFolderIndex.Entry entry) throws IOException {
        return load(entry, false);
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

    public static ConstructionPlan loadCard(ItemStack card) throws IOException {
        return loadCard(card, false);
    }

    private static List<ConstructionEntityEntry> loadEntities(SchematicFolderIndex.Entry entry) throws IOException {
        CompoundTag root = readCompressed(entry.fileName());
        return switch (entry.format()) {
            case VANILLA_NBT -> readVanillaEntities(root);
            case SPONGE_SCHEM -> readSpongeEntities(root);
            case LITEMATICA -> readLitematicEntities(root);
            case LEGACY_SCHEMATIC -> readLegacyEntities(root);
        };
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

    /** Vanilla/Create structure templates: outer pos is authoritative, nbt contains the entity payload. */
    private static List<ConstructionEntityEntry> readVanillaEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag entities = root.getListOrEmpty("entities");
        for (int i = 0; i < entities.size(); i++) {
            if (!(entities.get(i) instanceof CompoundTag wrapper)) continue;
            Vec3 pos = readVec(wrapper.getListOrEmpty("pos"));
            if (pos == null) continue;
            CompoundTag data = wrapper.getCompoundOrEmpty("nbt").copy();
            if (data.isEmpty()) continue;
            normalizeEntityPayload(data, pos);
            result.add(new ConstructionEntityEntry(pos, data));
        }
        return result;
    }

    /**
     * Sponge v1/v2 store entity position in schematic coordinates and data in
     * the outer record. WorldEdit v3 stores Pos relative to schematic minimum
     * and nests the entity payload under Data.
     */
    private static List<ConstructionEntityEntry> readSpongeEntities(CompoundTag originalRoot) {
        CompoundTag root = originalRoot;
        CompoundTag nested = originalRoot.getCompoundOrEmpty("Schematic");
        boolean v3 = !nested.isEmpty() && number(nested, "Version", 0) >= 3;
        if (v3) root = nested;

        Vec3 minimum = Vec3.ZERO;
        if (!v3) {
            int[] offset = intArray(root, "Offset");
            if (offset.length >= 3) minimum = new Vec3(offset[0], offset[1], offset[2]);
        }

        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag entities = root.getListOrEmpty("Entities");
        for (int i = 0; i < entities.size(); i++) {
            if (!(entities.get(i) instanceof CompoundTag wrapper)) continue;
            Vec3 storedPos = readVec(wrapper.getListOrEmpty("Pos"));
            if (storedPos == null) continue;
            Vec3 relative = v3 ? storedPos : storedPos.subtract(minimum);

            CompoundTag data;
            if (v3) {
                data = wrapper.getCompoundOrEmpty("Data").copy();
            } else {
                data = wrapper.copy();
                data.remove("Id");
                data.remove("Pos");
            }
            String id = wrapper.getString("Id").orElse("");
            if (!id.isBlank()) data.putString("id", id);
            normalizeEntityPayload(data, relative);
            if (!data.isEmpty()) result.add(new ConstructionEntityEntry(relative, data));
        }
        return result;
    }

    /**
     * Litematica v2+ stores each entity NBT directly in a region and its Pos is
     * relative to that region's Position. Version 1 wraps EntityData but uses
     * the same region-relative position.
     */
    private static List<ConstructionEntityEntry> readLitematicEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        int version = number(root, "Version", 2);
        CompoundTag regions = root.getCompoundOrEmpty("Regions");
        for (String regionName : regions.keySet()) {
            CompoundTag region = regions.getCompoundOrEmpty(regionName);
            if (region.isEmpty()) continue;
            CompoundTag regionPosTag = region.getCompoundOrEmpty("Position");
            Vec3 regionPos = new Vec3(
                    number(regionPosTag, "x", 0),
                    number(regionPosTag, "y", 0),
                    number(regionPosTag, "z", 0)
            );

            ListTag entities = region.getListOrEmpty("Entities");
            for (int i = 0; i < entities.size(); i++) {
                if (!(entities.get(i) instanceof CompoundTag wrapper)) continue;
                Vec3 local = readVec(wrapper.getListOrEmpty("Pos"));
                if (local == null) continue;
                CompoundTag data = version == 1 ? wrapper.getCompoundOrEmpty("EntityData").copy() : wrapper.copy();
                Vec3 relative = local.add(regionPos);
                normalizeEntityPayload(data, relative);
                if (!data.isEmpty()) result.add(new ConstructionEntityEntry(relative, data));
            }
        }
        return result;
    }

    /** MCEdit/Schematica entities are direct entity NBT with a Pos list. */
    private static List<ConstructionEntityEntry> readLegacyEntities(CompoundTag root) {
        ArrayList<ConstructionEntityEntry> result = new ArrayList<>();
        ListTag entities = root.getListOrEmpty("Entities");
        for (int i = 0; i < entities.size(); i++) {
            if (!(entities.get(i) instanceof CompoundTag data)) continue;
            Vec3 pos = readVec(data.getListOrEmpty("Pos"));
            if (pos == null) continue;
            CompoundTag copy = data.copy();
            normalizeEntityPayload(copy, pos);
            result.add(new ConstructionEntityEntry(pos, copy));
        }
        return result;
    }

    /**
     * Never preserve identity/transport state from a captured entity. A new
     * entity must receive a new UUID and authoritative target position when the
     * Constructor materializes it.
     */
    private static void normalizeEntityPayload(CompoundTag data, Vec3 relative) {
        data.remove("UUID");
        data.remove("UUIDMost");
        data.remove("UUIDLeast");
        data.remove("Dimension");
        data.remove("PortalCooldown");
        ListTag pos = new ListTag();
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(relative.x));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(relative.y));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(relative.z));
        data.put("Pos", pos);
        data.putDouble("MotionX", 0.0D); // harmless compatibility breadcrumbs for older payload processors
        data.putDouble("MotionY", 0.0D);
        data.putDouble("MotionZ", 0.0D);
    }

    private static Vec3 readVec(ListTag list) {
        if (list.size() < 3) return null;
        Tag x = list.get(0);
        Tag y = list.get(1);
        Tag z = list.get(2);
        if (!(x instanceof NumericTag nx) || !(y instanceof NumericTag ny) || !(z instanceof NumericTag nz)) return null;
        return new Vec3(nx.doubleValue(), ny.doubleValue(), nz.doubleValue());
    }

    private static int[] intArray(CompoundTag tag, String key) {
        Tag value = tag.get(key);
        return value instanceof net.minecraft.nbt.IntArrayTag ints ? ints.getAsIntArray() : new int[0];
    }

    private static int number(CompoundTag tag, String key, int fallback) {
        Tag value = tag.get(key);
        return value instanceof NumericTag numeric ? numeric.intValue() : fallback;
    }
}
