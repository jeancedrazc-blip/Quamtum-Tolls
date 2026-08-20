package mcjty.rftoolsbuilder.constructor.client;

import com.mojang.blaze3d.platform.InputConstants;
import mcjty.rftoolsbuilder.constructor.ConstructorNetworking;
import mcjty.rftoolsbuilder.constructor.ConstructorStateFilterRegistry;
import mcjty.rftoolsbuilder.constructor.SchematicCardItem;
import mcjty.rftoolsbuilder.constructor.SchematicFolderIndex;
import mcjty.rftoolsbuilder.constructor.SchematicTransform;
import mcjty.rftoolsbuilder.constructor.UniversalSchematicLoader;
import mcjty.rftoolsbuilder.constructor.plan.BlockSubstitutionRules;
import mcjty.rftoolsbuilder.constructor.plan.ConstructionPlan;
import net.minecraft.client.KeyMapping;
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
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Client-side deployment/preview handler modeled after Create's schematic handler. */
public final class SchematicPlacementHandler {
    public static final KeyMapping DEPLOY = new KeyMapping("key.rftoolsbuilder.schematic_deploy", InputConstants.KEY_G, KeyMapping.Category.MISC);
    public static final KeyMapping ROTATE = new KeyMapping("key.rftoolsbuilder.schematic_rotate", InputConstants.KEY_R, KeyMapping.Category.MISC);
    public static final KeyMapping MIRROR = new KeyMapping("key.rftoolsbuilder.schematic_mirror", InputConstants.KEY_M, KeyMapping.Category.MISC);

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Map<BlockState, BlockModelRenderState> MODEL_CACHE = new HashMap<>();
    private static boolean installed;
    private static String loadedKey = "";
    private static String loadingKey = "";
    private static String failedPreviewHash = "";
    private static int loadGeneration;
    private static ConstructionPlan plan;
    private static BlockSubstitutionRules substitutions = new BlockSubstitutionRules();
    private static boolean editing;
    private static BlockPos editAnchor = BlockPos.ZERO;
    private static int editRotation;
    private static int editMirror;
    private static int verticalOffset;
    private static int lastCardComponentsHash;

    private SchematicPlacementHandler() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DEPLOY);
        event.register(ROTATE);
        event.register(MIRROR);
    }

    public static void install() {
        if (installed) return;
        installed = true;
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onScroll);
        NeoForge.EVENT_BUS.addListener(SchematicPlacementHandler::onSubmitGeometry);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientSchematicUploader.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        ItemStack card = mc.player.getMainHandItem();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card)) {
            clearTransient();
            return;
        }

        ensurePlan(card);
        if (!SchematicCardItem.deployed(card) && !editing) beginEditing(card);

        if (editing) {
            updateLookAnchor(mc);
            if (ROTATE.consumeClick()) {
                editRotation = Math.floorMod(editRotation + 1, 4);
                MODEL_CACHE.clear();
                message("Schematic rotation: " + (editRotation * 90) + "°");
            }
            if (MIRROR.consumeClick()) {
                editMirror = (editMirror + 1) % 3;
                MODEL_CACHE.clear();
                message("Schematic mirror: " + mirrorName(editMirror));
            }
            if (DEPLOY.consumeClick()) {
                if (mc.player.isShiftKeyDown()) {
                    syncDeployment(card, false);
                    editing = true;
                    message("Schematic deployment cleared");
                } else if (plan != null) {
                    syncDeployment(card, true);
                    editing = false;
                    message("Schematic position confirmed");
                } else {
                    message("Schematic preview is not ready yet");
                }
            }
        } else if (DEPLOY.consumeClick()) {
            beginEditing(card);
            message("Schematic edit mode — look to move, Shift+scroll changes height");
        }
    }

    private static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!editing || mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown()) return;
        ItemStack card = mc.player.getMainHandItem();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card)) return;
        int delta = event.getAccumulatedScrollY();
        if (delta == 0) delta = (int) Math.signum(event.getScrollDeltaY());
        if (delta == 0) return;
        verticalOffset += Integer.signum(delta);
        event.setCanceled(true);
        updateLookAnchor(mc);
        message("Schematic height offset: " + verticalOffset);
    }

    private static void beginEditing(ItemStack card) {
        editing = true;
        editRotation = SchematicCardItem.rotation(card);
        editMirror = SchematicCardItem.mirror(card);
        editAnchor = SchematicCardItem.deployed(card) ? SchematicCardItem.anchor(card) : BlockPos.ZERO;
        verticalOffset = 0;
        MODEL_CACHE.clear();
    }

    private static void updateLookAnchor(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockPos base = hit.getBlockPos().relative(hit.getDirection());
        editAnchor = new BlockPos(base.getX(), base.getY() + verticalOffset, base.getZ());
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

    private record LoadResult(ConstructionPlan plan, boolean missingAuthoritativeCopy, String detail) {}

    private static void ensurePlan(ItemStack card) {
        int componentHash = card.getComponentsPatch().hashCode();
        String clientFile = SchematicCardItem.clientFile(card);
        String sourceType = SchematicCardItem.sourceType(card);
        String sha256 = SchematicCardItem.sha256(card);
        String key = clientFile + "|" + sourceType + "|" + sha256;

        boolean sameCard = key.equals(loadedKey) && componentHash == lastCardComponentsHash;
        if (sameCard && plan != null) return;
        if (sameCard && key.equals(loadingKey)) return;
        if (sameCard && ClientSchematicPreviewCache.isDownloading(sha256)) return;
        if (sameCard && sha256.equals(failedPreviewHash)) return;

        if (!sameCard) {
            ClientSchematicPreviewCache.cancelOutstanding();
            loadedKey = key;
            lastCardComponentsHash = componentHash;
            plan = null;
            MODEL_CACHE.clear();
            substitutions = new BlockSubstitutionRules();
            SchematicCardItem.applyReplacements(card, substitutions);
            failedPreviewHash = "";
            ++loadGeneration;
        }

        SchematicFolderIndex.Format format = SchematicFolderIndex.Format.fromId(sourceType);
        if (format == null) format = SchematicFolderIndex.Format.fromFileName(clientFile);
        if (format == null) {
            failedPreviewHash = sha256;
            message("Unsupported schematic format on card");
            return;
        }

        final SchematicFolderIndex.Format resolvedFormat = format;
        final int generation = loadGeneration;
        final int expectedX = SchematicCardItem.sizeX(card);
        final int expectedY = SchematicCardItem.sizeY(card);
        final int expectedZ = SchematicCardItem.sizeZ(card);
        loadingKey = key;

        CompletableFuture.supplyAsync(() -> loadBestAvailable(clientFile, resolvedFormat, sha256))
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (generation != loadGeneration) return;
                    loadingKey = "";
                    if (error != null) {
                        plan = null;
                        failedPreviewHash = sha256;
                        message("Could not parse schematic preview: " + safeMessage(error));
                        return;
                    }
                    if (result == null || result.missingAuthoritativeCopy()) {
                        plan = null;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && validHash(sha256)) {
                            ClientSchematicPreviewCache.request(mc.player.getInventory().getSelectedSlot(), sha256);
                        } else {
                            failedPreviewHash = sha256;
                            message(result == null || result.detail().isBlank() ? "Schematic preview file is missing" : result.detail());
                        }
                        return;
                    }

                    ConstructionPlan loaded = result.plan();
                    if (loaded == null || loaded.totalTargets() == 0) {
                        plan = null;
                        failedPreviewHash = sha256;
                        message("Schematic contains no previewable targets");
                        return;
                    }
                    if (expectedX > 0 && (loaded.sizeX() != expectedX || loaded.sizeY() != expectedY || loaded.sizeZ() != expectedZ)) {
                        plan = null;
                        failedPreviewHash = sha256;
                        message("Schematic preview bounds do not match the written card");
                        return;
                    }
                    plan = loaded;
                    failedPreviewHash = "";
                }));
    }

    private static LoadResult loadBestAvailable(String clientFile, SchematicFolderIndex.Format format, String sha256) {
        try {
            Path local = SchematicFolderIndex.resolve(clientFile);
            if (local != null && Files.isRegularFile(local)) {
                if (!validHash(sha256) || ClientSchematicPreviewCache.verifySha256(local, sha256)) {
                    return new LoadResult(UniversalSchematicLoader.load(new SchematicFolderIndex.Entry(clientFile, format), false), false, "");
                }
            }

            if (validHash(sha256)) {
                Path cache = ClientSchematicPreviewCache.cachePath(sha256, format);
                if (cache != null) {
                    if (ClientSchematicPreviewCache.verifySha256(cache, sha256)) {
                        SchematicFolderIndex.Entry entry = ClientSchematicPreviewCache.cacheEntry(cache, format);
                        if (entry != null) return new LoadResult(UniversalSchematicLoader.load(entry, false), false, "");
                    } else {
                        try { Files.deleteIfExists(cache); } catch (IOException ignored) {}
                    }
                }
            }
            return new LoadResult(null, true, "Local schematic is unavailable; requesting authoritative server copy");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Called after size + SHA validation and atomic cache commit. */
    public static void onPreviewCacheReady(String hash, SchematicFolderIndex.Format format, Path path) {
        if (!hash.equals(currentCardHash())) return;
        failedPreviewHash = "";
        loadingKey = "";
        plan = null;
        MODEL_CACHE.clear();
        // Keep loadedKey/card hash so ensurePlan retries the same card from its now-verified cache.
    }

    public static void onPreviewCacheFailed(String hash) {
        if (hash == null || hash.isBlank() || hash.equals(currentCardHash())) failedPreviewHash = hash == null ? "" : hash;
        loadingKey = "";
        plan = null;
    }

    private static String currentCardHash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "";
        ItemStack card = mc.player.getMainHandItem();
        return card.getItem() instanceof SchematicCardItem ? SchematicCardItem.sha256(card) : "";
    }

    private static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || plan == null) return;
        ItemStack card = mc.player.getMainHandItem();
        if (!(card.getItem() instanceof SchematicCardItem) || !SchematicCardItem.hasSource(card)) return;

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

    private static void clearTransient() {
        if (!loadedKey.isEmpty() || ClientSchematicPreviewCache.isDownloading(currentCardHash())) {
            ClientSchematicPreviewCache.cancelOutstanding();
            loadedKey = "";
            loadingKey = "";
            failedPreviewHash = "";
            plan = null;
            MODEL_CACHE.clear();
            editing = false;
            verticalOffset = 0;
            ++loadGeneration;
        }
    }

    private static boolean validHash(String hash) {
        return hash != null && hash.matches("[0-9a-fA-F]{64}");
    }

    private static String mirrorName(int mirror) {
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
