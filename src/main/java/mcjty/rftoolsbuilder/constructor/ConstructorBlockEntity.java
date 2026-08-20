package mcjty.rftoolsbuilder.constructor;

import mcjty.rftoolsbuilder.constructor.plan.BlockSubstitutionRules;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionJob;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.io.IOException;

public final class ConstructorBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int ENERGY_CAPACITY = 5_000_000;
    public static final int MAX_RECEIVE = 250_000;
    public static final int BASE_PLACEMENT_COST = 1_000;
    public static final int DISTANCE_COST = 15;
    public static final int BLOCK_ENTITY_SURCHARGE = 1_500;
    public static final int AIM_TICKS = 4;
    public static final int CHARGE_TICKS = 5;
    public static final int MIN_FLIGHT_TICKS = 10;
    public static final int SLOT_SCHEMATIC = 0;

    private final ConstructorEnergyStorage energy = new ConstructorEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    private ConstructorStatus status = ConstructorStatus.IDLE;
    private ConstructorReplaceMode replaceMode = ConstructorReplaceMode.REPLACE_ANY;
    private boolean skipMissing;
    private boolean replaceBlockEntities;

    private BlockPos targetPos;
    private BlockState targetState;
    private int phaseTick;
    private int shotProgress;
    private int flightTicks = MIN_FLIGHT_TICKS;
    private boolean running;
    private boolean shotReserved;
    private ConstructionJob activeJob;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> status.ordinal();
                case 3 -> jobIndex();
                case 4 -> jobTotal();
                case 5 -> shotProgress;
                case 6 -> running ? 1 : 0;
                case 7 -> currentEnergyCost();
                case 8 -> SchematicCardItem.hasSource(schematicCard()) ? 1 : 0;
                case 9 -> replaceMode.ordinal();
                case 10 -> skipMissing ? 1 : 0;
                case 11 -> replaceBlockEntities ? 1 : 0;
                case 12 -> flightTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 13;
        }
    };

    public ConstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ConstructorBootstrap.CONSTRUCTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public ConstructorEnergyStorage energyStorage() { return energy; }
    public ConstructorStatus status() { return status; }
    public ConstructorReplaceMode replaceMode() { return replaceMode; }
    public boolean skipMissing() { return skipMissing; }
    public boolean replaceBlockEntities() { return replaceBlockEntities; }
    public BlockPos targetPos() { return targetPos; }
    public BlockState targetState() { return targetState; }
    public int shotProgress() { return shotProgress; }
    public int flightTicks() { return Math.max(MIN_FLIGHT_TICKS, flightTicks); }
    public boolean shotReserved() { return shotReserved; }
    public boolean isRunning() { return running; }
    public ContainerData menuData() { return menuData; }
    public ItemStack schematicCard() { return items.get(SLOT_SCHEMATIC); }
    public int jobIndex() { return activeJob == null ? 0 : activeJob.index(); }
    public int jobTotal() { return activeJob == null ? (targetPos == null ? 0 : 1) : activeJob.total(); }
    public float jobProgress() { return activeJob == null ? (status == ConstructorStatus.COMPLETE ? 1.0f : 0.0f) : activeJob.progress(); }

    public int currentEnergyCost() {
        return targetPos == null || targetState == null ? 0 : energyCost(targetPos, targetState);
    }

    public boolean queuePlacement(BlockPos target, BlockState state) {
        if (target == null || state == null || target.equals(worldPosition)) return false;
        if (this.targetPos != null && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) return false;
        activeJob = null;
        prepareTarget(target, state);
        return true;
    }

    public boolean startPlan(ConstructionPlan plan, BlockPos origin, BlockSubstitutionRules substitutions) {
        if (plan == null || origin == null || substitutions == null) return false;
        if (running && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) return false;
        activeJob = new ConstructionJob(plan, origin, substitutions);
        if (!activeJob.hasCurrent()) {
            finishJob(0);
            return true;
        }
        loadCurrentFromJob();
        return true;
    }

    public boolean startCardPlan() {
        if (!(level instanceof ServerLevel)) return false;
        ItemStack card = schematicCard();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card)) return false;
        if (running && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) return false;

        try {
            boolean includeAir = replaceMode == ConstructorReplaceMode.REPLACE_EMPTY;
            ConstructionPlan plan = SchematicPlanLoader.loadCard(card, includeAir);
            if (plan.size() <= 0) {
                status = ConstructorStatus.ERROR;
                setChangedAndSync();
                return false;
            }
            BlockSubstitutionRules rules = new BlockSubstitutionRules();
            SchematicCardItem.applyReplacements(card, rules);
            BlockPos origin = worldPosition
                    .relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING), 4)
                    .offset(SchematicCardItem.offsetX(card), SchematicCardItem.offsetY(card), SchematicCardItem.offsetZ(card));
            return startPlan(plan, origin, rules);
        } catch (IOException | RuntimeException ignored) {
            running = false;
            status = ConstructorStatus.ERROR;
            setChangedAndSync();
            return false;
        }
    }

    private void prepareTarget(BlockPos target, BlockState state) {
        targetPos = target.immutable();
        targetState = state;
        phaseTick = 0;
        shotProgress = 0;
        flightTicks = ticksForDistance(target);
        shotReserved = false;
        running = true;
        status = ConstructorStatus.READY;
        setChangedAndSync();
    }

    private void loadCurrentFromJob() {
        if (activeJob == null || !activeJob.hasCurrent()) {
            finishJob(0);
            return;
        }
        targetPos = activeJob.currentWorldPos();
        targetState = activeJob.currentTargetState();
        phaseTick = 0;
        shotProgress = 0;
        flightTicks = ticksForDistance(targetPos);
        shotReserved = false;
        running = true;
        status = ConstructorStatus.READY;
        setChangedAndSync();
    }

    public void pause() {
        if (targetPos == null || status == ConstructorStatus.COMPLETE || status == ConstructorStatus.ERROR) return;
        running = false;
        status = ConstructorStatus.PAUSED;
        setChangedAndSync();
    }

    public void resume() {
        if (targetPos != null && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) {
            running = true;
            status = ConstructorStatus.READY;
            phaseTick = 0;
            setChangedAndSync();
        }
    }

    public boolean clearJob() {
        if (shotReserved || status == ConstructorStatus.FIRING) return false;
        activeJob = null;
        targetPos = null;
        targetState = null;
        phaseTick = 0;
        shotProgress = 0;
        flightTicks = MIN_FLIGHT_TICKS;
        running = false;
        status = ConstructorStatus.IDLE;
        setChangedAndSync();
        return true;
    }

    public boolean handleMenuButton(int id) {
        return switch (id) {
            case 0 -> {
                if (running) pause(); else resume();
                yield true;
            }
            case 1 -> clearJob();
            case 2 -> startCardPlan();
            case 3 -> {
                replaceMode = replaceMode.next();
                setChangedAndSync();
                yield true;
            }
            case 4 -> {
                skipMissing = !skipMissing;
                setChangedAndSync();
                yield true;
            }
            case 5 -> {
                replaceBlockEntities = !replaceBlockEntities;
                setChangedAndSync();
                yield true;
            }
            default -> false;
        };
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

        if (!shotReserved) {
            if (!prepareShotIfPossible(level)) return;
        }

        switch (status) {
            case READY, WAITING_ENERGY, WAITING_MATERIAL, WAITING_CHUNK -> {
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
                    if (!shotReserved && !reserveShot(level)) return;
                    phaseTick = 0;
                    shotProgress = 0;
                    transition(ConstructorStatus.FIRING);
                }
            }
            case FIRING -> {
                shotProgress++;
                if (++phaseTick >= flightTicks()) completeShot(level); else syncClientState();
            }
            default -> { }
        }
    }

    private boolean prepareShotIfPossible(ServerLevel level) {
        if (shouldIgnoreSchematicState(targetState)) {
            skipCurrentAndAdvance();
            return false;
        }

        BlockState current = level.getBlockState(targetPos);
        if (current == targetState || current.equals(targetState)) {
            skipCurrentAndAdvance();
            return false;
        }

        if (!replaceBlockEntities && current.hasBlockEntity()) {
            skipCurrentAndAdvance();
            return false;
        }

        if (!current.isAir() && current.getDestroySpeed(level, targetPos) < 0) {
            skipCurrentAndAdvance();
            return false;
        }

        if (!shouldReplace(level, current, targetState)) {
            skipCurrentAndAdvance();
            return false;
        }

        if (!targetState.isAir() && !targetState.canSurvive(level, targetPos)) {
            deferForSupport();
            return false;
        }

        if (!ConstructorMaterialAccess.isRepresentable(targetState)) {
            skipCurrentAndAdvance();
            return false;
        }

        int cost = energyCost(targetPos, targetState);
        if (energy.getEnergyStored() < cost) {
            transition(ConstructorStatus.WAITING_ENERGY);
            return false;
        }

        if (!ConstructorMaterialAccess.hasOne(level, worldPosition, targetState)) {
            if (skipMissing) {
                skipCurrentAndAdvance();
            } else {
                transition(ConstructorStatus.WAITING_MATERIAL);
            }
            return false;
        }

        return true;
    }

    private boolean shouldReplace(ServerLevel level, BlockState current, BlockState desired) {
        if (desired.isAir()) return replaceMode == ConstructorReplaceMode.REPLACE_EMPTY && !current.isAir();
        if (current.isAir()) return true;
        return switch (replaceMode) {
            case DONT_REPLACE -> false;
            case REPLACE_SOLID -> desired.isRedstoneConductor(level, targetPos);
            case REPLACE_ANY, REPLACE_EMPTY -> true;
        };
    }

    private boolean shouldIgnoreSchematicState(BlockState state) {
        if (state == null || state.getBlock() == Blocks.STRUCTURE_VOID) return true;
        if (state.getBlock() instanceof PistonHeadBlock) return true;
        // Fluids and other non-item virtual states cannot be consumed safely by a
        // generic cross-mod constructor. Air remains meaningful in clear mode.
        return !state.isAir() && state.getBlock().asItem() == net.minecraft.world.item.Items.AIR
                && ConstructorMaterialAccess.requiresMaterial(state);
    }

    private void deferForSupport() {
        if (activeJob != null && activeJob.deferCurrent()) {
            loadCurrentFromJob();
            return;
        }
        running = false;
        status = ConstructorStatus.BLOCKED;
        phaseTick = 0;
        setChangedAndSync();
    }

    private boolean reserveShot(ServerLevel level) {
        int cost = energyCost(targetPos, targetState);
        if (energy.getEnergyStored() < cost) {
            transition(ConstructorStatus.WAITING_ENERGY);
            return false;
        }
        if (!ConstructorMaterialAccess.extractOne(level, worldPosition, targetState)) {
            if (skipMissing) skipCurrentAndAdvance(); else transition(ConstructorStatus.WAITING_MATERIAL);
            return false;
        }
        energy.consume(cost);
        shotReserved = true;
        setChangedAndSync();
        return true;
    }

    private void completeShot(ServerLevel level) {
        BlockState current = level.getBlockState(targetPos);
        if (!replaceBlockEntities && current.hasBlockEntity()) {
            blockShotAtImpact();
            return;
        }
        if (!current.isAir() && current.getDestroySpeed(level, targetPos) < 0) {
            blockShotAtImpact();
            return;
        }
        if (!shouldReplace(level, current, targetState) && !(current == targetState || current.equals(targetState))) {
            blockShotAtImpact();
            return;
        }
        if (!targetState.isAir() && !targetState.canSurvive(level, targetPos)) {
            blockShotAtImpact();
            return;
        }

        if (!level.setBlock(targetPos, targetState, Block.UPDATE_ALL)) {
            running = false;
            status = ConstructorStatus.ERROR;
            phaseTick = 0;
            setChangedAndSync();
            return;
        }

        restoreBlockEntityData(level);
        shotReserved = false;
        finishCurrentAndAdvance(flightTicks());
    }

    private void restoreBlockEntityData(ServerLevel level) {
        if (activeJob == null || targetState == null || !targetState.hasBlockEntity()) return;
        CompoundTag data = activeJob.currentBlockEntityData();
        if (data == null || data.isEmpty()) return;
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (blockEntity == null) return;
        try {
            data.putInt("x", targetPos.getX());
            data.putInt("y", targetPos.getY());
            data.putInt("z", targetPos.getZ());
            blockEntity.loadWithComponents(data, level.registryAccess());
            blockEntity.setChanged();
        } catch (RuntimeException ignored) {
            // The block itself is still valid; incompatible BE payloads must not
            // abort the entire schematic.
        }
    }

    private void blockShotAtImpact() {
        running = false;
        status = ConstructorStatus.BLOCKED;
        phaseTick = 0;
        shotProgress = flightTicks();
        setChangedAndSync();
    }

    private void skipCurrentAndAdvance() {
        shotReserved = false;
        if (activeJob != null && activeJob.skipCurrent()) {
            loadCurrentFromJob();
        } else {
            finishJob(0);
        }
    }

    private void finishCurrentAndAdvance(int finishedShotProgress) {
        shotReserved = false;
        if (activeJob != null && activeJob.advance()) {
            loadCurrentFromJob();
            return;
        }
        finishJob(finishedShotProgress);
    }

    private void finishJob(int finishedShotProgress) {
        status = ConstructorStatus.COMPLETE;
        running = false;
        phaseTick = 0;
        shotProgress = finishedShotProgress;
        targetPos = null;
        targetState = null;
        setChangedAndSync();
    }

    private int energyCost(BlockPos target, BlockState state) {
        double distance = Math.sqrt(worldPosition.distSqr(target));
        int cost = BASE_PLACEMENT_COST + (int) Math.ceil(distance * DISTANCE_COST);
        if (state.hasBlockEntity()) cost += BLOCK_ENTITY_SURCHARGE;
        return cost;
    }

    private int ticksForDistance(BlockPos target) {
        double distSqr = target.distSqr(worldPosition);
        return Math.max(MIN_FLIGHT_TICKS, (int) (Math.sqrt(Math.sqrt(distSqr)) * 4.0));
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
    public Component getDisplayName() {
        return Component.literal("Constructor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ConstructorMenu(id, inventory, this, menuData);
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
        output.putInt("ReplaceMode", replaceMode.ordinal());
        output.putBoolean("SkipMissing", skipMissing);
        output.putBoolean("ReplaceBlockEntities", replaceBlockEntities);
        output.putBoolean("Running", running);
        output.putBoolean("ShotReserved", shotReserved);
        output.putInt("PhaseTick", phaseTick);
        output.putInt("ShotProgress", shotProgress);
        output.putInt("FlightTicks", flightTicks);
        if (targetPos != null) output.putLong("TargetPos", targetPos.asLong());
        if (targetState != null) output.store("TargetState", BlockState.CODEC, targetState);
        if (!schematicCard().isEmpty()) output.store("SchematicCard", ItemStack.CODEC, schematicCard());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setStored(input.getIntOr("Energy", 0));
        int s = input.getIntOr("Status", ConstructorStatus.IDLE.ordinal());
        status = ConstructorStatus.values()[Math.max(0, Math.min(ConstructorStatus.values().length - 1, s))];
        int mode = input.getIntOr("ReplaceMode", ConstructorReplaceMode.REPLACE_ANY.ordinal());
        replaceMode = ConstructorReplaceMode.values()[Math.max(0, Math.min(ConstructorReplaceMode.values().length - 1, mode))];
        skipMissing = input.getBooleanOr("SkipMissing", false);
        replaceBlockEntities = input.getBooleanOr("ReplaceBlockEntities", false);
        running = input.getBooleanOr("Running", false);
        shotReserved = input.getBooleanOr("ShotReserved", false);
        phaseTick = Math.max(0, input.getIntOr("PhaseTick", 0));
        shotProgress = Math.max(0, input.getIntOr("ShotProgress", 0));
        flightTicks = Math.max(MIN_FLIGHT_TICKS, input.getIntOr("FlightTicks", MIN_FLIGHT_TICKS));
        long packed = input.getLongOr("TargetPos", Long.MIN_VALUE);
        targetPos = packed == Long.MIN_VALUE ? null : BlockPos.of(packed);
        targetState = input.read("TargetState", BlockState.CODEC).orElse(null);
        items.set(SLOT_SCHEMATIC, input.read("SchematicCard", ItemStack.CODEC).orElse(ItemStack.EMPTY));

        // The runtime queue is reconstructed from the card. Never continue a
        // partially persisted single target as if it were the whole schematic.
        if (running) {
            running = false;
            shotReserved = false;
            status = ConstructorStatus.PAUSED;
        }
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return schematicCard().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == SLOT_SCHEMATIC ? schematicCard() : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT_SCHEMATIC) return ItemStack.EMPTY;
        ItemStack stack = schematicCard();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
        setChangedAndSync();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != SLOT_SCHEMATIC) return ItemStack.EMPTY;
        ItemStack result = schematicCard();
        items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT_SCHEMATIC) return;
        items.set(SLOT_SCHEMATIC, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        setChangedAndSync();
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
        setChangedAndSync();
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
