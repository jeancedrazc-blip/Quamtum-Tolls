package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.client.MaterialListTabletClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Portable, scrollable bill of materials written by the Constructor. */
public final class MaterialListTabletItem extends Item {
    public MaterialListTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            MaterialListTabletClient.open(player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean isWritten(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
    }
}
