package mcjty.rftoolsbuilder.mixin;

import mcjty.rftoolsbuilder.BuilderBlockEntity;
import mcjty.rftoolsbuilder.QuarryMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuilderBlockEntity.class)
public abstract class BuilderSpeedMixin {
    private static final int WORK_INTERVAL_TICKS = 4;

    @Inject(method = "work", at = @At("HEAD"), cancellable = true)
    private void quantumtools$limitMiningRate(ServerLevel level, QuarryMode mode, ItemStack quarryCard, CallbackInfo ci) {
        if (Math.floorMod(level.getGameTime(), WORK_INTERVAL_TICKS) != 0L) {
            ci.cancel();
        }
    }
}
