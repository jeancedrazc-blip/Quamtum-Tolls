package mcjty.rftoolsbuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Builder control menu preserving the legacy card slots and config protocol. */
public final class BuilderMenu extends AbstractContainerMenu {
    public static final int CONFIG_BASE = 1000;
    public static final int CONFIG_BIAS = 16384;
    public static final int CONFIG_RANGE = 32769;

    private final BuilderBlockEntity builder;
    private final ContainerData data;

    public BuilderMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readBuilder(inventory, buffer), new SimpleContainerData(12));
    }

    private static BuilderBlockEntity readBuilder(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return inventory.player.level().getBlockEntity(pos) instanceof BuilderBlockEntity found ? found : null;
    }

    public BuilderMenu(int id, Inventory inventory, BuilderBlockEntity builder, ContainerData data) {
        super(RFToolsBuilder.BUILDER_MENU.get(), id);
        this.builder = builder;
        this.data = data;

        if (builder != null) {
            addSlot(new Slot(builder, BuilderBlockEntity.SLOT_SHAPE, 84, 42) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ShapeCardItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
            addSlot(new Slot(builder, BuilderBlockEntity.SLOT_QUARRY, 120, 42) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof QuarryCardItem; }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        int invX = 47, invY = 148;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, invX + col * 18, 206));
        addDataSlots(data);
    }

    public BuilderBlockEntity builder() { return builder; }
    public ContainerData data() { return data; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (builder == null) return false;
        if (id == 0) { builder.primaryAction(); return true; }
        if (id == 1) { builder.stopWork(); return true; }
        if (id >= CONFIG_BASE) {
            int code = id - CONFIG_BASE;
            int field = code / CONFIG_RANGE;
            int value = code % CONFIG_RANGE - CONFIG_BIAS;
            if (field < 0 || field > 5) return false;
            value = field < 3 ? Math.max(1, Math.min(512, value)) : Math.max(-16384, Math.min(16384, value));
            builder.setConfigValue(field, value);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        int machineSlots = builder == null ? 0 : 2;
        if (index < machineSlots) {
            if (!moveItemStackTo(source, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof QuarryCardItem) {
            if (builder == null || !moveItemStackTo(source, 1, 2, false)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof ShapeCardItem) {
            if (builder == null || !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return builder != null && builder.stillValid(player);
    }
}
