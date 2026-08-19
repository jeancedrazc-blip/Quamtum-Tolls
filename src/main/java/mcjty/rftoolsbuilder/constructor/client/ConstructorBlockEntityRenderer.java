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

    private static final double PIVOT_X = 0.5;
    private static final double PIVOT_Y = 0.96875;
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
        state.status = blockEntity.status();
        state.hasTarget = target != null && targetState != null;
        state.projectileVisible = false;
        state.projectileProgress = 0.0f;
        state.recoil = 0.0f;

        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float desiredYaw = homeYaw(facing);
        float desiredPitch = 0.0f;

        double time = partialTick;
        if (blockEntity.getLevel() != null) {
            time += blockEntity.getLevel().getGameTime();
        }

        if (!state.hasTarget && (state.status == ConstructorStatus.IDLE
                || state.status == ConstructorStatus.COMPLETE
                || state.status == ConstructorStatus.PAUSED)) {
            desiredYaw += (float) Math.sin(time * 0.035) * 1.6f;
            desiredPitch = (float) Math.sin(time * 0.045) * 0.7f;
        }

        if (state.hasTarget) {
            this.blockResolver.update(state.projectile, targetState, DISPLAY_CONTEXT);

            state.targetX = target.getX() - origin.getX() + 0.5;
            state.targetY = target.getY() - origin.getY() + 0.5;
            state.targetZ = target.getZ() - origin.getZ() + 0.5;

            double dx = state.targetX - PIVOT_X;
            double dy = state.targetY - PIVOT_Y;
            double dz = state.targetZ - PIVOT_Z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            desiredYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
            desiredPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(1.0e-5, horizontal)));
        }

        state.targetYaw = desiredYaw;
        state.targetPitch = desiredPitch;

        if (!state.initialized) {
            state.displayYaw = desiredYaw;
            state.displayPitch = desiredPitch;
            state.initialized = true;
        } else {
            float yawFactor = state.status == ConstructorStatus.AIMING ? 0.34f : 0.18f;
            float pitchFactor = state.status == ConstructorStatus.AIMING ? 0.30f : 0.16f;
            state.displayYaw = approachAngle(state.displayYaw, desiredYaw, yawFactor);
            state.displayPitch += (desiredPitch - state.displayPitch) * pitchFactor;
        }

        if (state.status == ConstructorStatus.CHARGING) {
            state.energyPulse = 1.02f + (float) ((Math.sin(time * 1.75) + 1.0) * 0.045);
        } else if (state.status == ConstructorStatus.FIRING) {
            state.energyPulse = 1.10f;
        } else if (state.status == ConstructorStatus.WAITING_ENERGY) {
            state.energyPulse = 0.94f + (float) ((Math.sin(time * 0.35) + 1.0) * 0.025);
        } else {
            state.energyPulse = 1.0f;
        }

        if (state.status == ConstructorStatus.FIRING && state.hasTarget) {
            state.projectileVisible = true;
            state.projectileProgress = clamp01((blockEntity.shotProgress() + partialTick) / (float) ConstructorBlockEntity.FLIGHT_TICKS);
            state.recoil = 0.145f * (1.0f - smoothStep(state.projectileProgress));
        }
    }

    @Override
    public void submit(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        submitTurret(state, poseStack, collector);
        submitBarrel(state, poseStack, collector);
        submitEnergy(state, poseStack, collector);
        if (state.projectileVisible && state.hasTarget) {
            submitProjectile(state, poseStack, collector);
        }
    }

    private static void submitTurret(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.displayYaw, 0.0f);
        state.turret.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitBarrel(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.displayYaw, state.displayPitch);
        poseStack.translate(0.0, 0.0, -state.recoil);
        state.barrel.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitEnergy(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        rotateAroundPivot(poseStack, state.displayYaw, state.displayPitch);
        poseStack.translate(0.0, 0.0, -state.recoil);
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.scale(state.energyPulse, state.energyPulse, 1.0f);
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        state.energyChannel.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitProjectile(ConstructorRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        double dx = state.targetX - PIVOT_X;
        double dy = state.targetY - PIVOT_Y;
        double dz = state.targetZ - PIVOT_Z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-5) return;

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

        float materialize = clamp01(state.projectileProgress * 3.0f);
        float scale = 0.26f + 0.12f * smoothStep(materialize);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(360.0f * p));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * p));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.5, -0.5);
        state.projectile.submit(poseStack, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void rotateAroundPivot(PoseStack poseStack, float yaw, float pitch) {
        poseStack.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
    }

    private static float approachAngle(float current, float target, float factor) {
        float delta = wrapDegrees(target - current);
        return current + delta * factor;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0f;
        if (value <= -180.0f) value += 360.0f;
        if (value > 180.0f) value -= 360.0f;
        return value;
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
