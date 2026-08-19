package mcjty.rftoolsbuilder.constructor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public final class ConstructorBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 5_000_000;
    public static final int MAX_RECEIVE = 250_000;
    public static final int BASE_PLACEMENT_COST = 1_000;
    public static final int DISTANCE_COST = 15;
    public static final int BLOCK_ENTITY_SURCHARGE = 1_500;
    public static final int AIM_TICKS = 4;
    public static final int CHARGE_TICKS = 5;
    public static final int FLIGHT_TICKS = 8;

    private final ConstructorEnergyStorage energy = new ConstructorEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE);
    private ConstructorStatus status = ConstructorStatus.IDLE;
    private BlockPos targetPos;
    private BlockState targetState;
    private int phaseTick;
    private int shotProgress;
    private boolean running;

    public ConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ConstructorBootstrap.CONSTRUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public ConstructorEnergyStorage energyStorage() { return energy; }
    public ConstructorStatus status() { return status; }
    public BlockPos targetPos() { return targetPos; }
    public BlockState targetState() { return targetState; }
    public int shotProgress() { return shotProgress; }

    public Component statusMessage() {
        return Component.literal("Constructor: " + status.name() + " | " + energy.getEnergyStored() + "/" + energy.getMaxEnergyStored() + " FE");
    }

    public boolean queuePlacement(BlockPos target, BlockState state) {
        if (target == null || state == null || target.equals(worldPosition)) return false;
        if (this.targetPos != null && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) return false;
        this.targetPos = target.immutable();
        this.targetState = state;
        this.phaseTick = 0;
        this.shotProgress = 0;
        this.running = true;
        this.status = ConstructorStatus.READY;
        setChangedAndSync();
        return true;
    }

    public void pause() {
        running = false;
        status = ConstructorStatus.PAUSED;
        setChangedAndSync();
    }

    public void resume() {
        if (targetPos != null) {
            running = true;
            status = ConstructorStatus.READY;
            phaseTick = 0;
            setChangedAndSync();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, ConstructorBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return;
        be.serverTick(server);
    }

    private void serverTick(ServerLevel level) {
        if (!running || targetPos == null || targetState == null) return;

        if (!level.hasChunkAt(targetPos)) {
            transition(ConstructorStatus.WAITING_CHUNK);
            return;
        }

        int cost = energyCost(targetPos, targetState);
        if (energy.getEnergyStored() < cost) {
            transition(ConstructorStatus.WAITING_ENERGY);
            return;
        }

        switch (status) {
            case READY, WAITING_ENERGY, WAITING_CHUNK -> {
                phaseTick = 0;
                transition(ConstructorStatus.AIMING);
            }
            case AIMING -> {
                if (++phaseTick >= AIM_TICKS) {
                    phaseTick = 0;
                    transition(ConstructorStatus.CHARGING);
                }
            }
            case CHARGING -> {
                if (++phaseTick >= CHARGE_TICKS) {
                    energy.consume(cost);
                    phaseTick = 0;
                    shotProgress = 0;
                    transition(ConstructorStatus.FIRING);
                }
            }
            case FIRING -> {
                shotProgress++;
                if (++phaseTick >= FLIGHT_TICKS) {
                    completeShot();
                } else {
                    syncClientState();
                }
            }
            default -> { }
        }
    }

    private void completeShot() {
        // Foundation milestone: FE transaction + aim/charge/fire timing are operational.
        // The normalized schematic/material transaction will perform authoritative placement here.
        status = ConstructorStatus.COMPLETE;
        running = false;
        phaseTick = 0;
        shotProgress = FLIGHT_TICKS;
        setChangedAndSync();
    }

    private int energyCost(BlockPos target, BlockState state) {
        double distance = Math.sqrt(worldPosition.distSqr(target));
        int cost = BASE_PLACEMENT_COST + (int) Math.ceil(distance * DISTANCE_COST);
        if (state.hasBlockEntity()) cost += BLOCK_ENTITY_SURCHARGE;
        return cost;
    }

    private void transition(ConstructorStatus next) {
        if (status != next) {
            status = next;
            phaseTick = 0;
            setChangedAndSync();
        }
    }

    private void setChangedAndSync() {
        setChanged();
        syncClientState();
    }

    private void syncClientState() {
        if (level instanceof ServerLevel server) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy.getEnergyStored());
        output.putInt("Status", status.ordinal());
        output.putBoolean("Running", running);
        output.putInt("PhaseTick", phaseTick);
        output.putInt("ShotProgress", shotProgress);
        if (targetPos != null) output.putLong("TargetPos", targetPos.asLong());
        if (targetState != null) output.store("TargetState", BlockState.CODEC, targetState);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setStored(input.getIntOr("Energy", 0));
        int s = input.getIntOr("Status", ConstructorStatus.IDLE.ordinal());
        status = ConstructorStatus.values()[Math.max(0, Math.min(ConstructorStatus.values().length - 1, s))];
        running = input.getBooleanOr("Running", false);
        phaseTick = Math.max(0, input.getIntOr("PhaseTick", 0));
        shotProgress = Math.max(0, input.getIntOr("ShotProgress", 0));
        long packed = input.getLongOr("TargetPos", Long.MIN_VALUE);
        targetPos = packed == Long.MIN_VALUE ? null : BlockPos.of(packed);
        targetState = input.read("TargetState", BlockState.CODEC).orElse(null);
    }

    public static final class ConstructorEnergyStorage extends SimpleEnergyHandler {
        private ConstructorEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0, 0);
        }

        public int getEnergyStored() { return (int) getAmountAsLong(); }
        public int getMaxEnergyStored() { return (int) getCapacityAsLong(); }

        private void consume(int amount) {
            this.energy = Math.max(0, this.energy - Math.max(0, amount));
        }

        private void setStored(int amount) {
            this.energy = Math.max(0, Math.min(this.capacity, amount));
        }
    }
}
