package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ConstructorBootstrap {
    public static final String MOD_ID = "rftoolsbuilder";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static boolean initialized;

    public static final DeferredBlock<ConstructorBlock> CONSTRUCTOR = BLOCKS.registerBlock(
            "constructor",
            ConstructorBlock::new,
            props -> props.strength(5.0f, 12.0f).sound(SoundType.METAL).noOcclusion()
    );

    public static final DeferredBlock<Block> CONSTRUCTOR_TURRET_VISUAL = BLOCKS.registerBlock(
            "constructor_turret_visual",
            Block::new,
            props -> props.noCollision().noOcclusion().strength(-1.0f, 3_600_000.0f)
    );
    public static final DeferredBlock<Block> CONSTRUCTOR_BARREL_VISUAL = BLOCKS.registerBlock(
            "constructor_barrel_visual",
            Block::new,
            props -> props.noCollision().noOcclusion().strength(-1.0f, 3_600_000.0f)
    );
    public static final DeferredBlock<Block> CONSTRUCTOR_ENERGY_VISUAL = BLOCKS.registerBlock(
            "constructor_energy_visual",
            Block::new,
            props -> props.noCollision().noOcclusion().strength(-1.0f, 3_600_000.0f).lightLevel(state -> 12)
    );

    public static final DeferredItem<BlockItem> CONSTRUCTOR_ITEM = ITEMS.registerSimpleBlockItem(CONSTRUCTOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConstructorBlockEntity>> CONSTRUCTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("constructor", () -> new BlockEntityType<>(ConstructorBlockEntity::new, false, CONSTRUCTOR.get()));

    private ConstructorBootstrap() {
    }

    public static synchronized void init(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(ConstructorBootstrap::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                CONSTRUCTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.energyStorage()
        );
    }
}
