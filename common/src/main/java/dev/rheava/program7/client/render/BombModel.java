package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Shares the howitzer shell's geometry ({@link HowitzerShellModel}) as a
 * placeholder — same non-living-projectile shape, baked under its own layer
 * for the CAS bomb entity. A dedicated finned-bomb silhouette is a later
 * art pass.
 */
public class BombModel {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Program7.id("bomb"), "main");

	private final ModelPart root;

	public BombModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		return HowitzerShellModel.getTexturedModelData();
	}

	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay) {
		this.root.render(matrices, vertexConsumer, light, overlay);
	}
}
