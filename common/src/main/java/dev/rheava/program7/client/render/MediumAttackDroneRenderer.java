package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MediumAttackDroneEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MediumAttackDroneRenderer extends MobEntityRenderer<MediumAttackDroneEntity, MediumAttackDroneModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/medium_attack_drone.png");
	// Tier 2 size bump ("still too small") to the ~1.2 x 0.8 hitbox; see the
	// matching P7Entities dimensions bump.
	private static final float MODEL_SCALE = 1.35f;

	public MediumAttackDroneRenderer(EntityRendererFactory.Context context) {
		super(context, new MediumAttackDroneModel(context.getPart(MediumAttackDroneModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(MediumAttackDroneEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(MediumAttackDroneEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
