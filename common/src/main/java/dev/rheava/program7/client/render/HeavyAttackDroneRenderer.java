package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HeavyAttackDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class HeavyAttackDroneRenderer extends MobEntityRenderer<HeavyAttackDroneEntity, HeavyAttackDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/heavy_attack_drone.png");
	// Tier 3 size bump — scaled +35% again ("still too small") to the
	// ~3.5 x 3.0 hitbox; see the matching P7Entities dimensions bump.
	private static final float MODEL_SCALE = 1.8f;

	public HeavyAttackDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new HeavyAttackDroneModel(context.getPart(HeavyAttackDroneModel.LAYER)), 0.9f);
	}

	@Override
	public Identifier getTexture(HeavyAttackDroneEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(HeavyAttackDroneEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
