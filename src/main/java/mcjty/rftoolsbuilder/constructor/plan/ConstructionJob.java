package mcjty.rftoolsbuilder.constructor.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;

/**
 * Runtime cursor over a normalized plan.
 *
 * Unlike the old linear cursor this keeps a pending queue, allowing blocks that
 * currently lack support to be deferred to a later pass (Create Schematicannon-
 * style) without losing deterministic progress.
 */
public final class ConstructionJob {
    private final ConstructionPlan plan;
    private final BlockPos origin;
    private final BlockSubstitutionRules substitutions;
    private final ArrayDeque<Integer> pending = new ArrayDeque<>();
    private final int total;
    private int completed;
    private int consecutiveDeferrals;

    public ConstructionJob(ConstructionPlan plan, BlockPos origin, BlockSubstitutionRules substitutions) {
        this.plan = plan;
        this.origin = origin.immutable();
        this.substitutions = substitutions;
        this.total = plan.size();
        for (int i = 0; i < total; i++) pending.addLast(i);
    }

    public boolean hasCurrent() {
        return !pending.isEmpty();
    }

    private int currentPlanIndex() {
        Integer index = pending.peekFirst();
        if (index == null) throw new IllegalStateException("Construction job has no current entry");
        return index;
    }

    public ConstructionEntry currentEntry() {
        return plan.get(currentPlanIndex());
    }

    public BlockPos currentWorldPos() {
        return origin.offset(currentEntry().relativePos());
    }

    public BlockState currentSourceState() {
        return currentEntry().sourceState();
    }

    public BlockState currentTargetState() {
        return substitutions.apply(currentSourceState());
    }

    public CompoundTag currentBlockEntityData() {
        return currentEntry().blockEntityDataCopy();
    }

    /** Mark the current target complete/irrelevant and move forward. */
    public boolean advance() {
        if (pending.isEmpty()) return false;
        pending.removeFirst();
        completed++;
        consecutiveDeferrals = 0;
        return !pending.isEmpty();
    }

    /** Same accounting as advance(), but semantically used for ignored/skipped targets. */
    public boolean skipCurrent() {
        return advance();
    }

    /**
     * Move the current target to the back of the queue. Returns false after a
     * complete pass in which every remaining target was deferred, signalling a
     * genuine support deadlock.
     */
    public boolean deferCurrent() {
        if (pending.isEmpty()) return false;
        Integer current = pending.removeFirst();
        pending.addLast(current);
        consecutiveDeferrals++;
        return consecutiveDeferrals < pending.size();
    }

    public int index() {
        return completed;
    }

    public int total() {
        return total;
    }

    public int remaining() {
        return pending.size();
    }

    public float progress() {
        return total == 0 ? 1.0f : Math.min(1.0f, completed / (float) total);
    }

    public ConstructionPlan plan() {
        return plan;
    }

    public BlockSubstitutionRules substitutions() {
        return substitutions;
    }
}
