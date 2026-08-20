package mcjty.rftoolsbuilder.constructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Safe creation and materialization of normalized schematic entities. */
final class ConstructorEntityDataCompat {
    private ConstructorEntityDataCompat() {}

    static Entity createDetached(ServerLevel level, CompoundTag rawData) {
        if (level == null || rawData == null || rawData.isEmpty()) return null;
        CompoundTag data = sanitizeIdentity(rawData.copy());
        try {
            Entity entity = EntityType.loadEntityRecursive(
                    TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data),
                    level,
                    EntitySpawnReason.STRUCTURE,
                    loaded -> loaded
            );
            if (entity == null || entity.getType().onlyOpCanSetNbt()) return null;
            return entity;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static boolean canMaterialize(ServerLevel level, CompoundTag rawData, Vec3 worldPos, SchematicTransform transform) {
        Entity entity = prepare(level, rawData, worldPos, transform);
        if (entity == null) return false;
        return !(entity instanceof HangingEntity hanging) || hanging.survives();
    }

    static boolean materialize(ServerLevel level, CompoundTag rawData, Vec3 worldPos, SchematicTransform transform) {
        Entity entity = prepare(level, rawData, worldPos, transform);
        if (entity == null) return false;
        if (entity instanceof HangingEntity hanging && !hanging.survives()) return false;
        level.addFreshEntityWithPassengers(entity);
        return true;
    }

    private static Entity prepare(ServerLevel level, CompoundTag rawData, Vec3 worldPos, SchematicTransform transform) {
        if (level == null || rawData == null || rawData.isEmpty() || worldPos == null || transform == null) return null;
        CompoundTag data = sanitizeIdentity(rawData.copy());
        writeVec(data, "Pos", worldPos);

        Entity entity;
        try {
            entity = EntityType.loadEntityRecursive(
                    TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data),
                    level,
                    EntitySpawnReason.STRUCTURE,
                    loaded -> loaded
            );
        } catch (RuntimeException exception) {
            return null;
        }
        if (entity == null || entity.getType().onlyOpCanSetNbt()) return null;

        // Vanilla StructureTemplate applies orientation after loading the NBT.
        // Use the same sequence so ItemFrames, ArmorStands and modded entities
        // see the same mirror/rotation semantics as a normal structure place.
        float yRot = entity.rotate(transform.vanillaRotation());
        yRot += entity.mirror(transform.vanillaMirror()) - entity.getYRot();
        entity.snapTo(worldPos.x, worldPos.y, worldPos.z, yRot, entity.getXRot());
        entity.setYBodyRot(yRot);
        entity.setYHeadRot(yRot);
        return entity;
    }

    private static CompoundTag sanitizeIdentity(CompoundTag data) {
        data.remove("UUID");
        data.remove("UUIDMost");
        data.remove("UUIDLeast");
        data.remove("Dimension");
        data.remove("PortalCooldown");
        return data;
    }

    private static void writeVec(CompoundTag data, String key, Vec3 value) {
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(value.x));
        pos.add(DoubleTag.valueOf(value.y));
        pos.add(DoubleTag.valueOf(value.z));
        data.put(key, pos);
    }
}
