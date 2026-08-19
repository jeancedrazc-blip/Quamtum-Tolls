package mcjty.rftoolsbuilder.constructor.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class ConstructorRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState turret = new BlockModelRenderState();
    public final BlockModelRenderState barrel = new BlockModelRenderState();
    public final BlockModelRenderState energyChannel = new BlockModelRenderState();
    public final BlockModelRenderState projectile = new BlockModelRenderState();

    public float yaw;
    public float pitch;
    public float projectileProgress;
    public double targetX;
    public double targetY;
    public double targetZ;
    public boolean hasTarget;
    public boolean projectileVisible;
    public boolean energyActive;
}
