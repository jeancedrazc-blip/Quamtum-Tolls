package mcjty.rftoolsbuilder.constructor.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** One normalized schematic block, relative to the plan origin. */
public record ConstructionEntry(BlockPos relativePos, BlockState sourceState) {
    public ConstructionEntry {
        relativePos = relativePos.immutable();
    }
}
