package mcjty.rftoolsbuilder.constructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/** Authoritative, sanitized placement path for schematic entities. */
public final class ConstructorEntityPlacementHelper {
    private ConstructorEntityPlacementHelper() {}

    public static boolean place(ServerLevel level, Vec3 target, CompoundTag rawEntityData, SchematicTransform transform) {
        if (level == null || target == null || rawEntityData == null || transform == null) return false;
        ConstructorSafeEntityData.registerDefaults();
        CompoundTag safe = ConstructorSafeEntityData.sanitize(rawEntityData);
        if (safe == null || safe.isEmpty()) return false;

        Entity entity;
        try {
            entity = EntityType.loadEntityRecursive(safe, level, EntitySpawnReason.STRUCTURE, e -> e);
        } catch (RuntimeException ignored) {
            return false;
        }
        if (entity == null) return false;

        float yaw = transformYaw(entity.getYRot(), transform);
        float pitch = entity.getXRot();
        try {
            entity.moveTo(target.x, target.y, target.z, yaw, pitch);
            entity.setDeltaMovement(Vec3.ZERO);
            return level.addFreshEntity(entity);
        } catch (RuntimeException ignored) {
            entity.discard();
            return false;
        }
    }

    /** Mirrors first, then rotates exactly like SchematicTransform.transformRelative(). */
    static float transformYaw(float sourceYaw, SchematicTransform transform) {
        float yaw = sourceYaw;
        if (transform.mirrorMode() == 1) yaw = 180.0f - yaw; // Z reflection / LEFT_RIGHT
        else if (transform.mirrorMode() == 2) yaw = -yaw;   // X reflection / FRONT_BACK
        yaw += transform.rotationQuarterTurns() * 90.0f;
        yaw %= 360.0f;
        if (yaw <= -180.0f) yaw += 360.0f;
        if (yaw > 180.0f) yaw -= 360.0f;
        return yaw;
    }
}
