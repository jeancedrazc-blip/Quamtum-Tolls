package mcjty.rftoolsbuilder.constructor.plan;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format-neutral structure consumed by the Constructor engine.
 * Adapters for Create/Sponge/vanilla/etc. all normalize into this class.
 */
public final class ConstructionPlan {
    private final List<ConstructionEntry> entries;

    public ConstructionPlan(List<ConstructionEntry> entries) {
        ArrayList<ConstructionEntry> normalized = new ArrayList<>(entries);
        // Safe baseline traversal: bottom-to-top, then keep nearby X/Z coordinates together.
        normalized.sort(Comparator
                .comparingInt((ConstructionEntry e) -> e.relativePos().getY())
                .thenComparingInt(e -> e.relativePos().getZ())
                .thenComparingInt(e -> e.relativePos().getX()));
        this.entries = List.copyOf(normalized);
    }

    public List<ConstructionEntry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public ConstructionEntry get(int index) {
        return entries.get(index);
    }

    /** Material checklist after substitutions, matching what the machine will actually consume. */
    public Map<Item, Integer> materialChecklist(BlockSubstitutionRules substitutions) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (ConstructionEntry entry : entries) {
            BlockState state = substitutions.apply(entry.sourceState());
            Item item = state.getBlock().asItem();
            if (item != Items.AIR) {
                result.merge(item, 1, Integer::sum);
            }
        }
        return result;
    }
}
