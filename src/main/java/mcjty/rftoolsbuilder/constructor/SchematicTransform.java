package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Discrete schematic transform shared by client preview and server printing.
 * The anchor is always the minimum corner of the transformed structure.
 */
public record SchematicTransform(BlockPos anchor, int rotationQuarterTurns, int mirrorMode, int sizeX, int sizeY, int sizeZ) {
    public SchematicTransform {
        anchor = anchor == null ? BlockPos.ZERO : anchor.immutable();
        rotationQuarterTurns = Math.floorMod(rotationQuarterTurns, 4);
        mirrorMode = Math.max(0, Math.min(2, mirrorMode));
        sizeX = Math.max(0, sizeX);
        sizeY = Math.max(0, sizeY);
        sizeZ = Math.max(0, sizeZ);
    }

    public Rotation vanillaRotation() {
        return switch (rotationQuarterTurns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public Mirror vanillaMirror() {
        return switch (mirrorMode) {
            case 1 -> Mirror.LEFT_RIGHT;
            case 2 -> Mirror.FRONT_BACK;
            default -> Mirror.NONE;
        };
    }

    public int transformedSizeX() {
        return (rotationQuarterTurns & 1) == 0 ? sizeX : sizeZ;
    }

    public int transformedSizeZ() {
        return (rotationQuarterTurns & 1) == 0 ? sizeZ : sizeX;
    }

    public BlockPos transformRelative(BlockPos source) {
        int x = source.getX();
        int y = source.getY();
        int z = source.getZ();

        // Match vanilla Mirror semantics: LEFT_RIGHT flips Z, FRONT_BACK flips X.
        if (mirrorMode == 1) z = sizeZ - 1 - z;
        if (mirrorMode == 2) x = sizeX - 1 - x;

        int tx;
        int tz;
        switch (rotationQuarterTurns) {
            case 1 -> {
                tx = sizeZ - 1 - z;
                tz = x;
            }
            case 2 -> {
                tx = sizeX - 1 - x;
                tz = sizeZ - 1 - z;
            }
            case 3 -> {
                tx = z;
                tz = sizeX - 1 - x;
            }
            default -> {
                tx = x;
                tz = z;
            }
        }
        return new BlockPos(tx, y, tz);
    }

    public BlockPos transformWorld(BlockPos source) {
        return anchor.offset(transformRelative(source));
    }

    public BlockState transformState(BlockState source) {
        BlockState result = source;
        Mirror mirror = vanillaMirror();
        if (mirror != Mirror.NONE) result = result.mirror(mirror);
        Rotation rotation = vanillaRotation();
        if (rotation != Rotation.NONE) result = result.rotate(rotation);
        return result;
    }
}
