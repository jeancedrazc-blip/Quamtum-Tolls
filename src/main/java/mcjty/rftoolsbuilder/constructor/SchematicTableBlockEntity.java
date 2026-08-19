package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.BlockSubstitutionRules;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class SchematicTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_CARD = 0;
    public static final int SLOT_FROM = 1;
    public static final int SLOT_TO = 2;
    public static final int TOTAL_SLOTS = 3;

    public static final int STATUS_NO_CARD = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_SENT = 2;
    public static final int STATUS_NO_CONSTRUCTOR = 3;
    public static final int STATUS_REPLACEMENT_SAVED = 4;
    public static final int STATUS_PREVIEW_READY = 5;
    public static final int STATUS_BAD_REPLACEMENT = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int rotation;
    private int mirror;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private int status = STATUS_NO_CARD;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> rotation;
                case 1 -> mirror;
                case 2 -> offsetX;
                case 3 -> offsetY;
                case 4 -> offsetZ;
                case 5 -> status;
                case 6 -> card().isEmpty() ? 0 : SchematicCardItem.replacementCount(card());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> rotation = value;
                case 1 -> mirror = value;
                case 2 -> offsetX = value;
                case 3 -> offsetY = value;
                case 4 -> offsetZ = value;
                case 5 -> status = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public SchematicTableBlockEntity(BlockPos pos, BlockState state) {
        super(ConstructorBootstrap.SCHEMATIC_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData data() {
        return data;
    }

    public ItemStack card() {
        return items.get(SLOT_CARD);
    }

    public boolean handleButton(int id) {
        if (!(card().getItem() instanceof SchematicCardItem)) {
            status = STATUS_NO_CARD;
            setChanged();
            return true;
        }

        switch (id) {
            case 0 -> rotation = Math.floorMod(rotation - 1, 4);
            case 1 -> rotation = Math.floorMod(rotation + 1, 4);
            case 2 -> mirror = (mirror + 1) % 3;
            case 10 -> offsetX = clampOffset(offsetX - 1);
            case 11 -> offsetX = clampOffset(offsetX + 1);
            case 12 -> offsetY = clampOffset(offsetY - 1);
            case 13 -> offsetY = clampOffset(offsetY + 1);
            case 14 -> offsetZ = clampOffset(offsetZ - 1);
            case 15 -> offsetZ = clampOffset(offsetZ + 1);
            case 20 -> {
                SchematicCardItem.markTestPattern(card());
                status = STATUS_PREVIEW_READY;
            }
            case 21 -> {
                writeConfigToCard();
                status = sendTestPlan() ? STATUS_SENT : STATUS_NO_CONSTRUCTOR;
            }
            case 30 -> saveReplacement();
            case 31 -> {
                SchematicCardItem.clearReplacements(card());
                status = STATUS_READY;
            }
            default -> {
                return false;
            }
        }

        writeConfigToCard();
        setChanged();
        return true;
    }

    private void saveReplacement() {
        ItemStack fromStack = items.get(SLOT_FROM);
        ItemStack toStack = items.get(SLOT_TO);
        if (fromStack.getItem() instanceof BlockItem from && toStack.getItem() instanceof BlockItem to) {
            status = SchematicCardItem.addReplacement(card(), from.getBlock(), to.getBlock())
                    ? STATUS_REPLACEMENT_SAVED
                    : STATUS_BAD_REPLACEMENT;
        } else {
            status = STATUS_BAD_REPLACEMENT;
        }
    }

    private boolean sendTestPlan() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        ConstructorBlockEntity constructor = nearestConstructor(serverLevel, 16, 5);
        if (constructor == null) {
            return false;
        }

        SchematicCardItem.markTestPattern(card());
        List<ConstructionEntry> entries = new ArrayList<>();
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x <= 2; x++) {
                BlockState source = (y == 0 || y == 2 || x == -2 || x == 2)
                        ? Blocks.COBBLESTONE.defaultBlockState()
                        : Blocks.STONE.defaultBlockState();
                BlockPos relative = transform(new BlockPos(x, y, 0));
                entries.add(new ConstructionEntry(relative, source));
            }
        }

        BlockSubstitutionRules rules = new BlockSubstitutionRules();
        SchematicCardItem.applyReplacements(card(), rules);

        Direction facing = constructor.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos origin = constructor.getBlockPos().relative(facing, 4).offset(offsetX, offsetY, offsetZ);
        return constructor.startPlan(new ConstructionPlan(entries), origin, rules);
    }

    private ConstructorBlockEntity nearestConstructor(ServerLevel level, int horizontalRadius, int verticalRadius) {
        ConstructorBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos origin = getBlockPos();
        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    if ((x * x + z * z) > horizontalRadius * horizontalRadius) {
                        continue;
                    }
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.getBlockEntity(pos) instanceof ConstructorBlockEntity constructor) {
                        double distance = origin.distSqr(pos);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = constructor;
                        }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos transform(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        if (mirror == 1) {
            x = -x;
        } else if (mirror == 2) {
            z = -z;
        }
        return switch (Math.floorMod(rotation, 4)) {
            case 1 -> new BlockPos(-z, pos.getY(), x);
            case 2 -> new BlockPos(-x, pos.getY(), -z);
            case 3 -> new BlockPos(z, pos.getY(), -x);
            default -> new BlockPos(x, pos.getY(), z);
        };
    }

    private void loadConfigFromCard() {
        if (card().getItem() instanceof SchematicCardItem) {
            rotation = SchematicCardItem.rotation(card());
            mirror = SchematicCardItem.mirror(card());
            offsetX = SchematicCardItem.offsetX(card());
            offsetY = SchematicCardItem.offsetY(card());
            offsetZ = SchematicCardItem.offsetZ(card());
            status = STATUS_READY;
        } else {
            rotation = mirror = offsetX = offsetY = offsetZ = 0;
            status = STATUS_NO_CARD;
        }
    }

    private void writeConfigToCard() {
        if (card().getItem() instanceof SchematicCardItem) {
            SchematicCardItem.setConfig(card(), rotation, mirror, offsetX, offsetY, offsetZ);
        }
    }

    private static int clampOffset(int value) {
        return Math.max(-64, Math.min(64, value));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Schematic Table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SchematicTableMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeConfigToCard();
        output.putInt("Rotation", rotation);
        output.putInt("Mirror", mirror);
        output.putInt("OffsetX", offsetX);
        output.putInt("OffsetY", offsetY);
        output.putInt("OffsetZ", offsetZ);
        output.putInt("Status", status);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                output.store("Item" + i, ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, input.read("Item" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        rotation = Math.floorMod(input.getIntOr("Rotation", 0), 4);
        mirror = Math.max(0, Math.min(2, input.getIntOr("Mirror", 0)));
        offsetX = clampOffset(input.getIntOr("OffsetX", 0));
        offsetY = clampOffset(input.getIntOr("OffsetY", 0));
        offsetZ = clampOffset(input.getIntOr("OffsetZ", 0));
        status = input.getIntOr("Status", STATUS_NO_CARD);
        if (!card().isEmpty()) {
            loadConfigFromCard();
        }
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (!result.isEmpty()) {
            if (slot == SLOT_CARD) loadConfigFromCard();
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot == SLOT_CARD) loadConfigFromCard();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot == SLOT_CARD) {
            loadConfigFromCard();
        }
        setChanged();
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        loadConfigFromCard();
        setChanged();
    }
}
