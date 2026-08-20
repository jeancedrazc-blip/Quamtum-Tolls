package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Material requirement registry for schematic entities.
 *
 * Unknown entities are deliberately NOT free. Mods can register an explicit
 * resolver, while vanilla portable/decorative entities fall back to the item
 * with the same registry id (boats, minecarts, armor stands, item frames, etc.).
 * Living mobs, projectiles, dropped items and transient entities are rejected
 * by default so a schematic can never become an entity/item duplication path.
 */
public final class ConstructorEntityRequirementRegistry {
    private static final Map<EntityType<?>, Function<CompoundTag, ConstructorRequirement>> RESOLVERS = new ConcurrentHashMap<>();
    private static final Set<String> EXACT_PORTABLE = Set.of(
            "armor_stand", "item_frame", "glow_item_frame", "end_crystal",
            "painting", "minecart", "chest_minecart", "furnace_minecart",
            "hopper_minecart", "tnt_minecart"
    );

    private ConstructorEntityRequirementRegistry() {}

    public static void register(EntityType<?> type, Function<CompoundTag, ConstructorRequirement> resolver) {
        if (type != null && resolver != null) RESOLVERS.put(type, resolver);
    }

    public static ConstructorRequirement resolve(CompoundTag raw) {
        EntityType<?> type = resolveType(raw);
        if (type == null) return ConstructorRequirement.INVALID;

        Function<CompoundTag, ConstructorRequirement> resolver = RESOLVERS.get(type);
        if (resolver != null) {
            try {
                ConstructorRequirement requirement = resolver.apply(raw == null ? new CompoundTag() : raw.copy());
                return requirement == null ? ConstructorRequirement.INVALID : requirement;
            } catch (RuntimeException ignored) {
                return ConstructorRequirement.INVALID;
            }
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null || !portableByDefault(id)) return ConstructorRequirement.INVALID;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == null || item == Items.AIR ? ConstructorRequirement.INVALID : ConstructorRequirement.consume(item, 1);
    }

    public static EntityType<?> resolveType(CompoundTag raw) {
        if (raw == null || raw.isEmpty()) return null;
        String id = raw.getString("id").orElse("");
        if (id.isBlank()) id = raw.getString("Id").orElse("");
        if (id.isBlank()) return null;
        try {
            return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean portableByDefault(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) return false;
        String path = id.getPath();
        if (EXACT_PORTABLE.contains(path)) return true;
        return path.endsWith("_boat") || path.endsWith("_chest_boat")
                || path.endsWith("_raft") || path.endsWith("_chest_raft");
    }
}
