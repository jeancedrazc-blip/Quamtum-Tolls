package mcjty.rftoolsbuilder.constructor.plan;

import mcjty.rftoolsbuilder.constructor.SchematicTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;

/**
 * Persistent Create-style print cursor. Primary blocks are visited first,
 * support-sensitive blocks can be deferred to a second pass, and the stage is
 * persisted so unloading a world never restarts the build from the beginning.
 */
public final class ConstructionJob {
    public enum Stage { BLOCKS, DEFERRED_BLOCKS, ENTITIES, COMPLETE }

    private final ConstructionPlan plan;
    private final SchematicTransform transform;
    private final BlockSubstitutionRules substitutions;
    private final ArrayDeque<Integer> primary = new ArrayDeque<>();
    private final ArrayDeque<Integer> deferred = new ArrayDeque<>();
    private Stage stage = Stage.BLOCKS;
    private int completed;
    private int deferredAttempts;

    public ConstructionJob(ConstructionPlan plan, SchematicTransform transform, BlockSubstitutionRules substitutions) {
        if (plan == null) throw new IllegalArgumentException("plan");
        if (transform == null) throw new IllegalArgumentException("transform");
        this.plan = plan;
        this.transform = transform;
        this.substitutions = substitutions == null ? new BlockSubstitutionRules() : substitutions;
        for (int i = 0; i < plan.size(); i++) primary.addLast(i);
        normalizeStage();
    }

    public static ConstructionJob restore(ConstructionPlan plan, SchematicTransform transform,
                                          BlockSubstitutionRules substitutions, CompoundTag tag) {
        ConstructionJob job = new ConstructionJob(plan, transform, substitutions);
        if (tag == null || tag.isEmpty()) return job;

        job.primary.clear();
        job.deferred.clear();
        int[] p = tag.getIntArray("Primary").orElseGet(() -> new int[0]);
        int[] d = tag.getIntArray("Deferred").orElseGet(() -> new int[0]);
        for (int index : p) if (index >= 0 && index < plan.size()) job.primary.addLast(index);
        for (int index : d) if (index >= 0 && index < plan.size()) job.deferred.addLast(index);

        int stageOrdinal = tag.getIntOr("Stage", Stage.BLOCKS.ordinal());
        job.stage = Stage.values()[Math.max(0, Math.min(Stage.values().length - 1, stageOrdinal))];
        job.completed = Math.max(0, Math.min(plan.size(), tag.getIntOr("Completed", 0)));
        job.deferredAttempts = Math.max(0, tag.getIntOr("DeferredAttempts", 0));

        if (job.completed < plan.size() && job.primary.isEmpty() && job.deferred.isEmpty()) {
            for (int i = job.completed; i < plan.size(); i++) job.primary.addLast(i);
            job.stage = Stage.BLOCKS;
        }
        job.normalizeStage();
        return job;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Stage", stage.ordinal());
        tag.putInt("Completed", completed);
        tag.putInt("DeferredAttempts", deferredAttempts);
        tag.put("Primary", new IntArrayTag(primary.stream().mapToInt(Integer::intValue).toArray()));
        tag.put("Deferred", new IntArrayTag(deferred.stream().mapToInt(Integer::intValue).toArray()));
        return tag;
    }

    private void normalizeStage() {
        if (stage == Stage.BLOCKS && primary.isEmpty()) stage = Stage.DEFERRED_BLOCKS;
        if (stage == Stage.DEFERRED_BLOCKS && deferred.isEmpty()) stage = Stage.ENTITIES;
        if (stage == Stage.ENTITIES && !plan.hasEntities()) stage = Stage.COMPLETE;
    }

    public Stage stage() { normalizeStage(); return stage; }

    public boolean hasCurrentBlock() {
        normalizeStage();
        return stage == Stage.BLOCKS ? !primary.isEmpty()
                : stage == Stage.DEFERRED_BLOCKS && !deferred.isEmpty();
    }

    private int currentPlanIndex() {
        Integer value = stage == Stage.BLOCKS ? primary.peekFirst() : deferred.peekFirst();
        if (value == null) throw new IllegalStateException("No current block in stage " + stage);
        return value;
    }

    public ConstructionEntry currentEntry() { return plan.get(currentPlanIndex()); }
    public BlockState currentSourceState() { return currentEntry().sourceState(); }

    /** Substitution is applied before transform so material and placement see the same replacement. */
    public BlockState currentTargetState() {
        BlockState substituted = substitutions.apply(currentSourceState());
        return transform.transformState(substituted);
    }

    public BlockPos currentWorldPos() { return transform.transformWorld(currentEntry().relativePos()); }
    public CompoundTag currentBlockEntityData() { return currentEntry().blockEntityDataCopy(); }

    public boolean advance() {
        if (!hasCurrentBlock()) return false;
        if (stage == Stage.BLOCKS) primary.removeFirst(); else deferred.removeFirst();
        completed++;
        deferredAttempts = 0;
        normalizeStage();
        return hasCurrentBlock();
    }

    public boolean skipCurrent() { return advance(); }

    /**
     * First-pass dependency failures move to the dedicated deferred pass. In
     * that pass entries rotate until one succeeds; a complete no-progress pass
     * reports a real support deadlock instead of looping forever.
     */
    public boolean deferCurrent() {
        if (!hasCurrentBlock()) return false;
        if (stage == Stage.BLOCKS) {
            Integer current = primary.removeFirst();
            if (current != null) deferred.addLast(current);
            deferredAttempts = 0;
            normalizeStage();
            return hasCurrentBlock();
        }

        Integer current = deferred.removeFirst();
        if (current != null) deferred.addLast(current);
        deferredAttempts++;
        return !deferred.isEmpty() && deferredAttempts < deferred.size();
    }

    public int completed() { return completed; }
    public int total() { return plan.size(); }
    public int remaining() { return Math.max(0, total() - completed); }
    public float progress() { return total() == 0 ? 1f : Math.min(1f, completed / (float) total()); }
    public ConstructionPlan plan() { return plan; }
    public BlockSubstitutionRules substitutions() { return substitutions; }
    public SchematicTransform transform() { return transform; }
}
