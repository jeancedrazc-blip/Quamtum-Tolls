package mcjty.rftoolsbuilder.constructor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mcjty.rftoolsbuilder.constructor.ConstructorBlockEntity;
import mcjty.rftoolsbuilder.constructor.ConstructorBootstrap;
import mcjty.rftoolsbuilder.constructor.ConstructorStatus;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ConstructorBlockEntityRenderer implements BlockEntityRenderer<ConstructorBlockEntity, ConstructorRenderState> {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    // The barrel trunnion is deliberately above the one-block base. The old
    // 0.52 Y pivot made the approved long cannon rotate almost on the ground.
    private static final double PIVOT_X = 0.5;
    private static final double PIVOT_Y = 0.96875; // 15.5 model pixels
    private static final double PIVOT_Z = 0.5;
    private static final double MUZZLE_DISTANCE = 1.50;

    private final BlockModelResolver blockResolver;

    public ConstructorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockResolver = context.blockModelResolver();
    }

    @Override
    public ConstructorRenderState createRenderState() {
        return new ConstructorRenderState();
    }

    @Override
    public void extractRenderState(
            ConstructorBlockEntity blockEntity,
            ConstructorRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, crumblingOverlay);

        this.blockResolver.update(state.turret, ConstructorBootstrap.CONSTRUCTOR_TURRET_VISUAL.get().defaultBlockState(), DISPLAY_CONTEXT);
        this.blockResolver.update(state.barrel, ConstructorBootstrap.CONSTRUCTOR_BARREL_VISUAL.get().defaultBlockState(), DISPLAY_CONTEXT);
        this.blockResolver.update(state.energyChannel, ConstructorBootstrap.CONSTRUCTOR_ENERGY_VISUAL.get().defaultBlockState(), DISPLAY_CONTEXT);

        BlockPos origin = blockEntity.getBlockPos();
        BlockPos target = blockEntity.targetPos();
        BlockState targetState = blockEntity.targetState();
        state.hasTarget = target != null && targetState != null;

        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.yaw = homeYaw(facing);
        state.pitch = 0.0f;
        state.projectileVisible = false;
        state.projectileProgress = 0.0f;
        state.energyActive = blockEntity.status() == ConstructorStatus.CHARGING || blockEntity.status() == ConstructorStatus.FIRING;

        if (!state.hasTarget) {
            return;
        }

        this.blockResolver.update(state.projectile, targetState, DISPLAY_CONTEXT);

        state.targetX = target.getX() - origin.getX() + 0.5;
        state.targetY = target.getY() - origin.getY() + 0.5;
        state.targetZ = target.getZ() - origin.getZ() + 0.5;

        double dx = state.targetX - PIVOT_X;
        double dy = state.targetY - PIVOT_Y;
        double dz = state.targetZ - PIVOT_Z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        state.yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        state.pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(1.0e-5, horizontal)));

        if (blockEntity.status() == ConstructorStatus.FIRING) {
            state.projectileVisible = true;
            state.projectileProgress = clamp01((blockEntity.shotProgress() + partialTick) / (float) ConstructorBlockEntity.FLIGHT_TICKS);
        }
    }

    @Override
    public void submit(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        submitTurret(state, poseStack, collector);
        submitBarrel(state, poseStack, collector);
        if (state.energyActive) {
            submitEnergy(state, poseStack, collector);
        }
        if (state.projectileVisible && state.hasTarget) {
            submitProjectile(state, poseStack, collector);
        }
    }

    private static void submitTurret(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.yaw, 0.0f);
        state.turret.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitBarrel(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.yaw, state.pitch);
        state.barrel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitEnergy(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.yaw, state.pitch);
        state.energyChannel.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitProjectile(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        double dx = state.targetX - PIVOT_X;
        double dy = state.targetY - PIVOT_Y;
        double dz = state.targetZ - PIVOT_Z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-5) {
            return;
        }

        double nx = dx / length;
        double ny = dy / length;
        double nz = dz / length;
        double muzzleX = PIVOT_X + nx * MUZZLE_DISTANCE;
        double muzzleY = PIVOT_Y + ny * MUZZLE_DISTANCE;
        double muzzleZ = PIVOT_Z + nz * MUZZLE_DISTANCE;

        float p = smoothStep(state.projectileProgress);
        double x = lerp(muzzleX, state.targetX, p);
        double y = lerp(muzzleY, state.targetY, p) + Math.sin(Math.PI * p) * 0.32;
        double z = lerp(muzzleZ, state.targetZ, p);

        final float scale = 0.38f;
        poseStack.pushPose();
        poseStack.translate(x - scale * 0.5, y - scale * 0.5, z - scale * 0.5);
        poseStack.scale(scale, scale, scale);
        state.projectile.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void rotateAroundPivot(PoseStack poseStack, float yaw, float pitch) {
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
    }

    private static float homeYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0f;
            case EAST -> 90.0f;
            case NORTH -> 180.0f;
            case WEST -> -90.0f;
            default -> 0.0f;
        };
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smoothStep(float value) {
        float t = clamp01(value);
        return t * t * (3.0f - 2.0f * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
