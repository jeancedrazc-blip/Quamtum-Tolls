package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.ConstructionEntityEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create-style entity bridge. Only entities with an explicit material rule are
 * printable. Unknown/modded entities can opt in through the public registry.
 */
public final class ConstructorEntitySupport {
    @FunctionalInterface
    public interface RequirementProvider {
        ConstructorRequirement get(Entity entity);
    }

    private static final Map<EntityType<?>, RequirementProvider> REQUIREMENTS = new ConcurrentHashMap<>();

    private ConstructorEntitySupport() {}

    public static void register(EntityType<?> type, RequirementProvider provider) {
        if (type != null && provider != null) REQUIREMENTS.put(type, provider);
    }

    public record Prepared(Entity entity, ConstructorRequirement requirement, Vec3 target, ItemStack projectileStack) {}

    public static Prepared prepare(ServerLevel level, ConstructionEntityEntry entry, SchematicTransform transform) {
        if (entry == null || transform == null) return null;
        CompoundTag data = entry.entityDataCopy();
        if (data.isEmpty()) return null;

        Entity entity;
        try {
            entity = EntityType.create(data, level).orElse(null);
        } catch (RuntimeException exception) {
            return null;
        }
        if (entity == null || entity.onlyOpCanSetNbt()) return null;

        Vec3 target = transform.transformWorld(entry.relativePos());
        entity.setPos(target.x, target.y, target.z);
        float yaw = transformEntityYaw(entity.getYRot(), transform);
        entity.setYRot(yaw);
        entity.setOldPosAndRot();
        entity.yRotO = yaw;

        ConstructorRequirement requirement = requirement(entity);
        if (requirement.isInvalid()) return null;
        ItemStack projectile = firstVisualStack(requirement);
        return new Prepared(entity, requirement, target, projectile);
    }

    public static boolean spawn(ServerLevel level, Prepared prepared) {
        if (prepared == null || prepared.entity() == null) return false;
        try {
            return level.addFreshEntity(prepared.entity());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static ConstructorRequirement requirement(Entity entity) {
        RequirementProvider provider = REQUIREMENTS.get(entity.getType());
        if (provider != null) {
            try {
                ConstructorRequirement custom = provider.get(entity);
                return custom == null ? ConstructorRequirement.INVALID : custom;
            } catch (RuntimeException ignored) {
                return ConstructorRequirement.INVALID;
            }
        }

        if (entity instanceof ItemFrame frame) {
            ItemStack frameStack = new ItemStack(entity.getType() == EntityType.GLOW_ITEM_FRAME ? Items.GLOW_ITEM_FRAME : Items.ITEM_FRAME);
            ConstructorRequirement result = ConstructorRequirement.consume(frameStack, false);
            ItemStack displayed = frame.getItem();
            if (!displayed.isEmpty()) result = result.union(ConstructorRequirement.consume(displayed.copyWithCount(1), true));
            return result;
        }

        if (entity instanceof ArmorStand armorStand) {
            List<ConstructorRequirement.StackRequirement> stacks = new ArrayList<>();
            stacks.add(new ConstructorRequirement.StackRequirement(new ItemStack(Items.ARMOR_STAND), ConstructorRequirement.Use.CONSUME, false));
            for (ItemStack equipment : armorStand.getAllSlots()) {
                if (!equipment.isEmpty()) stacks.add(new ConstructorRequirement.StackRequirement(equipment.copyWithCount(1), ConstructorRequirement.Use.CONSUME, true));
            }
            return new ConstructorRequirement(stacks);
        }

        if (entity instanceof Painting) return ConstructorRequirement.consume(Items.PAINTING, 1);
        return ConstructorRequirement.INVALID;
    }

    private static ItemStack firstVisualStack(ConstructorRequirement requirement) {
        if (requirement == null) return ItemStack.EMPTY;
        for (ConstructorRequirement.StackRequirement stack : requirement.requirements()) {
            if (!stack.stack().isEmpty()) return stack.stack().copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    private static float transformEntityYaw(float yaw, SchematicTransform transform) {
        float result = yaw;
        // Mirror around the corresponding schematic axis before applying rotation.
        if (transform.mirrorMode() == 1) result = 180.0f - result;
        if (transform.mirrorMode() == 2) result = -result;
        result += transform.rotationQuarterTurns() * 90.0f;
        result %= 360.0f;
        if (result < -180.0f) result += 360.0f;
        if (result > 180.0f) result -= 360.0f;
        return result;
    }
}
