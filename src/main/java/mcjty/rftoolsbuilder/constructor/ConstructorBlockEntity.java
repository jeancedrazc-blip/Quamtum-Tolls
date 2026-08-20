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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.io.IOException;

/** Server-authoritative FE schematic printer. */
public final class ConstructorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider, Container {
    public static final int ENERGY_CAPACITY = 5_000_000;
    public static final int MAX_RECEIVE = 250_000;
    public static final int BASE_PLACEMENT_COST = 1_000;
    public static final int DISTANCE_COST = 15;
    public static final int BLOCK_ENTITY_SURCHARGE = 1_500;
    public static final int AIM_TICKS = 4;
    public static final int CHARGE_TICKS = 5;
    public static final int MIN_FLIGHT_TICKS = 10;
    public static final int SHOT_COOLDOWN_TICKS = 4;
    public static final int MAX_TARGET_DISTANCE = 256;
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
    private int shotCooldown;
    private boolean running;
    private boolean shotReserved;
    private boolean pauseAfterShot;
    private ItemStack reservedPlacementStack = ItemStack.EMPTY;
    private ItemStack missingItem = ItemStack.EMPTY;
    private ConstructionJob activeJob;
    private CompoundTag pendingJobData;

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> status.ordinal();
                case 3 -> jobIndex();
                case 4 -> jobTotal();
                case 5 -> shotProgress;
                case 6 -> running ? 1 : 0;
                case 7 -> currentEnergyCost();
                case 8 -> SchematicCardItem.hasSource(schematicCard()) && SchematicCardItem.deployed(schematicCard()) ? 1 : 0;
                case 9 -> replaceMode.ordinal();
                case 10 -> skipMissing ? 1 : 0;
                case 11 -> replaceBlockEntities ? 1 : 0;
                case 12 -> flightTicks;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 13; }
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
    public ItemStack missingItem() { return missingItem; }
    public ContainerData menuData() { return menuData; }
    public ItemStack schematicCard() { return items.get(SLOT_SCHEMATIC); }
    public int jobIndex() { return activeJob == null ? 0 : activeJob.completed(); }
    public int jobTotal() { return activeJob == null ? (targetPos == null ? 0 : 1) : activeJob.total(); }
    public float jobProgress() { return activeJob == null ? (status == ConstructorStatus.COMPLETE ? 1f : 0f) : activeJob.progress(); }

    public boolean canRemoveCard() {
        return !shotReserved && !running && (activeJob == null || status == ConstructorStatus.IDLE
                || status == ConstructorStatus.COMPLETE || status == ConstructorStatus.ERROR || status == ConstructorStatus.BLOCKED);
    }

    public int currentEnergyCost() {
        return targetPos == null || targetState == null ? 0 : energyCost(targetPos, targetState);
    }

    public boolean queuePlacement(BlockPos target, BlockState state) {
        if (target == null || state == null || target.equals(worldPosition)) return false;
        if (running || shotReserved) return false;
        activeJob = null;
        pendingJobData = null;
        prepareTarget(target, ConstructorPlacementHelper.sanitizeState(state));
        return true;
    }

    public boolean startPlan(ConstructionPlan plan, SchematicTransform transform, BlockSubstitutionRules substitutions) {
        if (plan == null || transform == null || substitutions == null) return false;
        if (running || shotReserved) return false;
        activeJob = new ConstructionJob(plan, transform, substitutions);
        pendingJobData = null;
        if (!activeJob.hasCurrentBlock()) {
            finishJob(0);
            return true;
        }
        loadCurrentFromJob(false);
        return true;
    }

    /** Compatibility overload for older internal callers. */
    public boolean startPlan(ConstructionPlan plan, BlockPos origin, BlockSubstitutionRules substitutions) {
        if (plan == null || origin == null) return false;
        return startPlan(plan, new SchematicTransform(origin, 0, 0, plan.sizeX(), plan.sizeY(), plan.sizeZ()), substitutions);
    }

    public boolean startCardPlan() {
        if (!(level instanceof ServerLevel)) return false;
        ItemStack card = schematicCard();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card)
                || !SchematicCardItem.hasBounds(card) || !SchematicCardItem.deployed(card)) {
            running = false;
            status = ConstructorStatus.BLOCKED;
            setChangedAndSync();
            return false;
        }
        if (running || shotReserved) return false;

        try {
            boolean includeAir = replaceMode == ConstructorReplaceMode.REPLACE_EMPTY;
            ConstructionPlan plan = SchematicPlanLoader.loadCard(card, includeAir);
            if (plan.size() <= 0 || plan.sizeX() != SchematicCardItem.sizeX(card)
                    || plan.sizeY() != SchematicCardItem.sizeY(card) || plan.sizeZ() != SchematicCardItem.sizeZ(card)) {
                failJob(ConstructorStatus.ERROR);
                return false;
            }
            BlockPos anchor = SchematicCardItem.anchor(card);
            if (worldPosition.distSqr(anchor) > (double) MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
                failJob(ConstructorStatus.BLOCKED);
                return false;
            }
            BlockSubstitutionRules rules = new BlockSubstitutionRules();
            SchematicCardItem.applyReplacements(card, rules);
            SchematicTransform transform = new SchematicTransform(anchor, SchematicCardItem.rotation(card), SchematicCardItem.mirror(card),
                    plan.sizeX(), plan.sizeY(), plan.sizeZ());
            return startPlan(plan, transform, rules);
        } catch (IOException | RuntimeException ignored) {
            failJob(ConstructorStatus.ERROR);
            return false;
        }
    }

    private void prepareTarget(BlockPos target, BlockState state) {
        targetPos = target.immutable();
        targetState = ConstructorPlacementHelper.sanitizeState(state);
        phaseTick = 0;
        shotProgress = 0;
        flightTicks = ticksForDistance(target);
        shotReserved = false;
        reservedPlacementStack = ItemStack.EMPTY;
        missingItem = ItemStack.EMPTY;
        running = true;
        status = ConstructorStatus.READY;
        setChangedAndSync();
    }

    private void loadCurrentFromJob(boolean preserveReservedFlight) {
        if (activeJob == null || !activeJob.hasCurrentBlock()) {
            finishJob(0);
            return;
        }
        BlockPos nextPos = activeJob.currentWorldPos();
        BlockState nextState = ConstructorPlacementHelper.sanitizeState(activeJob.currentTargetState());
        if (preserveReservedFlight && shotReserved) {
            if (targetPos == null || !targetPos.equals(nextPos) || targetState == null || !targetState.equals(nextState)) {
                failJob(ConstructorStatus.ERROR);
                return;
            }
        } else {
            targetPos = nextPos;
            targetState = nextState;
            phaseTick = 0;
            shotProgress = 0;
            flightTicks = ticksForDistance(targetPos);
            shotReserved = false;
            reservedPlacementStack = ItemStack.EMPTY;
            missingItem = ItemStack.EMPTY;
            status = ConstructorStatus.READY;
        }
        running = status != ConstructorStatus.PAUSED && status != ConstructorStatus.BLOCKED && status != ConstructorStatus.ERROR;
        setChangedAndSync();
    }

    public void pause() {
        if (targetPos == null || status == ConstructorStatus.COMPLETE || status == ConstructorStatus.ERROR) return;
        if (shotReserved || status == ConstructorStatus.FIRING) {
            pauseAfterShot = true;
            setChangedAndSync();
            return;
        }
        running = false;
        status = ConstructorStatus.PAUSED;
        setChangedAndSync();
    }

    public void resume() {
        pauseAfterShot = false;
        if (targetPos != null && status != ConstructorStatus.COMPLETE && status != ConstructorStatus.ERROR) {
            running = true;
            if (!shotReserved) status = ConstructorStatus.READY;
            setChangedAndSync();
        }
    }

    public boolean clearJob() {
        if (shotReserved || status == ConstructorStatus.FIRING) return false;
        activeJob = null;
        pendingJobData = null;
        targetPos = null;
        targetState = null;
        phaseTick = 0;
        shotProgress = 0;
        flightTicks = MIN_FLIGHT_TICKS;
        shotCooldown = 0;
        pauseAfterShot = false;
        reservedPlacementStack = ItemStack.EMPTY;
        missingItem = ItemStack.EMPTY;
        running = false;
        status = ConstructorStatus.IDLE;
        setChangedAndSync();
        return true;
    }

    public boolean handleMenuButton(int id) {
        return switch (id) {
            case 0 -> { if (running && !pauseAfterShot) pause(); else resume(); yield true; }
            case 1 -> clearJob();
            case 2 -> startCardPlan();
            case 3 -> { replaceMode = replaceMode.next(); setChangedAndSync(); yield true; }
            case 4 -> { skipMissing = !skipMissing; setChangedAndSync(); yield true; }
            case 5 -> { replaceBlockEntities = !replaceBlockEntities; setChangedAndSync(); yield true; }
            default -> false;
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, ConstructorBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return;
        be.serverTick(server);
    }

    private void serverTick(ServerLevel level) {
        if (activeJob == null && pendingJobData != null) restorePendingJob();
        if (!running || targetPos == null || targetState == null) return;
        if (shotCooldown > 0 && !shotReserved) { shotCooldown--; return; }

        if (!level.hasChunkAt(targetPos)) { transition(ConstructorStatus.WAITING_CHUNK); return; }
        if (!level.getWorldBorder().isWithinBounds(targetPos)
                || worldPosition.distSqr(targetPos) > (double) MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            failJob(ConstructorStatus.BLOCKED);
            return;
        }

        // A projectile already in flight owns its reservation and continues even if pause was requested.
        if (shotReserved && status == ConstructorStatus.FIRING) {
            shotProgress++;
            if (++phaseTick >= flightTicks()) completeShot(level); else syncClientState();
            return;
        }

        if (!prepareShotIfPossible(level)) return;

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
                    if (!reserveShot(level)) return;
                    phaseTick = 0;
                    shotProgress = 0;
                    transition(ConstructorStatus.FIRING);
                }
            }
            default -> { }
        }
    }

    private void restorePendingJob() {
        CompoundTag snapshot = pendingJobData;
        pendingJobData = null;
        ItemStack card = schematicCard();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card) || !SchematicCardItem.deployed(card)) {
            failJob(ConstructorStatus.ERROR);
            return;
        }
        try {
            boolean includeAir = replaceMode == ConstructorReplaceMode.REPLACE_EMPTY;
            ConstructionPlan plan = SchematicPlanLoader.loadCard(card, includeAir);
            BlockSubstitutionRules rules = new BlockSubstitutionRules();
            SchematicCardItem.applyReplacements(card, rules);
            SchematicTransform transform = new SchematicTransform(SchematicCardItem.anchor(card), SchematicCardItem.rotation(card),
                    SchematicCardItem.mirror(card), plan.sizeX(), plan.sizeY(), plan.sizeZ());
            activeJob = ConstructionJob.restore(plan, transform, rules, snapshot);
            if (!activeJob.hasCurrentBlock()) {
                finishJob(shotProgress);
                return;
            }
            loadCurrentFromJob(true);
        } catch (IOException | RuntimeException ignored) {
            failJob(ConstructorStatus.ERROR);
        }
    }

    private boolean prepareShotIfPossible(ServerLevel level) {
        if (ConstructorPlacementHelper.shouldIgnore(targetState)) { skipCurrentAndAdvance(); return false; }

        BlockState current = level.getBlockState(targetPos);
        boolean sameState = current.equals(targetState);
        if (sameState && (!targetState.hasBlockEntity() || !replaceBlockEntities)) {
            skipCurrentAndAdvance();
            return false;
        }
        if (!replaceBlockEntities && current.hasBlockEntity()) { skipCurrentAndAdvance(); return false; }
        if (!current.isAir() && current.getDestroySpeed(level, targetPos) < 0) { skipCurrentAndAdvance(); return false; }
        if (!shouldReplace(level, current, targetState) && !sameState) { skipCurrentAndAdvance(); return false; }

        if (!targetState.isAir() && !targetState.canSurvive(level, targetPos)) {
            deferForSupport();
            return false;
        }

        ConstructorRequirement requirement = currentRequirement();
        if (requirement.isInvalid()) {
            missingItem = ItemStack.EMPTY;
            failJob(ConstructorStatus.BLOCKED);
            return false;
        }

        int cost = energyCost(targetPos, targetState);
        if (energy.getEnergyStored() < cost) { transition(ConstructorStatus.WAITING_ENERGY); return false; }

        ConstructorMaterialAccess.Result material = ConstructorMaterialAccess.simulate(level, worldPosition, requirement);
        if (!material.success()) {
            missingItem = material.missingStack();
            if (skipMissing) skipCurrentAndAdvance(); else transition(ConstructorStatus.WAITING_MATERIAL);
            return false;
        }
        missingItem = ItemStack.EMPTY;
        return true;
    }

    private ConstructorRequirement currentRequirement() {
        CompoundTag data = activeJob == null ? null : activeJob.currentBlockEntityData();
        return ConstructorRequirementRegistry.resolve(targetState, data);
    }

    private boolean shouldReplace(ServerLevel level, BlockState current, BlockState desired) {
        if (desired.isAir()) return replaceMode == ConstructorReplaceMode.REPLACE_EMPTY && !current.isAir();
        if (current.isAir()) return true;
        return switch (replaceMode) {
            case DONT_REPLACE -> false;
            case REPLACE_SOLID -> desired.isRedstoneConductor(level, targetPos)
                    || !current.isRedstoneConductor(level, targetPos);
            case REPLACE_ANY, REPLACE_EMPTY -> true;
        };
    }

    private void deferForSupport() {
        if (activeJob != null && activeJob.deferCurrent()) {
            loadCurrentFromJob(false);
            return;
        }
        running = false;
        status = ConstructorStatus.BLOCKED;
        phaseTick = 0;
        setChangedAndSync();
    }

    private boolean reserveShot(ServerLevel level) {
        int cost = energyCost(targetPos, targetState);
        if (energy.getEnergyStored() < cost) { transition(ConstructorStatus.WAITING_ENERGY); return false; }

        ConstructorMaterialAccess.Result material = ConstructorMaterialAccess.consume(level, worldPosition, currentRequirement());
        if (!material.success()) {
            missingItem = material.missingStack();
            if (skipMissing) skipCurrentAndAdvance(); else transition(ConstructorStatus.WAITING_MATERIAL);
            return false;
        }
        energy.consume(cost);
        reservedPlacementStack = material.placementStack();
        missingItem = ItemStack.EMPTY;
        shotReserved = true;
        setChangedAndSync();
        return true;
    }

    private void completeShot(ServerLevel level) {
        BlockState current = level.getBlockState(targetPos);
        if ((!replaceBlockEntities && current.hasBlockEntity()) || (!current.isAir() && current.getDestroySpeed(level, targetPos) < 0)) {
            blockShotAtImpact();
            return;
        }

        CompoundTag data = activeJob == null ? null : activeJob.currentBlockEntityData();
        boolean placed = ConstructorPlacementHelper.place(level, targetPos, targetState, reservedPlacementStack, data);
        shotReserved = false;
        reservedPlacementStack = ItemStack.EMPTY;
        if (!placed) {
            failJob(ConstructorStatus.ERROR);
            return;
        }
        finishCurrentAndAdvance(flightTicks());
    }

    private void blockShotAtImpact() {
        shotReserved = false;
        reservedPlacementStack = ItemStack.EMPTY;
        running = false;
        status = ConstructorStatus.BLOCKED;
        phaseTick = 0;
        shotProgress = flightTicks();
        setChangedAndSync();
    }

    private void skipCurrentAndAdvance() {
        shotReserved = false;
        reservedPlacementStack = ItemStack.EMPTY;
        if (activeJob != null && activeJob.skipCurrent()) loadCurrentFromJob(false);
        else finishJob(0);
    }

    private void finishCurrentAndAdvance(int finishedShotProgress) {
        shotReserved = false;
        reservedPlacementStack = ItemStack.EMPTY;
        shotCooldown = SHOT_COOLDOWN_TICKS;
        if (activeJob != null && activeJob.advance()) {
            loadCurrentFromJob(false);
            if (pauseAfterShot) {
                pauseAfterShot = false;
                running = false;
                status = ConstructorStatus.PAUSED;
                setChangedAndSync();
            }
            return;
        }
        finishJob(finishedShotProgress);
    }

    private void finishJob(int finishedShotProgress) {
        status = ConstructorStatus.COMPLETE;
        running = false;
        pauseAfterShot = false;
        phaseTick = 0;
        shotProgress = finishedShotProgress;
        targetPos = null;
        targetState = null;
        missingItem = ItemStack.EMPTY;
        setChangedAndSync();
    }

    private void failJob(ConstructorStatus failure) {
        running = false;
        pauseAfterShot = false;
        status = failure;
        phaseTick = 0;
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

    private void setChangedAndSync() { setChanged(); syncClientState(); }
    private void syncClientState() {
        if (level instanceof ServerLevel server) server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public Component getDisplayName() { return Component.literal("Constructor"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ConstructorMenu(id, inventory, this, menuData); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

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
        output.putBoolean("PauseAfterShot", pauseAfterShot);
        output.putInt("PhaseTick", phaseTick);
        output.putInt("ShotProgress", shotProgress);
        output.putInt("FlightTicks", flightTicks);
        output.putInt("ShotCooldown", shotCooldown);
        if (targetPos != null) output.putLong("TargetPos", targetPos.asLong());
        if (targetState != null) output.store("TargetState", BlockState.CODEC, targetState);
        if (!schematicCard().isEmpty()) output.store("SchematicCard", ItemStack.CODEC, schematicCard());
        if (!reservedPlacementStack.isEmpty()) output.store("ReservedPlacementStack", ItemStack.CODEC, reservedPlacementStack);
        if (!missingItem.isEmpty()) output.store("MissingItem", ItemStack.CODEC, missingItem);
        if (activeJob != null) output.store("ConstructionJob", CompoundTag.CODEC, activeJob.save());
        else if (pendingJobData != null) output.store("ConstructionJob", CompoundTag.CODEC, pendingJobData);
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
        pauseAfterShot = input.getBooleanOr("PauseAfterShot", false);
        phaseTick = Math.max(0, input.getIntOr("PhaseTick", 0));
        shotProgress = Math.max(0, input.getIntOr("ShotProgress", 0));
        flightTicks = Math.max(MIN_FLIGHT_TICKS, input.getIntOr("FlightTicks", MIN_FLIGHT_TICKS));
        shotCooldown = Math.max(0, input.getIntOr("ShotCooldown", 0));
        long packed = input.getLongOr("TargetPos", Long.MIN_VALUE);
        targetPos = packed == Long.MIN_VALUE ? null : BlockPos.of(packed);
        targetState = input.read("TargetState", BlockState.CODEC).orElse(null);
        items.set(SLOT_SCHEMATIC, input.read("SchematicCard", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        reservedPlacementStack = input.read("ReservedPlacementStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        missingItem = input.read("MissingItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        pendingJobData = input.read("ConstructionJob", CompoundTag.CODEC).orElse(null);
        activeJob = null;

        // Old development saves could claim an in-flight reservation without a resumable job.
        // Stop safely rather than charging the same shot twice.
        if ((running || shotReserved) && pendingJobData == null) {
            running = false;
            shotReserved = false;
            reservedPlacementStack = ItemStack.EMPTY;
            targetPos = null;
            targetState = null;
            status = ConstructorStatus.ERROR;
            phaseTick = 0;
            shotProgress = 0;
        }
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return schematicCard().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == SLOT_SCHEMATIC ? schematicCard() : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != SLOT_SCHEMATIC || !canRemoveCard()) return ItemStack.EMPTY;
        ItemStack stack = schematicCard();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
            clearJob();
        } else setChangedAndSync();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != SLOT_SCHEMATIC || !canRemoveCard()) return ItemStack.EMPTY;
        ItemStack result = schematicCard();
        items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
        activeJob = null;
        pendingJobData = null;
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != SLOT_SCHEMATIC || (!canRemoveCard() && !ItemStack.matches(schematicCard(), stack))) return;
        items.set(SLOT_SCHEMATIC, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        if (stack.isEmpty()) clearJob(); else setChangedAndSync();
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override
    public void clearContent() {
        if (!canRemoveCard()) return;
        items.set(SLOT_SCHEMATIC, ItemStack.EMPTY);
        clearJob();
    }

    public static final class ConstructorEnergyStorage extends SimpleEnergyHandler {
        private ConstructorEnergyStorage(int capacity, int maxReceive) { super(capacity, maxReceive, 0, 0); }
        public int getEnergyStored() { return (int) getAmountAsLong(); }
        public int getMaxEnergyStored() { return (int) getCapacityAsLong(); }
        private void consume(int amount) { this.energy = Math.max(0, this.energy - Math.max(0, amount)); }
        private void setStored(int amount) { this.energy = Math.max(0, Math.min(this.capacity, amount)); }
    }
}
