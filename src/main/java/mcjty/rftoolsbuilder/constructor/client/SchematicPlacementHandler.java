package mcjty.rftoolsbuilder.constructor.client;

import mcjty.rftoolsbuilder.constructor.ConstructorNetworking;
import mcjty.rftoolsbuilder.constructor.ConstructorStateFilterRegistry;
import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicTransform;
import mcjty.rftoolsbuilder.constructor.UniversalSchematicLoader;
import mcjty.rftoolsbuilder.constructor.plan.BlockSubstitutionRules;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mouse/UI-driven schematic deployment controller.
 *
 * There are deliberately no key mappings here. Right-clicking a written
 * Schematic Card opens {@link SchematicPlacementScreen}; after that the anchor,
 * rotation and mirror are changed only by explicit UI actions. The player's
 * crosshair is sampled only when the editor is opened or when the user presses
 * the target button.
 */
public final class SchematicPlacementHandler {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Map<BlockState, BlockModelRenderState> MODEL_CACHE = new HashMap<>();

    private static boolean installed;
    private static String loadedKey = "";
    private static int loadGeneration;
    private static ConstructionPlan plan;
    private static BlockSubstitutionRules substitutions = new BlockSubstitutionRules();
    private static int lastCardComponentsHash;

    private static boolean editing;
    private static BlockPos editAnchor = BlockPos.ZERO;
    private static int editRotation;
    private static int editMirror;
    private static BlockPos resetAnchor = BlockPos.ZERO;
    private static int resetRotation;
    private static int resetMirror;

    private SchematicPlacementHandler() {}

    public static void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onMouseButton);
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onSubmitGeometry);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientSchematicUploader.tick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearTransient();
            return;
        }

        ItemStack card = currentCard(mc);
        if (!isWrittenCard(card)) {
            if (editing) editing = false;
            clearTransient();
            return;
        }

        ensurePlan(card);
    }

    private static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 1 || event.getAction() != 1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        ItemStack card = currentCard(mc);
        if (!isWrittenCard(card)) return;

        event.setCanceled(true);
        ensurePlan(card);
        beginEditing(card, mc);
        mc.setScreen(new SchematicPlacementScreen());
    }

    private static void beginEditing(ItemStack card, Minecraft mc) {
        editRotation = SchematicCardItem.rotation(card);
        editMirror = SchematicCardItem.mirror(card);

        if (SchematicCardItem.deployed(card)) {
            editAnchor = SchematicCardItem.anchor(card);
        } else {
            BlockPos target = lookAnchor(mc);
            editAnchor = target != null ? target : centeredInFront(mc, 4);
        }

        resetAnchor = editAnchor;
        resetRotation = editRotation;
        resetMirror = editMirror;
        editing = true;
        MODEL_CACHE.clear();
    }

    public static boolean isEditing() { return editing; }

    public static boolean hasValidCard() {
        return isWrittenCard(currentCard(Minecraft.getInstance()));
    }

    public static boolean hasSavedDeployment() {
        ItemStack card = currentCard(Minecraft.getInstance());
        return isWrittenCard(card) && SchematicCardItem.deployed(card);
    }

    public static BlockPos anchor() { return editAnchor; }
    public static int rotationQuarter() { return editRotation; }
    public static int rotationDegrees() { return Math.floorMod(editRotation, 4) * 90; }
    public static int mirror() { return editMirror; }
    public static String mirrorName() { return mirrorName(editMirror); }

    public static int sizeX() {
        if (plan != null) return plan.sizeX();
        ItemStack card = currentCard(Minecraft.getInstance());
        return isWrittenCard(card) ? SchematicCardItem.sizeX(card) : 0;
    }

    public static int sizeY() {
        if (plan != null) return plan.sizeY();
        ItemStack card = currentCard(Minecraft.getInstance());
        return isWrittenCard(card) ? SchematicCardItem.sizeY(card) : 0;
    }

    public static int sizeZ() {
        if (plan != null) return plan.sizeZ();
        ItemStack card = currentCard(Minecraft.getInstance());
        return isWrittenCard(card) ? SchematicCardItem.sizeZ(card) : 0;
    }

    public static boolean previewReady() {
        return plan != null && plan.totalTargets() > 0;
    }

    public static int previewBlockCount() {
        return plan == null ? 0 : plan.blockCount();
    }

    public static void nudge(int dx, int dy, int dz) {
        if (!editing) return;
        editAnchor = editAnchor.offset(dx, dy, dz);
    }

    public static void setRotation(int quarterTurns) {
        if (!editing) return;
        editRotation = Math.floorMod(quarterTurns, 4);
        MODEL_CACHE.clear();
    }

    public static void setMirror(int mirror) {
        if (!editing) return;
        editMirror = Math.max(0, Math.min(2, mirror));
        MODEL_CACHE.clear();
    }

    public static void placeAtLook() {
        if (!editing) return;
        Minecraft mc = Minecraft.getInstance();
        BlockPos target = lookAnchor(mc);
        if (target != null) editAnchor = target;
    }

    public static void placeInFront() {
        if (!editing) return;
        editAnchor = centeredInFront(Minecraft.getInstance(), 4);
    }

    public static void centerOnPlayer() {
        if (!editing) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int sx = transformedSizeX();
        int sz = transformedSizeZ();
        BlockPos base = mc.player.blockPosition();
        editAnchor = base.offset(-sx / 2, 0, -sz / 2);
    }

    public static void reset() {
        if (!editing) return;
        editAnchor = resetAnchor;
        editRotation = resetRotation;
        editMirror = resetMirror;
        MODEL_CACHE.clear();
    }

    public static boolean confirm() {
        if (!editing) return false;
        if (!previewReady()) {
            message("Schematic preview is still loading");
            return false;
        }

        ItemStack card = currentCard(Minecraft.getInstance());
        if (!isWrittenCard(card)) return false;

        syncDeployment(card, true);
        editing = false;
        MODEL_CACHE.clear();
        return true;
    }

    public static boolean clearDeployment() {
        ItemStack card = currentCard(Minecraft.getInstance());
        if (!isWrittenCard(card)) return false;

        syncDeployment(card, false);
        editing = false;
        MODEL_CACHE.clear();
        return true;
    }

    public static void cancel() {
        editing = false;
        MODEL_CACHE.clear();
    }

    private static int transformedSizeX() {
        return (Math.floorMod(editRotation, 4) & 1) == 0 ? Math.max(1, sizeX()) : Math.max(1, sizeZ());
    }

    private static int transformedSizeZ() {
        return (Math.floorMod(editRotation, 4) & 1) == 0 ? Math.max(1, sizeZ()) : Math.max(1, sizeX());
    }

    private static BlockPos centeredInFront(Minecraft mc, int distance) {
        if (mc.player == null) return BlockPos.ZERO;
        int sx = transformedSizeX();
        int sz = transformedSizeZ();
        BlockPos center = mc.player.blockPosition().relative(mc.player.getDirection(), distance);
        return center.offset(-sx / 2, 0, -sz / 2);
    }

    private static BlockPos lookAnchor(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit)) return null;
        return hit.getBlockPos().relative(hit.getDirection());
    }

    private static void syncDeployment(ItemStack card, boolean deployed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        BlockPos anchor = deployed ? editAnchor : BlockPos.ZERO;
        int rotation = deployed ? editRotation : 0;
        int mirror = deployed ? editMirror : 0;

        SchematicCardItem.setDeployment(card, anchor, rotation, mirror, deployed);
        ClientPacketDistributor.sendToServer(new ConstructorNetworking.SyncDeployment(
                mc.player.getInventory().getSelectedSlot(), anchor, rotation, mirror, deployed, SchematicCardItem.sha256(card)));
    }

    private static void ensurePlan(ItemStack card) {
        int componentHash = card.getComponentsPatch().hashCode();
        String key = SchematicCardItem.clientFile(card) + "|" + SchematicCardItem.sourceType(card) + "|" + SchematicCardItem.sha256(card);
        if (key.equals(loadedKey) && componentHash == lastCardComponentsHash) return;

        loadedKey = key;
        lastCardComponentsHash = componentHash;
        plan = null;
        MODEL_CACHE.clear();
        substitutions = new BlockSubstitutionRules();
        SchematicCardItem.applyReplacements(card, substitutions);
        int generation = ++loadGeneration;

        SchematicFolderIndex.Format format = SchematicFolderIndex.Format.fromId(SchematicCardItem.sourceType(card));
        if (format == null) format = SchematicFolderIndex.Format.fromFileName(SchematicCardItem.clientFile(card));
        if (format == null) {
            message("Unsupported schematic format on card");
            return;
        }

        SchematicFolderIndex.Entry entry = new SchematicFolderIndex.Entry(SchematicCardItem.clientFile(card), format);
        CompletableFuture.supplyAsync(() -> {
            try {
                return UniversalSchematicLoader.load(entry, false);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((loaded, error) -> Minecraft.getInstance().execute(() -> {
            if (generation != loadGeneration) return;
            if (error != null || loaded == null || loaded.totalTargets() == 0) {
                plan = null;
                message("Could not preview schematic: " + safeMessage(error));
                return;
            }
            plan = loaded;
        }));
    }

    private static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || plan == null) return;

        ItemStack card = currentCard(mc);
        if (!isWrittenCard(card)) return;

        SchematicTransform transform = editing
                ? new SchematicTransform(editAnchor, editRotation, editMirror, plan.sizeX(), plan.sizeY(), plan.sizeZ())
                : SchematicCardItem.transform(card);

        if (!editing && !SchematicCardItem.deployed(card)) return;

        var pose = event.getPoseStack();
        var collector = event.getSubmitNodeCollector();
        var camera = event.getLevelRenderState().cameraRenderState.pos;

        for (var entry : plan.entries()) {
            BlockState source = substitutions.apply(entry.sourceState());
            BlockState state = ConstructorStateFilterRegistry.sanitize(transform.transformState(source));
            if (state.isAir() || state.getBlock() == Blocks.STRUCTURE_VOID) continue;

            BlockPos worldPos = transform.transformWorld(entry.relativePos());
            BlockModelRenderState renderState = MODEL_CACHE.computeIfAbsent(state, s -> {
                BlockModelRenderState created = new BlockModelRenderState();
                mc.getBlockModelResolver().update(created, s, DISPLAY_CONTEXT);
                return created;
            });

            pose.pushPose();
            pose.translate(worldPos.getX() - camera.x, worldPos.getY() - camera.y, worldPos.getZ() - camera.z);
            pose.translate(.01, .01, .01);
            pose.scale(.98f, .98f, .98f);
            renderState.submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static ItemStack currentCard(Minecraft mc) {
        if (mc == null || mc.player == null) return ItemStack.EMPTY;
        return mc.player.getMainHandItem();
    }

    private static boolean isWrittenCard(ItemStack stack) {
        return stack != null && stack.getItem() instanceof SchematicCardItem && SchematicCardItem.hasSource(stack);
    }

    private static void clearTransient() {
        if (!loadedKey.isEmpty() || plan != null) {
            loadedKey = "";
            plan = null;
            MODEL_CACHE.clear();
            editing = false;
            ++loadGeneration;
        }
    }

    public static String mirrorName(int mirror) {
        return switch (mirror) {
            case 1 -> "Left/Right";
            case 2 -> "Front/Back";
            default -> "None";
        };
    }

    private static void message(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.getChat().addClientSystemMessage(Component.literal(message));
    }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown error";
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
