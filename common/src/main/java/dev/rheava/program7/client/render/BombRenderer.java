package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.BombEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Same non-living-entity render shape as {@link MortarShellRenderer} (see
 * that class's doc). Reuses the mortar shell's texture as a placeholder — a
 * real finned-bomb texture is a later art pass.
 */
public class BombRenderer extends EntityRenderer<BombEntity> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/mortar_shell.png");

	private final BombModel model;

	public BombRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.model = new BombModel(context.getPart(BombModel.LAYER));
		this.shadowRadius = 0.35f;
	}

	@Override
	public void render(BombEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch(tickDelta)));

		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(this.getTexture(entity)));
		this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(BombEntity entity) {
		return TEXTURE;
	}
}
