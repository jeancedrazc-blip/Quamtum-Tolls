package mcjty.rftoolsbuilder.constructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class SchematicTableMenu extends AbstractContainerMenu {
    private final SchematicTableBlockEntity table;
    private final ContainerData data;

    public SchematicTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof SchematicTableBlockEntity found ? found : null,
                new SimpleContainerData(7));
    }

    public SchematicTableMenu(int id, Inventory inventory, SchematicTableBlockEntity table, ContainerData data) {
        super(ConstructorBootstrap.SCHEMATIC_TABLE_MENU.get(), id);
        this.table = table;
        this.data = data;

        if (table != null) {
            addSlot(new Slot(table, SchematicTableBlockEntity.SLOT_CARD, 24, 44) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SchematicCardItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
            addSlot(new Slot(table, SchematicTableBlockEntity.SLOT_FROM, 88, 44) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
            addSlot(new Slot(table, SchematicTableBlockEntity.SLOT_TO, 112, 44) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        int invX = 48;
        int invY = 142;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
        }
        addDataSlots(data);
    }

    public ContainerData data() { return data; }
    public SchematicTableBlockEntity table() { return table; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return table != null && table.handleButton(id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int machineSlots = table == null ? 0 : 3;

        if (index < machineSlots) {
            if (!moveItemStackTo(source, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof SchematicCardItem) {
            if (!moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof BlockItem) {
            if (!moveItemStackTo(source, 1, 3, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return table != null && table.stillValid(player);
    }
}
