package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opt-in entity NBT sanitizer.
 *
 * Schematic entity data is never replayed verbatim. UUIDs, position, motion,
 * passengers, inventories/equipment and arbitrary mod capability payloads are
 * stripped by default. This preserves the entity type and a small cosmetic
 * surface while preventing a Constructor shot from cloning contents or nested
 * entities. Mods may register a stricter type-specific sanitizer.
 */
public final class ConstructorSafeEntityData {
    @FunctionalInterface
    public interface Sanitizer {
        CompoundTag sanitize(CompoundTag source);
    }

    private static final Map<EntityType<?>, Sanitizer> SANITIZERS = new ConcurrentHashMap<>();
    private static final Set<String> UNIVERSAL_SAFE = Set.of(
            "CustomName", "custom_name", "CustomNameVisible", "Silent", "NoGravity", "Glowing"
    );
    private static boolean defaultsRegistered;

    private ConstructorSafeEntityData() {}

    public static void register(EntityType<?> type, Sanitizer sanitizer) {
        if (type != null && sanitizer != null) SANITIZERS.put(type, sanitizer);
    }

    public static synchronized void registerDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;

        registerKeys("minecraft:armor_stand",
                "Invisible", "Small", "ShowArms", "NoBasePlate", "Marker", "Pose", "DisabledSlots");
        registerKeys("minecraft:item_frame", "Invisible", "Fixed", "ItemRotation", "Facing", "facing");
        registerKeys("minecraft:glow_item_frame", "Invisible", "Fixed", "ItemRotation", "Facing", "facing");
        registerKeys("minecraft:painting", "variant", "Variant", "Facing", "facing");
        registerKeys("minecraft:end_crystal", "ShowBottom", "show_bottom");
    }

    public static CompoundTag sanitize(CompoundTag raw) {
        if (raw == null || raw.isEmpty()) return null;
        EntityType<?> type = ConstructorEntityRequirementRegistry.resolveType(raw);
        if (type == null) return null;

        CompoundTag base = new CompoundTag();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return null;
        base.putString("id", id.toString());
        copyKeys(raw, base, UNIVERSAL_SAFE);

        Sanitizer sanitizer = SANITIZERS.get(type);
        if (sanitizer != null) {
            try {
                CompoundTag extra = sanitizer.sanitize(raw.copy());
                if (extra != null) {
                    for (String key : extra.keySet()) {
                        if (isForbiddenKey(key)) continue;
                        Tag value = extra.get(key);
                        if (value != null) base.put(key, value.copy());
                    }
                }
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        // Never trust location/identity/runtime hierarchy from an external file.
        for (String key : Set.of("UUID", "UUIDMost", "UUIDLeast", "Pos", "Motion", "Rotation", "Passengers")) base.remove(key);
        return base;
    }

    private static void registerKeys(String id, String... keys) {
        EntityType<?> type;
        try {
            type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
        } catch (RuntimeException ignored) {
            return;
        }
        if (type == null) return;
        register(type, source -> {
            CompoundTag result = new CompoundTag();
            for (String key : keys) {
                Tag value = source.get(key);
                if (value != null) result.put(key, value.copy());
            }
            return result;
        });
    }

    private static void copyKeys(CompoundTag source, CompoundTag destination, Set<String> keys) {
        for (String key : keys) {
            Tag value = source.get(key);
            if (value != null) destination.put(key, value.copy());
        }
    }

    private static boolean isForbiddenKey(String key) {
        if (key == null) return true;
        String lower = key.toLowerCase();
        return lower.equals("uuid") || lower.equals("pos") || lower.equals("motion") || lower.equals("rotation")
                || lower.equals("passengers") || lower.equals("items") || lower.equals("inventory")
                || lower.equals("item") || lower.equals("equipment") || lower.equals("armoritems")
                || lower.equals("handitems") || lower.equals("loottable") || lower.equals("loottableseed")
                || lower.equals("owner") || lower.equals("owneruuid") || lower.equals("energy")
                || lower.equals("tank") || lower.equals("fluids") || lower.equals("capabilities");
    }
}
