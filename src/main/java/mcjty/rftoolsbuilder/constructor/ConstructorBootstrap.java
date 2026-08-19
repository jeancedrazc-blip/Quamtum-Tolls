package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ConstructorBootstrap.MOD_ID)
public final class ConstructorBootstrap {
    public static final String MOD_ID = "rftoolsbuilder";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<ConstructorBlock> CONSTRUCTOR = BLOCKS.registerBlock(
            "constructor",
            ConstructorBlock::new,
            props -> props.strength(5.0f, 12.0f).sound(SoundType.METAL).noOcclusion()
    );

    public static final DeferredItem<BlockItem> CONSTRUCTOR_ITEM = ITEMS.registerSimpleBlockItem(CONSTRUCTOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConstructorBlockEntity>> CONSTRUCTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("constructor", () -> new BlockEntityType<>(ConstructorBlockEntity::new, false, CONSTRUCTOR.get()));

    public ConstructorBootstrap(IEventBus modBus, ModContainer container) {
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
