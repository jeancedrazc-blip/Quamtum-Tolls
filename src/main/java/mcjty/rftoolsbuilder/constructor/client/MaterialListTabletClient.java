package mcjty.rftoolsbuilder.constructor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class MaterialListTabletClient {
    private MaterialListTabletClient() {}

    public static void open(ItemStack tablet) {
        Minecraft.getInstance().setScreen(new MaterialListTabletScreen(tablet.copy()));
    }
}
