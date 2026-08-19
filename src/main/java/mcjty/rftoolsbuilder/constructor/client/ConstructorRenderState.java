package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorStatus;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class ConstructorRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState turret = new BlockModelRenderState();
    public final BlockModelRenderState barrel = new BlockModelRenderState();
    public final BlockModelRenderState energyChannel = new BlockModelRenderState();
    public final BlockModelRenderState projectile = new BlockModelRenderState();

    public float targetYaw;
    public float targetPitch;
    public float displayYaw;
    public float displayPitch;
    public float recoil;
    public float energyPulse = 1.0f;
    public float projectileProgress;
    public double targetX;
    public double targetY;
    public double targetZ;
    public boolean initialized;
    public boolean hasTarget;
    public boolean projectileVisible;
    public ConstructorStatus status = ConstructorStatus.IDLE;
}
