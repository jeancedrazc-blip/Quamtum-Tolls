package mcjty.rftoolsbuilder.constructor.plan;

import mcjty.rftoolsbuilder.constructor.ConstructorRequirement;
import mcjty.rftoolsbuilder.constructor.ConstructorRequirementRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format-neutral schematic consumed by both preview and Constructor printing.
 * Every adapter is normalized to a zero-based block grid here so transforms are
 * identical regardless of source format.
 */
public final class ConstructionPlan {
    private final List<ConstructionEntry> entries;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public ConstructionPlan(List<ConstructionEntry> sourceEntries) {
        ArrayList<ConstructionEntry> source = new ArrayList<>();
        if (sourceEntries != null) {
            for (ConstructionEntry entry : sourceEntries) {
                if (entry != null && entry.relativePos() != null && entry.sourceState() != null) source.add(entry);
            }
        }

        if (source.isEmpty()) {
            entries = List.of();
            sizeX = sizeY = sizeZ = 0;
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ConstructionEntry entry : source) {
            BlockPos p = entry.relativePos();
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        ArrayList<ConstructionEntry> normalized = new ArrayList<>(source.size());
        for (ConstructionEntry entry : source) {
            BlockPos p = entry.relativePos();
            normalized.add(new ConstructionEntry(
                    new BlockPos(p.getX() - minX, p.getY() - minY, p.getZ() - minZ),
                    entry.sourceState(),
                    entry.blockEntityDataCopy()
            ));
        }

        // Create-style traversal: X fastest, then Z, then Y.
        normalized.sort(Comparator
                .comparingInt((ConstructionEntry e) -> e.relativePos().getY())
                .thenComparingInt(e -> e.relativePos().getZ())
                .thenComparingInt(e -> e.relativePos().getX()));
        entries = List.copyOf(normalized);
        sizeX = maxX - minX + 1;
        sizeY = maxY - minY + 1;
        sizeZ = maxZ - minZ + 1;
    }

    public List<ConstructionEntry> entries() { return entries; }
    public int size() { return entries.size(); }
    public ConstructionEntry get(int index) { return entries.get(index); }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public long volume() { return (long) sizeX * sizeY * sizeZ; }

    /** Reserved for normalized entity entries; never conflated with block progress. */
    public boolean hasEntities() { return false; }

    /** Material checklist after substitutions, matching actual consumption. */
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
