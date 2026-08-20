package mcjty.rftoolsbuilder;

import mcjty.rftoolsbuilder.constructor.ConstructorBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Standalone source entry point used by the dev.10 Constructor validation build.
 *
 * The canonical complete source is still being reconstructed; this class makes
 * the source-built Constructor test JAR a real NeoForge mod without binary JAR
 * patching. All Constructor registrations originate from editable source.
 */
@Mod(ConstructorBootstrap.MOD_ID)
public final class QuantumToolsMod {
    public QuantumToolsMod(IEventBus modBus, ModContainer container) {
        ConstructorBootstrap.init(modBus);
    }
}
