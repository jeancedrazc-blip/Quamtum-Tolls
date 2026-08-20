package mcjty.rftoolsbuilder.constructor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueInputContextHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 26.1 moved BlockEntity deserialization from raw CompoundTag to ValueInput.
 * Keep external schematic BE payload support isolated here so loaders stay
 * format-neutral and a future mapping change only touches this bridge.
 */
final class ConstructorBlockEntityDataCompat {
    private ConstructorBlockEntityDataCompat() {}

    static boolean apply(BlockEntity blockEntity, CompoundTag data, ServerLevel level) {
        if (blockEntity == null || data == null || data.isEmpty()) return false;
        try {
            ValueInputContextHelper helper = new ValueInputContextHelper(level.registryAccess(), NbtOps.INSTANCE);
            ValueInput input = findInput(helper, data);
            if (input == null) return false;
            blockEntity.loadWithComponents(input);
            blockEntity.setChanged();
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static ValueInput findInput(ValueInputContextHelper helper, CompoundTag data) throws ReflectiveOperationException {
        for (Method method : ValueInputContextHelper.class.getDeclaredMethods()) {
            if (!ValueInput.class.isAssignableFrom(method.getReturnType()) || method.getParameterCount() != 1) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            if (!parameter.isAssignableFrom(CompoundTag.class)) continue;
            if (!method.canAccess(Modifier.isStatic(method.getModifiers()) ? null : helper)) method.setAccessible(true);
            Object result = method.invoke(Modifier.isStatic(method.getModifiers()) ? null : helper, data);
            if (result instanceof ValueInput input) return input;
        }
        return null;
    }
}
