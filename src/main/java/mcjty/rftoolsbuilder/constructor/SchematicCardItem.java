package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.BlockSubstitutionRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class SchematicCardItem extends Item {
    public static final int MAX_REPLACEMENTS = 8;
    private static final String P = "QTSchematic";

    public SchematicCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static CompoundTag root(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void saveRoot(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setSource(ItemStack stack, String fileName, String sourceType) {
        CompoundTag tag = root(stack);
        tag.putString(P + "SourceType", sourceType == null ? "" : sourceType);
        tag.putString(P + "SourceName", fileName == null ? "" : fileName);
        tag.putString(P + "SourceFile", fileName == null ? "" : fileName);
        saveRoot(stack, tag);
    }

    public static boolean hasSource(ItemStack stack) {
        return !sourceFile(stack).isBlank() && !sourceType(stack).isBlank();
    }

    public static String sourceName(ItemStack stack) {
        String value = root(stack).getString(P + "SourceName").orElse("");
        return value.isBlank() ? "Empty Schematic Card" : value;
    }

    public static String sourceFile(ItemStack stack) {
        return root(stack).getString(P + "SourceFile").orElse("");
    }

    public static String sourceType(ItemStack stack) {
        return root(stack).getString(P + "SourceType").orElse("");
    }

    public static int rotation(ItemStack stack) {
        return Math.floorMod(root(stack).getIntOr(P + "Rotation", 0), 4);
    }

    public static int mirror(ItemStack stack) {
        return Math.max(0, Math.min(2, root(stack).getIntOr(P + "Mirror", 0)));
    }

    public static int offsetX(ItemStack stack) { return root(stack).getIntOr(P + "OffsetX", 0); }
    public static int offsetY(ItemStack stack) { return root(stack).getIntOr(P + "OffsetY", 0); }
    public static int offsetZ(ItemStack stack) { return root(stack).getIntOr(P + "OffsetZ", 0); }

    public static void setConfig(ItemStack stack, int rotation, int mirror, int x, int y, int z) {
        CompoundTag tag = root(stack);
        tag.putInt(P + "Rotation", Math.floorMod(rotation, 4));
        tag.putInt(P + "Mirror", Math.max(0, Math.min(2, mirror)));
        tag.putInt(P + "OffsetX", clampOffset(x));
        tag.putInt(P + "OffsetY", clampOffset(y));
        tag.putInt(P + "OffsetZ", clampOffset(z));
        saveRoot(stack, tag);
    }

    public static int replacementCount(ItemStack stack) {
        return Math.max(0, Math.min(MAX_REPLACEMENTS, root(stack).getIntOr(P + "ReplacementCount", 0)));
    }

    public static boolean addReplacement(ItemStack stack, Block from, Block to) {
        Identifier fromId = BuiltInRegistries.BLOCK.getKey(from);
        Identifier toId = BuiltInRegistries.BLOCK.getKey(to);
        if (fromId == null || toId == null || from == to) {
            return false;
        }
        CompoundTag tag = root(stack);
        int count = Math.max(0, Math.min(MAX_REPLACEMENTS, tag.getIntOr(P + "ReplacementCount", 0)));
        String fromString = fromId.toString();
        for (int i = 0; i < count; i++) {
            String existing = tag.getString(P + "ReplacementFrom" + i).orElse("");
            if (existing.equals(fromString)) {
                tag.putString(P + "ReplacementTo" + i, toId.toString());
                saveRoot(stack, tag);
                return true;
            }
        }
        if (count >= MAX_REPLACEMENTS) {
            return false;
        }
        tag.putString(P + "ReplacementFrom" + count, fromString);
        tag.putString(P + "ReplacementTo" + count, toId.toString());
        tag.putInt(P + "ReplacementCount", count + 1);
        saveRoot(stack, tag);
        return true;
    }

    public static void clearReplacements(ItemStack stack) {
        CompoundTag tag = root(stack);
        int count = Math.max(0, Math.min(MAX_REPLACEMENTS, tag.getIntOr(P + "ReplacementCount", 0)));
        for (int i = 0; i < count; i++) {
            tag.remove(P + "ReplacementFrom" + i);
            tag.remove(P + "ReplacementTo" + i);
        }
        tag.putInt(P + "ReplacementCount", 0);
        saveRoot(stack, tag);
    }

    public static void applyReplacements(ItemStack stack, BlockSubstitutionRules rules) {
        CompoundTag tag = root(stack);
        int count = Math.max(0, Math.min(MAX_REPLACEMENTS, tag.getIntOr(P + "ReplacementCount", 0)));
        for (int i = 0; i < count; i++) {
            String fromString = tag.getString(P + "ReplacementFrom" + i).orElse("");
            String toString = tag.getString(P + "ReplacementTo" + i).orElse("");
            if (fromString.isEmpty() || toString.isEmpty()) continue;
            try {
                Block from = BuiltInRegistries.BLOCK.getValue(Identifier.parse(fromString));
                Block to = BuiltInRegistries.BLOCK.getValue(Identifier.parse(toString));
                if (from != null && to != null) {
                    rules.replace(from, to.defaultBlockState());
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static int clampOffset(int value) {
        return Math.max(-64, Math.min(64, value));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> text, TooltipFlag flag) {
        if (!hasSource(stack)) {
            text.accept(Component.literal("Empty — write a schematic at the Schematic Table"));
            return;
        }
        text.accept(Component.literal(sourceName(stack)));
        text.accept(Component.literal("Format: " + switch (sourceType(stack)) {
            case "create_nbt" -> "Create / Vanilla NBT";
            default -> sourceType(stack);
        }));
        int replacements = replacementCount(stack);
        if (replacements > 0) text.accept(Component.literal("Replacements: " + replacements));
    }
}
