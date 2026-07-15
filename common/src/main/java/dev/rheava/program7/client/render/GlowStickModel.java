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
 * The thrown glow stick in flight: a slim shaft with a brighter bulb at the
 * tip, the same static-geometry "dart" shape as {@link MortarShellModel} /
 * {@link HowitzerShellModel} (see those classes' docs for why this isn't a
 * living {@code EntityModel}).
 */
public class GlowStickModel {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("glow_stick"), "main");

	private final ModelPart root;

	public GlowStickModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		root.addChild("glow_stick",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-1.0f, -5.0f, -1.0f, 2.0f, 8.0f, 2.0f)
						.uv(0, 10).cuboid(-1.5f, -8.0f, -1.5f, 3.0f, 3.0f, 3.0f),
				ModelTransform.NONE);

		return TexturedModelData.of(modelData, 16, 16);
	}

	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
		this.root.render(matrices, vertexConsumer, light, overlay);
	}
}
