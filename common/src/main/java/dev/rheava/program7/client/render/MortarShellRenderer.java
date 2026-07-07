package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MortarShellEntity;
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
 * The shell isn't a living entity — like {@link DropPodRenderer}, this is a
 * plain {@code EntityRenderer} subclass rather than a
 * MobEntityRenderer/LivingEntityRenderer. It bakes its model layer once in
 * the constructor via {@code context.getPart}, then draws it straight into
 * the passed-in matrices with a hand-picked {@code RenderLayer.getEntityCutout}
 * buffer instead of relying on the living-entity render pipeline.
 */
public class MortarShellRenderer extends EntityRenderer<MortarShellEntity> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/mortar_shell.png");

	private final MortarShellModel model;

	public MortarShellRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.model = new MortarShellModel(context.getPart(MortarShellModel.LAYER));
		this.shadowRadius = 0.2f;
	}

	@Override
	public void render(MortarShellEntity entity, float yaw, float tickDelta, MatrixStack matrices,
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
	public Identifier getTexture(MortarShellEntity entity) {
		return TEXTURE;
	}
}
