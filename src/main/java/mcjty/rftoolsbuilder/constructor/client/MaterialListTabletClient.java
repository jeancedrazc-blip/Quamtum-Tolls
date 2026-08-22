package mcjty.rftoolsbuilder.constructor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

public final class MaterialListTabletClient {
    private MaterialListTabletClient() {}

    public static void open(ItemStack tablet, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new MaterialListTabletScreen(tablet, hand));
    }
}
