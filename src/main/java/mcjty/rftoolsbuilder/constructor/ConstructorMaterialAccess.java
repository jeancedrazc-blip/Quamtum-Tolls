package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Material bridge for the Constructor. Uses NeoForge's transactional item capability,
 * so modded inventories work without hard dependencies on the inventory's mod.
 */
final class ConstructorMaterialAccess {
    private ConstructorMaterialAccess() {
    }

    static boolean isRepresentable(BlockState state) {
        return state != null && state.getBlock().asItem() != Items.AIR;
    }

    static boolean hasOne(ServerLevel level, BlockPos machinePos, BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return false;
        }
        ItemResource resource = ItemResource.of(item);

        for (Direction direction : Direction.values()) {
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK,
                    machinePos.relative(direction),
                    direction.getOpposite()
            );
            if (handler == null) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(resource, 1, transaction) == 1) {
                    // Intentionally do not commit: closing the transaction rolls the probe back.
                    return true;
                }
            }
        }
        return false;
    }

    static boolean extractOne(ServerLevel level, BlockPos machinePos, BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return false;
        }
        ItemResource resource = ItemResource.of(item);

        for (Direction direction : Direction.values()) {
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK,
                    machinePos.relative(direction),
                    direction.getOpposite()
            );
            if (handler == null) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(resource, 1, transaction) == 1) {
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }
}
