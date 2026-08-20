package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Converts external schematic formats into the Constructor's neutral plan. */
public final class SchematicPlanLoader {
    private static final long MAX_NBT_BYTES = 0x20000000L;

    private SchematicPlanLoader() {}

    public static ConstructionPlan load(SchematicFolderIndex.Entry entry) throws IOException {
        if (entry == null) throw new IOException("No schematic selected");
        return switch (entry.format()) {
            case CREATE_NBT -> loadCreateNbt(entry.fileName());
        };
    }

    public static ConstructionPlan loadCard(net.minecraft.world.item.ItemStack card) throws IOException {
        String fileName = SchematicCardItem.sourceFile(card);
        String sourceType = SchematicCardItem.sourceType(card);
        if (fileName.isBlank() || sourceType.isBlank()) throw new IOException("Schematic Card is empty");
        SchematicFolderIndex.Format format = switch (sourceType) {
            case "create_nbt" -> SchematicFolderIndex.Format.CREATE_NBT;
            default -> throw new IOException("Unsupported schematic type: " + sourceType);
        };
        return load(new SchematicFolderIndex.Entry(fileName, format));
    }

    private static ConstructionPlan loadCreateNbt(String fileName) throws IOException {
        Path path = SchematicFolderIndex.resolve(fileName);
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Missing schematic: " + fileName);
        }

        CompoundTag root;
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            root = NbtIo.readCompressed(stream, NbtAccounter.create(MAX_NBT_BYTES));
        }
        if (root == null) throw new IOException("Invalid schematic NBT: " + fileName);

        ListTag paletteTag = root.getListOrEmpty("palette");
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            if (paletteTag.get(i) instanceof CompoundTag stateTag) {
                palette.add(readState(stateTag));
            } else {
                palette.add(Blocks.AIR.defaultBlockState());
            }
        }

        ListTag blocksTag = root.getListOrEmpty("blocks");
        ArrayList<ConstructionEntry> entries = new ArrayList<>(blocksTag.size());
        for (int i = 0; i < blocksTag.size(); i++) {
            if (!(blocksTag.get(i) instanceof CompoundTag blockTag)) continue;
            int stateIndex = blockTag.getIntOr("state", -1);
            if (stateIndex < 0 || stateIndex >= palette.size()) continue;
            BlockState state = palette.get(stateIndex);
            if (state.isAir()) continue;

            ListTag pos = blockTag.getListOrEmpty("pos");
            if (pos.size() < 3) continue;
            int x = pos.getInt(0).orElse(0);
            int y = pos.getInt(1).orElse(0);
            int z = pos.getInt(2).orElse(0);
            BlockPos relative = new BlockPos(x, y, z);
            entries.add(new ConstructionEntry(relative, state));
        }

        return new ConstructionPlan(entries);
    }

    private static BlockState readState(CompoundTag stateTag) {
        String id = stateTag.getString("Name").orElse("minecraft:air");
        Block block;
        try {
            block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        } catch (RuntimeException ignored) {
            return Blocks.AIR.defaultBlockState();
        }
        if (block == null) return Blocks.AIR.defaultBlockState();

        BlockState state = block.defaultBlockState();
        CompoundTag properties = stateTag.getCompoundOrEmpty("Properties");
        for (String key : properties.keySet()) {
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property == null) continue;
            String value = properties.getString(key).orElse("");
            state = applyProperty(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }
}
