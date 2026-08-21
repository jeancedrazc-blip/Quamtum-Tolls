package mcjty.rftoolsbuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative filter editor for quarry cards. */
public final class QuarryFilterMenu extends AbstractContainerMenu {
    public static final int REMOVE_BASE = 100;
    public static final int EXPAND_BASE = 200;
    public static final int TOGGLE_RULE_BASE = 300;

    private final Inventory inventory;
    private final int cardSlot;

    public QuarryFilterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    public QuarryFilterMenu(int id, Inventory inventory, int cardSlot) {
        super(RFToolsBuilder.QUARRY_FILTER_MENU.get(), id);
        this.inventory = inventory;
        this.cardSlot = cardSlot;
        int invX = 47, invY = 217;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, invX + col * 18, invY + 58));
    }

    public int cardSlot() { return cardSlot; }

    public ItemStack cardStack() {
        if (cardSlot < 0 || cardSlot >= inventory.getContainerSize()) return ItemStack.EMPTY;
        ItemStack stack = inventory.getItem(cardSlot);
        return stack.getItem() instanceof QuarryCardItem ? stack : ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            ItemStack source = slots.get(slotId).getItem();
            ItemStack card = cardStack();
            if (!source.isEmpty() && !(source.getItem() instanceof QuarryCardItem) && !card.isEmpty() && !player.level().isClientSide()) {
                if (input == ContainerInput.QUICK_MOVE) QuarryCardItem.addTagsFromItem(card, source);
                else QuarryCardItem.addFilterItem(card, source, player.registryAccess());
                broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStack card = cardStack();
        if (card.isEmpty()) return false;
        if (id >= 1 && id <= 3) {
            QuarryCardItem.toggle(card, id);
        } else if (id == 4) {
            QuarryCardItem.clearFilter(card);
        } else if (id >= REMOVE_BASE && id < REMOVE_BASE + QuarryCardItem.MAX_FILTER_ENTRIES) {
            QuarryCardItem.removeEntry(card, id - REMOVE_BASE, player.registryAccess());
        } else if (id >= EXPAND_BASE && id < EXPAND_BASE + QuarryCardItem.MAX_FILTER_ENTRIES) {
            QuarryCardItem.expandEntryToTags(card, id - EXPAND_BASE, player.registryAccess());
        } else if (id >= TOGGLE_RULE_BASE && id < TOGGLE_RULE_BASE + QuarryCardItem.MAX_FILTER_ENTRIES) {
            QuarryCardItem.toggleEntryRule(card, id - TOGGLE_RULE_BASE);
        } else return false;
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        ItemStack source = slots.get(index).getItem();
        ItemStack card = cardStack();
        if (source.isEmpty() || source.getItem() instanceof QuarryCardItem || card.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = source.copy();
        if (!player.level().isClientSide()) QuarryCardItem.addTagsFromItem(card, source);
        broadcastChanges();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return !cardStack().isEmpty();
    }
}
