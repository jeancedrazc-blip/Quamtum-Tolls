package mcjty.rftoolsbuilder.constructor.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Runtime cursor over a normalized plan. Format readers never execute construction directly. */
public final class ConstructionJob {
    private final ConstructionPlan plan;
    private final BlockPos origin;
    private final BlockSubstitutionRules substitutions;
    private int index;

    public ConstructionJob(ConstructionPlan plan, BlockPos origin, BlockSubstitutionRules substitutions) {
        this.plan = plan;
        this.origin = origin.immutable();
        this.substitutions = substitutions;
        this.index = 0;
    }

    public boolean hasCurrent() {
        return index >= 0 && index < plan.size();
    }

    public BlockPos currentWorldPos() {
        return origin.offset(plan.get(index).relativePos());
    }

    public BlockState currentSourceState() {
        return plan.get(index).sourceState();
    }

    public BlockState currentTargetState() {
        return substitutions.apply(currentSourceState());
    }

    public boolean advance() {
        index++;
        return hasCurrent();
    }

    public int index() {
        return index;
    }

    public int total() {
        return plan.size();
    }

    public float progress() {
        return plan.size() == 0 ? 1.0f : Math.min(1.0f, index / (float) plan.size());
    }

    public ConstructionPlan plan() {
        return plan;
    }

    public BlockSubstitutionRules substitutions() {
        return substitutions;
    }
}
