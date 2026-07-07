package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * The falling mortar bomb: a stubby dart body with a single tail fin. Static
 * geometry only — it doesn't animate, it just falls and detonates. Not an
 * {@code EntityModel}: the shell isn't a living entity, so it's rendered
 * directly by {@link MortarShellRenderer} instead of going through
 * MobEntityRenderer's living-entity machinery.
 */
public class MortarShellModel {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("mortar_shell"), "main");

	private final ModelPart root;

	public MortarShellModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		root.addChild("shell",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-1.0f, -2.0f, -1.0f, 2.0f, 3.0f, 2.0f)
						.uv(8, 0).cuboid(-0.5f, 1.0f, -0.5f, 1.0f, 1.0f, 1.0f),
				ModelTransform.NONE);

		return TexturedModelData.of(modelData, 16, 16);
	}

	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
		this.root.render(matrices, vertexConsumer, light, overlay);
	}
}
