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
 * The howitzer's shell in flight: the same static-geometry dart shape as
 * {@link MortarShellModel} (see that class's doc for why this isn't a living
 * {@code EntityModel}), just scaled up into the heavier round the design doc
 * calls for.
 */
public class HowitzerShellModel {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("howitzer_shell"), "main");

	private final ModelPart root;

	public HowitzerShellModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		root.addChild("shell",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-1.5f, -3.0f, -1.5f, 3.0f, 5.0f, 3.0f)
						.uv(12, 0).cuboid(-0.75f, 2.0f, -0.75f, 1.5f, 1.5f, 1.5f),
				ModelTransform.NONE);

		return TexturedModelData.of(modelData, 32, 32);
	}

	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
		this.root.render(matrices, vertexConsumer, light, overlay);
	}
}
