package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Material bridge for the Constructor. It intentionally uses NeoForge's
 * transactional item capability so chests, drawers and modded inventories can
 * all feed the machine without hard dependencies.
 */
final class ConstructorMaterialAccess {
    private ConstructorMaterialAccess() {}

    static boolean isRepresentable(BlockState state) {
        if (state == null || state.isAir()) return true;
        if (!requiresMaterial(state)) return true;
        return state.getBlock().asItem() != Items.AIR;
    }

    /**
     * Parts that share one placement item (upper doors/tall plants, bed heads,
     * piston heads) must still be constructed, but must not consume a second item.
     */
    static boolean requiresMaterial(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.getBlock() == Blocks.STRUCTURE_VOID) return false;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return false;
        if (state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) return false;
        if (state.getBlock() instanceof PistonHeadBlock) return false;
        return true;
    }

    static Item materialItem(BlockState state) {
        return state == null ? Items.AIR : state.getBlock().asItem();
    }

    static boolean hasOne(ServerLevel level, BlockPos machinePos, BlockState state) {
        if (!requiresMaterial(state)) return true;
        Item item = materialItem(state);
        if (item == Items.AIR) return false;
        ItemResource resource = ItemResource.of(item);

        for (Direction direction : Direction.values()) {
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK,
                    machinePos.relative(direction),
                    direction.getOpposite()
            );
            if (handler == null) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                if (handler.extract(resource, 1, transaction) == 1) return true;
            }
        }
        return false;
    }

    static boolean extractOne(ServerLevel level, BlockPos machinePos, BlockState state) {
        if (!requiresMaterial(state)) return true;
        Item item = materialItem(state);
        if (item == Items.AIR) return false;
        ItemResource resource = ItemResource.of(item);

        for (Direction direction : Direction.values()) {
            ResourceHandler<ItemResource> handler = level.getCapability(
                    Capabilities.Item.BLOCK,
                    machinePos.relative(direction),
                    direction.getOpposite()
            );
            if (handler == null) continue;
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
