package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.HowitzerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renders the M109-style self-propelled howitzer with its own custom texture
 * (olive-drab / gunmetal armour, tread-patterned tracks, Program-cyan turret
 * optics), painted to the model's box-UV layout.
 */
public class HowitzerRenderer extends MobEntityRenderer<HowitzerEntity, HowitzerModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/howitzer.png");
	// Big ground vehicle, bigger than the IFV's own scale bump — scaled +35%
	// again ("still too small") to roughly fill the new ~3.9 x 3.5 hitbox;
	// see the matching P7Entities dimensions bump.
	private static final float MODEL_SCALE = 2.16f;

	public HowitzerRenderer(EntityRendererFactory.Context context) {
		super(context, new HowitzerModel(context.getPart(HowitzerModel.LAYER)), 1.7f);
	}

	@Override
	public Identifier getTexture(HowitzerEntity entity) {
		return TEXTURE;
	}

	@Override
	protected void scale(HowitzerEntity entity, MatrixStack matrices, float amount) {
		super.scale(entity, matrices, amount);
		matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
	}
}
