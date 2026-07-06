package dev.rheava.program7.client.render;

import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.registry.P7Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the descending pod as the probe core block it is about to become.
 * A dedicated pod model can replace this later without touching the entity.
 */
public class DropPodRenderer extends EntityRenderer<DropPodEntity> {
	public DropPodRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.shadowRadius = 0.6f;
	}

	@Override
	public void render(DropPodEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.translate(-0.5, 0.0, -0.5);
		MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
				P7Blocks.PROBE_CORE.getDefaultState(), matrices, vertexConsumers,
				light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(DropPodEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
