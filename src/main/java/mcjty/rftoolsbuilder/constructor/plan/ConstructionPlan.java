package mcjty.rftoolsbuilder.constructor.plan;

import mcjty.rftoolsbuilder.constructor.ConstructorRequirement;
import mcjty.rftoolsbuilder.constructor.ConstructorRequirementRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format-neutral schematic consumed by both preview and Constructor printing.
 * Every adapter is normalized to one zero-based coordinate space so transforms
 * are identical regardless of the source file format.
 */
public final class ConstructionPlan {
    private final List<ConstructionEntry> entries;
    private final List<ConstructionEntityEntry> entities;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public ConstructionPlan(List<ConstructionEntry> sourceEntries) {
        this(sourceEntries, List.of());
    }

    public ConstructionPlan(List<ConstructionEntry> sourceEntries, List<ConstructionEntityEntry> sourceEntities) {
        ArrayList<ConstructionEntry> blockSource = new ArrayList<>();
        if (sourceEntries != null) {
            for (ConstructionEntry entry : sourceEntries) {
                if (entry != null && entry.relativePos() != null && entry.sourceState() != null) blockSource.add(entry);
            }
        }

        ArrayList<ConstructionEntityEntry> entitySource = new ArrayList<>();
        if (sourceEntities != null) {
            for (ConstructionEntityEntry entity : sourceEntities) {
                if (entity != null && entity.relativePos() != null && entity.entityData() != null) entitySource.add(entity);
            }
        }

        if (blockSource.isEmpty() && entitySource.isEmpty()) {
            entries = List.of();
            entities = List.of();
            sizeX = sizeY = sizeZ = 0;
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (ConstructionEntry entry : blockSource) {
            BlockPos p = entry.relativePos();
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        for (ConstructionEntityEntry entity : entitySource) {
            Vec3 p = entity.relativePos();
            int x = floor(p.x);
            int y = floor(p.y);
            int z = floor(p.z);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        ArrayList<ConstructionEntry> normalizedBlocks = new ArrayList<>(blockSource.size());
        for (ConstructionEntry entry : blockSource) {
            BlockPos p = entry.relativePos();
            normalizedBlocks.add(new ConstructionEntry(
                    new BlockPos(p.getX() - minX, p.getY() - minY, p.getZ() - minZ),
                    entry.sourceState(),
                    entry.blockEntityDataCopy()
            ));
        }

        ArrayList<ConstructionEntityEntry> normalizedEntities = new ArrayList<>(entitySource.size());
        for (ConstructionEntityEntry entity : entitySource) {
            Vec3 p = entity.relativePos();
            normalizedEntities.add(new ConstructionEntityEntry(
                    new Vec3(p.x - minX, p.y - minY, p.z - minZ),
                    entity.entityDataCopy()
            ));
        }

        // Same primary scan order used by Create's printer: X fastest, then Z, then Y.
        normalizedBlocks.sort(Comparator
                .comparingInt((ConstructionEntry e) -> e.relativePos().getY())
                .thenComparingInt(e -> e.relativePos().getZ())
                .thenComparingInt(e -> e.relativePos().getX()));
        normalizedEntities.sort(Comparator
                .comparingDouble((ConstructionEntityEntry e) -> e.relativePos().y)
                .thenComparingDouble(e -> e.relativePos().z)
                .thenComparingDouble(e -> e.relativePos().x));

        entries = List.copyOf(normalizedBlocks);
        entities = List.copyOf(normalizedEntities);
        sizeX = maxX - minX + 1;
        sizeY = maxY - minY + 1;
        sizeZ = maxZ - minZ + 1;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public List<ConstructionEntry> entries() { return entries; }
    public List<ConstructionEntityEntry> entities() { return entities; }
    public int size() { return entries.size(); }
    public int blockCount() { return entries.size(); }
    public int entityCount() { return entities.size(); }
    public int totalTargets() { return blockCount() + entityCount(); }
    public ConstructionEntry get(int index) { return entries.get(index); }
    public ConstructionEntityEntry getEntity(int index) { return entities.get(index); }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public long volume() { return (long) sizeX * sizeY * sizeZ; }
    public boolean hasEntities() { return !entities.isEmpty(); }

    /** Block material checklist after substitutions, matching actual consumption. Entity requirements are resolved server-side. */
    public Map<Item, Integer> materialChecklist(BlockSubstitutionRules substitutions) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        BlockSubstitutionRules rules = substitutions == null ? new BlockSubstitutionRules() : substitutions;
        for (ConstructionEntry entry : entries) {
            BlockState state = rules.apply(entry.sourceState());
            ConstructorRequirement requirement = ConstructorRequirementRegistry.resolve(state, entry.blockEntityDataCopy());
            if (requirement.isEmpty() || requirement.isInvalid()) continue;
            for (ConstructorRequirement.StackRequirement stackRequirement : requirement.requirements()) {
                if (stackRequirement.use() != ConstructorRequirement.Use.CONSUME || stackRequirement.stack().isEmpty()) continue;
                result.merge(stackRequirement.stack().getItem(), stackRequirement.stack().getCount(), Integer::sum);
            }
        }
        return result;
    }
}
