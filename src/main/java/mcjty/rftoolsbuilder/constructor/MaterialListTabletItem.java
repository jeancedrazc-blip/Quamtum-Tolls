package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.client.MaterialListTabletClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/** Portable, scrollable bill of materials written by the Constructor. */
public final class MaterialListTabletItem extends Item {
    public MaterialListTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            MaterialListTabletClient.open(player.getItemInHand(hand), hand);
        } else if (level instanceof ServerLevel server) {
            refreshFromLinkedConstructor(server, player.getItemInHand(hand));
            player.getInventory().setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    static void refreshFromLinkedConstructor(ServerLevel level, ItemStack tablet) {
        CustomData data = tablet.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        var tag = data.copyTag();
        String linkedDimension = tag.getString("QTConstructorDimension").orElse("");
        if (!linkedDimension.equals(level.dimension().identifier().toString())) return;
        long packed = tag.getLongOr("QTConstructorPos", Long.MIN_VALUE);
        if (packed == Long.MIN_VALUE) return;
        BlockPos pos = BlockPos.of(packed);
        if (!level.hasChunkAt(pos)) return;
        if (level.getBlockEntity(pos) instanceof ConstructorBlockEntity constructor) {
            constructor.refreshMaterialTablet(tablet);
        }
    }

    public static boolean isWritten(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
    }
}
