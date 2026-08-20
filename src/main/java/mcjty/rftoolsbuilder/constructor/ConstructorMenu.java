package mcjty.rftoolsbuilder.constructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ConstructorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 14;

    private final ConstructorBlockEntity constructor;
    private final ContainerData data;

    public ConstructorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ConstructorBlockEntity found ? found : null,
                new SimpleContainerData(DATA_COUNT));
    }

    public ConstructorMenu(int id, Inventory inventory, ConstructorBlockEntity constructor, ContainerData data) {
        super(ConstructorBootstrap.CONSTRUCTOR_MENU.get(), id);
        this.constructor = constructor;
        this.data = data;

        if (constructor != null) {
            addSlot(new Slot(constructor, ConstructorBlockEntity.SLOT_SCHEMATIC, 112, 116) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return constructor.canRemoveCard() && stack.getItem() instanceof SchematicCardItem;
                }
                @Override public boolean mayPickup(Player player) { return constructor.canRemoveCard(); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        int invX = 48;
        int invY = 208;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
        addDataSlots(data);
    }

    public ConstructorBlockEntity constructor() { return constructor; }
    public ContainerData data() { return data; }

    @Override public boolean clickMenuButton(Player player, int id) { return constructor != null && constructor.handleMenuButton(id); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int machineSlots = constructor == null ? 0 : 1;

        if (index < machineSlots) {
            if (constructor != null && !constructor.canRemoveCard()) return ItemStack.EMPTY;
            if (!moveItemStackTo(source, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof SchematicCardItem) {
            if (constructor == null || !constructor.canRemoveCard() || !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;

        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (constructor == null || constructor.isRemoved() || constructor.getLevel() != player.level()) return false;
        var pos = constructor.getBlockPos();
        return player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64.0;
    }
}
