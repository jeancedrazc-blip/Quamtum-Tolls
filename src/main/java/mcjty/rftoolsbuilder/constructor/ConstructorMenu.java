package mcjty.rftoolsbuilder.constructor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class ConstructorMenu extends AbstractContainerMenu {
    private final ConstructorBlockEntity constructor;
    private final ContainerData data;

    public ConstructorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ConstructorBlockEntity found ? found : null,
                new SimpleContainerData(8));
    }

    public ConstructorMenu(int id, Inventory inventory, ConstructorBlockEntity constructor, ContainerData data) {
        super(ConstructorBootstrap.CONSTRUCTOR_MENU.get(), id);
        this.constructor = constructor;
        this.data = data;
        addDataSlots(data);
    }

    public ConstructorBlockEntity constructor() { return constructor; }
    public ContainerData data() { return data; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return constructor != null && constructor.handleMenuButton(id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (constructor == null || constructor.isRemoved() || constructor.getLevel() != player.level()) return false;
        var pos = constructor.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
