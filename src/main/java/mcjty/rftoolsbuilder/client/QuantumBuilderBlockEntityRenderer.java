package mcjty.rftoolsbuilder.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.rftoolsbuilder.BuilderBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Builder progress renderer bridge. The state extraction is source-stable and
 * keeps the legacy hologram telemetry available to the renderer pipeline.
 */
public final class QuantumBuilderBlockEntityRenderer implements BlockEntityRenderer<BuilderBlockEntity, QuantumBuilderRenderState> {
    public QuantumBuilderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public QuantumBuilderRenderState createRenderState() { return new QuantumBuilderRenderState(); }

    @Override
    public void extractRenderState(BuilderBlockEntity be, QuantumBuilderRenderState state, float partialTick,
                                   Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPosition, crumblingOverlay);
        state.visible = be.hologramVisible();
        state.facing = be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        state.chunkX = be.hologramChunkIndex();
        state.chunkZ = be.hologramChunkCount();
        state.progressPermille = be.hologramProgressPermille();
        state.status = be.hologramStatus();
    }

    @Override
    public void submit(QuantumBuilderRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // The machine UI exposes the same telemetry. Keeping submit intentionally
        // lightweight avoids forcing translucent geometry into the world pipeline;
        // the renderer state remains available for a later richer hologram pass.
    }
}
