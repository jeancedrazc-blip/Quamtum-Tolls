package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorEntityRequirementRegistry;
import mcjty.rftoolsbuilder.constructor.ConstructorRequirement;
import mcjty.rftoolsbuilder.constructor.SchematicTransform;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntityEntry;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the same supported schematic entities that the Constructor printer
 * can materialize. Detached client entities are cached per plan/transform and
 * are never inserted into the ClientLevel.
 */
final class SchematicEntityPreviewRenderer {
    private static ConstructionPlan cachedPlan;
    private static SchematicTransform cachedTransform;
    private static List<Entity> cachedEntities = List.of();

    private SchematicEntityPreviewRenderer() {}

    static void invalidate() {
        cachedPlan = null;
        cachedTransform = null;
        cachedEntities = List.of();
    }

    static void render(ConstructionPlan plan, SchematicTransform transform, SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || plan == null || !plan.hasEntities() || transform == null) return;
        ensureCache(plan, transform);
        if (cachedEntities.isEmpty()) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        var cameraState = event.getLevelRenderState().cameraRenderState;
        Vec3 camera = cameraState.pos;

        for (Entity entity : cachedEntities) {
            try {
                EntityRenderState renderState = dispatcher.extractEntity(entity, 0.0f);
                dispatcher.submit(
                        renderState,
                        cameraState,
                        entity.getX() - camera.x,
                        entity.getY() - camera.y,
                        entity.getZ() - camera.z,
                        event.getPoseStack(),
                        event.getSubmitNodeCollector()
                );
            } catch (RuntimeException ignored) {
                // A third-party renderer must not kill the entire schematic preview.
            }
        }
    }

    private static void ensureCache(ConstructionPlan plan, SchematicTransform transform) {
        if (cachedPlan == plan && transform.equals(cachedTransform)) return;
        cachedPlan = plan;
        cachedTransform = transform;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            cachedEntities = List.of();
            return;
        }

        ArrayList<Entity> entities = new ArrayList<>();
        for (ConstructionEntityEntry entry : plan.entities()) {
            CompoundTag data = entry.entityDataCopy();
            Vec3 worldPos = transform.transformWorld(entry.relativePos());
            writePosition(data, worldPos);
            data.remove("UUID");
            data.remove("UUIDMost");
            data.remove("UUIDLeast");

            try {
                Entity entity = EntityType.create(
                        TagValueInput.create(ProblemReporter.DISCARDING, mc.level.registryAccess(), data),
                        mc.level,
                        EntitySpawnReason.STRUCTURE
                ).orElse(null);
                if (entity == null || entity.getType().onlyOpCanSetNbt()) continue;

                // Keep preview and printer support sets identical. Unknown
                // entities are not shown as if they were constructible.
                ConstructorRequirement requirement = ConstructorEntityRequirementRegistry.resolve(entity);
                if (requirement.isInvalid()) continue;

                float yRot = entity.rotate(transform.vanillaRotation());
                yRot += entity.mirror(transform.vanillaMirror()) - entity.getYRot();
                entity.snapTo(worldPos.x, worldPos.y, worldPos.z, yRot, entity.getXRot());
                entity.setYBodyRot(yRot);
                entity.setYHeadRot(yRot);
                entities.add(entity);
            } catch (RuntimeException ignored) {
                // Bad entity NBT is isolated exactly like a bad BE preview.
            }
        }
        cachedEntities = List.copyOf(entities);
    }

    private static void writePosition(CompoundTag data, Vec3 pos) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(pos.x));
        list.add(DoubleTag.valueOf(pos.y));
        list.add(DoubleTag.valueOf(pos.z));
        data.put("Pos", list);
    }
}
