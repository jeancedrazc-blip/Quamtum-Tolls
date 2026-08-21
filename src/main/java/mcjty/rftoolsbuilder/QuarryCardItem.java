package mcjty.rftoolsbuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Quarry mode card with whitelist/blacklist item and tag filtering. */
public final class QuarryCardItem extends Item {
    public static final int MAX_FILTER_ENTRIES = 18;
    private static final String LEGACY_BLACK = "QuantumFilterBlacklist";
    private static final String DAMAGE = "QuantumFilterDamage";
    private static final String NBT = "QuantumFilterNbt";
    private static final String MOD = "QuantumFilterMod";
    private static final String ITEM_COUNT = "QuantumFilterItemCount";
    private static final String TAG_COUNT = "QuantumFilterTagCount";
    private static final String ITEM_PREFIX = "QuantumFilterItem";
    private static final String TAG_PREFIX = "QuantumFilterTag";
    private static final String ITEM_BLACK_PREFIX = "QuantumFilterItemBlack";
    private static final String TAG_BLACK_PREFIX = "QuantumFilterTagBlack";

    private final QuarryMode mode;

    public QuarryCardItem(Properties properties, QuarryMode mode) {
        super(properties.stacksTo(1));
        this.mode = mode;
    }

    public QuarryMode mode() { return mode; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new QuarryFilterMenu(id, inv, slot),
                    Component.translatable("gui.rftoolsbuilder.filter.title")),
                    data -> data.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }

    private static CompoundTag root(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void saveRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean damageMode(ItemStack stack) { return root(stack).getBooleanOr(DAMAGE, false); }
    public static boolean nbtMode(ItemStack stack) { return root(stack).getBooleanOr(NBT, false); }
    public static boolean modMode(ItemStack stack) { return root(stack).getBooleanOr(MOD, false); }
    public static int itemCount(ItemStack stack) { return Math.max(0, Math.min(MAX_FILTER_ENTRIES, root(stack).getIntOr(ITEM_COUNT, 0))); }
    public static int tagCount(ItemStack stack) { return Math.max(0, Math.min(MAX_FILTER_ENTRIES, root(stack).getIntOr(TAG_COUNT, 0))); }
    public static int entryCount(ItemStack stack) { return Math.min(MAX_FILTER_ENTRIES, itemCount(stack) + tagCount(stack)); }

    public static void toggle(ItemStack stack, int setting) {
        CompoundTag tag = root(stack);
        String key = switch (setting) { case 1 -> DAMAGE; case 2 -> NBT; case 3 -> MOD; default -> null; };
        if (key == null) return;
        tag.putBoolean(key, !tag.getBooleanOr(key, false));
        saveRoot(stack, tag);
    }

    public static String getTag(ItemStack stack, int index) {
        int count = tagCount(stack);
        if (index < 0 || index >= count) return "";
        return root(stack).getString(TAG_PREFIX + index).orElse("");
    }

    public static ItemStack getFilterItem(ItemStack stack, int index, HolderLookup.Provider registries) {
        int count = itemCount(stack);
        if (index < 0 || index >= count) return ItemStack.EMPTY;
        CompoundTag root = root(stack);
        CompoundTag encoded = root.getCompound(ITEM_PREFIX + index).orElse(null);
        if (encoded == null) return ItemStack.EMPTY;
        try {
            TagValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, registries, encoded);
            return input.read("Stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean entryBlacklist(ItemStack stack, int index) {
        CompoundTag tag = root(stack);
        boolean legacy = tag.getBooleanOr(LEGACY_BLACK, false);
        int tags = tagCount(stack);
        if (index < 0 || index >= entryCount(stack)) return legacy;
        if (index < tags) return tag.getBooleanOr(TAG_BLACK_PREFIX + index, legacy);
        return tag.getBooleanOr(ITEM_BLACK_PREFIX + (index - tags), legacy);
    }

    public static void toggleEntryRule(ItemStack stack, int index) {
        int tags = tagCount(stack);
        if (index < 0 || index >= entryCount(stack)) return;
        CompoundTag tag = root(stack);
        boolean current = entryBlacklist(stack, index);
        if (index < tags) tag.putBoolean(TAG_BLACK_PREFIX + index, !current);
        else tag.putBoolean(ITEM_BLACK_PREFIX + (index - tags), !current);
        saveRoot(stack, tag);
    }

    public static int blacklistCount(ItemStack stack) {
        int n = 0;
        for (int i = 0; i < entryCount(stack); i++) if (entryBlacklist(stack, i)) n++;
        return n;
    }
    public static int whitelistCount(ItemStack stack) { return entryCount(stack) - blacklistCount(stack); }

    public static List<String> getTags(ItemStack stack) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < tagCount(stack); i++) {
            String value = getTag(stack, i);
            if (!value.isBlank()) result.add(value);
        }
        return List.copyOf(result);
    }

    public static boolean addFilterItem(ItemStack card, ItemStack source, HolderLookup.Provider registries) {
        return addFilterItem(card, source, registries, false);
    }

    private static boolean addFilterItem(ItemStack card, ItemStack source, HolderLookup.Provider registries, boolean blacklist) {
        if (source == null || source.isEmpty() || source.getItem() instanceof QuarryCardItem || entryCount(card) >= MAX_FILTER_ENTRIES) return false;
        ItemStack normalized = source.copy();
        normalized.setCount(1);
        for (int i = 0; i < itemCount(card); i++) {
            if (ItemStack.isSameItemSameComponents(normalized, getFilterItem(card, i, registries))) return false;
        }
        CompoundTag tag = root(card);
        int index = itemCount(card);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.store("Stack", ItemStack.CODEC, normalized);
        tag.put(ITEM_PREFIX + index, output.buildResult());
        tag.putBoolean(ITEM_BLACK_PREFIX + index, blacklist);
        tag.putInt(ITEM_COUNT, index + 1);
        saveRoot(card, tag);
        return true;
    }

    public static boolean addFilterTag(ItemStack card, String raw) { return addFilterTag(card, raw, false); }

    private static boolean addFilterTag(ItemStack card, String raw, boolean blacklist) {
        if (raw == null || entryCount(card) >= MAX_FILTER_ENTRIES) return false;
        String clean = raw.trim().toLowerCase();
        if (clean.startsWith("#")) clean = clean.substring(1);
        if (!clean.contains(":")) return false;
        final String canonical;
        try { canonical = Identifier.parse(clean).toString(); }
        catch (RuntimeException ignored) { return false; }
        for (int i = 0; i < tagCount(card); i++) if (canonical.equals(getTag(card, i))) return false;
        CompoundTag tag = root(card);
        int index = tagCount(card);
        tag.putString(TAG_PREFIX + index, canonical);
        tag.putBoolean(TAG_BLACK_PREFIX + index, blacklist);
        tag.putInt(TAG_COUNT, index + 1);
        saveRoot(card, tag);
        return true;
    }

    public static int addTagsFromItem(ItemStack card, ItemStack source) { return addTagsFromItem(card, source, false); }

    private static int addTagsFromItem(ItemStack card, ItemStack source, boolean blacklist) {
        if (source == null || source.isEmpty()) return 0;
        int[] added = {0};
        source.getItem().builtInRegistryHolder().tags().forEach(tagKey -> {
            if (entryCount(card) < MAX_FILTER_ENTRIES && addFilterTag(card, tagKey.location().toString(), blacklist)) added[0]++;
        });
        return added[0];
    }

    public static void removeEntry(ItemStack card, int index, HolderLookup.Provider registries) {
        int tags = tagCount(card);
        int items = itemCount(card);
        if (index < 0 || index >= tags + items) return;
        CompoundTag tag = root(card);
        if (index < tags) {
            for (int i = index; i < tags - 1; i++) {
                tag.putString(TAG_PREFIX + i, tag.getString(TAG_PREFIX + (i + 1)).orElse(""));
                tag.putBoolean(TAG_BLACK_PREFIX + i, tag.getBooleanOr(TAG_BLACK_PREFIX + (i + 1), false));
            }
            tag.remove(TAG_PREFIX + (tags - 1)); tag.remove(TAG_BLACK_PREFIX + (tags - 1));
            tag.putInt(TAG_COUNT, tags - 1);
        } else {
            int itemIndex = index - tags;
            for (int i = itemIndex; i < items - 1; i++) {
                CompoundTag next = tag.getCompound(ITEM_PREFIX + (i + 1)).orElse(null);
                if (next != null) tag.put(ITEM_PREFIX + i, next.copy()); else tag.remove(ITEM_PREFIX + i);
                tag.putBoolean(ITEM_BLACK_PREFIX + i, tag.getBooleanOr(ITEM_BLACK_PREFIX + (i + 1), false));
            }
            tag.remove(ITEM_PREFIX + (items - 1)); tag.remove(ITEM_BLACK_PREFIX + (items - 1));
            tag.putInt(ITEM_COUNT, items - 1);
        }
        saveRoot(card, tag);
    }

    public static void expandEntryToTags(ItemStack card, int index, HolderLookup.Provider registries) {
        int tags = tagCount(card);
        if (index < tags || index >= entryCount(card)) return;
        boolean blacklist = entryBlacklist(card, index);
        ItemStack filter = getFilterItem(card, index - tags, registries);
        if (filter.isEmpty()) return;
        removeEntry(card, index, registries);
        addTagsFromItem(card, filter, blacklist);
    }

    public static void clearFilter(ItemStack card) {
        CompoundTag tag = root(card);
        int items = itemCount(card), tags = tagCount(card);
        for (int i = 0; i < items; i++) { tag.remove(ITEM_PREFIX + i); tag.remove(ITEM_BLACK_PREFIX + i); }
        for (int i = 0; i < tags; i++) { tag.remove(TAG_PREFIX + i); tag.remove(TAG_BLACK_PREFIX + i); }
        tag.remove(LEGACY_BLACK);
        tag.putInt(ITEM_COUNT, 0); tag.putInt(TAG_COUNT, 0);
        saveRoot(card, tag);
    }

    private static boolean matchesItemRule(ItemStack card, ItemStack target, ItemStack filter) {
        if (target.isEmpty() || filter.isEmpty()) return false;
        if (modMode(card)) {
            Identifier targetId = BuiltInRegistries.ITEM.getKey(target.getItem());
            Identifier filterId = BuiltInRegistries.ITEM.getKey(filter.getItem());
            return targetId != null && filterId != null && targetId.getNamespace().equals(filterId.getNamespace());
        }
        if (target.getItem() != filter.getItem()) return false;
        if (damageMode(card) && target.getDamageValue() != filter.getDamageValue()) return false;
        return !nbtMode(card) || ItemStack.isSameItemSameComponents(target, filter);
    }

    public static boolean allowsBlock(ItemStack card, BlockState state, HolderLookup.Provider registries) {
        int entries = entryCount(card);
        if (entries == 0) return true;
        boolean hasWhitelist = whitelistCount(card) > 0;
        ItemStack target = new ItemStack(state.getBlock().asItem());
        boolean whitelistMatch = false;
        int tags = tagCount(card);
        for (int i = 0; i < tags; i++) {
            String raw = getTag(card, i);
            try {
                Identifier id = Identifier.parse(raw);
                boolean match = state.is(TagKey.create(Registries.BLOCK, id))
                        || (!target.isEmpty() && target.is(TagKey.create(Registries.ITEM, id)));
                if (match) {
                    if (entryBlacklist(card, i)) return false;
                    whitelistMatch = true;
                }
            } catch (RuntimeException ignored) {}
        }
        for (int i = 0; i < itemCount(card); i++) {
            ItemStack rule = getFilterItem(card, i, registries);
            if (matchesItemRule(card, target, rule)) {
                if (entryBlacklist(card, tags + i)) return false;
                whitelistMatch = true;
            }
        }
        return whitelistMatch || !hasWhitelist;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> text, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, text, flag);
        if (mode.isFortune()) text.accept(Component.literal("Fortune III").withStyle(ChatFormatting.GOLD));
        else if (mode.isSilk()) text.accept(Component.literal("Silk Touch").withStyle(ChatFormatting.AQUA));
        text.accept(Component.translatable(mode.isClear() ? "tooltip.rftoolsbuilder.quarry.clear" : "tooltip.rftoolsbuilder.quarry.replace")
                .withStyle(ChatFormatting.GRAY));
        text.accept(Component.translatable("tooltip.rftoolsbuilder.filter_summary_mixed", whitelistCount(stack), blacklistCount(stack))
                .withStyle(ChatFormatting.DARK_AQUA));
        text.accept(Component.translatable("tooltip.rftoolsbuilder.filter_open").withStyle(ChatFormatting.DARK_GRAY));
    }
}
