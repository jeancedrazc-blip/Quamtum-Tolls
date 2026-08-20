package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public final class SchematicTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_CARD = 0;
    public static final int TOTAL_SLOTS = 1;

    public static final int STATUS_NO_CARD = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_WRITTEN = 2;
    public static final int STATUS_NO_SCHEMATICS = 3;
    public static final int STATUS_INVALID_SELECTION = 4;

    public static final int BUTTON_REFRESH = 900;
    public static final int BUTTON_SELECT_BASE = 1000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int status = STATUS_NO_CARD;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> status;
                case 1 -> SchematicCardItem.hasSource(card()) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) status = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public SchematicTableBlockEntity(BlockPos pos, BlockState state) {
        super(ConstructorBootstrap.SCHEMATIC_TABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public ContainerData data() { return data; }
    public ItemStack card() { return items.get(SLOT_CARD); }

    public boolean handleButton(int id) {
        if (!(card().getItem() instanceof SchematicCardItem)) {
            status = STATUS_NO_CARD;
            setChanged();
            return true;
        }

        List<SchematicFolderIndex.Entry> available = SchematicFolderIndex.list();
        if (id == BUTTON_REFRESH) {
            status = available.isEmpty() ? STATUS_NO_SCHEMATICS
                    : (SchematicCardItem.hasSource(card()) ? STATUS_WRITTEN : STATUS_READY);
            setChanged();
            return true;
        }

        if (id >= BUTTON_SELECT_BASE) {
            if (available.isEmpty()) {
                status = STATUS_NO_SCHEMATICS;
                setChanged();
                return true;
            }
            int index = id - BUTTON_SELECT_BASE;
            if (index < 0 || index >= available.size()) {
                status = STATUS_INVALID_SELECTION;
                setChanged();
                return true;
            }
            SchematicFolderIndex.Entry entry = available.get(index);
            SchematicCardItem.setSource(card(), entry.fileName(), entry.format().id());
            status = STATUS_WRITTEN;
            setChanged();
            return true;
        }

        return false;
    }

    private void refreshStatus() {
        if (!(card().getItem() instanceof SchematicCardItem)) {
            status = STATUS_NO_CARD;
        } else if (SchematicCardItem.hasSource(card())) {
            status = STATUS_WRITTEN;
        } else {
            status = SchematicFolderIndex.list().isEmpty() ? STATUS_NO_SCHEMATICS : STATUS_READY;
        }
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
        output.putInt("Status", status);
        ItemStack card = card();
        if (!card.isEmpty()) output.store("Card", ItemStack.CODEC, card);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.set(SLOT_CARD, input.read("Card", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        status = input.getIntOr("Status", STATUS_NO_CARD);
        refreshStatus();
    }

    @Override public int getContainerSize() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() { return card().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == SLOT_CARD ? card() : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT_CARD) return ItemStack.EMPTY;
        ItemStack stack = card();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) items.set(SLOT_CARD, ItemStack.EMPTY);
        refreshStatus();
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != SLOT_CARD) return ItemStack.EMPTY;
        ItemStack result = card();
        items.set(SLOT_CARD, ItemStack.EMPTY);
        refreshStatus();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT_CARD) return;
        items.set(SLOT_CARD, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        refreshStatus();
        setChanged();
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        items.set(SLOT_CARD, ItemStack.EMPTY);
        refreshStatus();
        setChanged();
    }
}
