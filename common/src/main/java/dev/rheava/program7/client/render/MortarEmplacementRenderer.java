package dev.rheava.program7.client.render;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.MortarEmplacementEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MortarEmplacementRenderer extends MobEntityRenderer<MortarEmplacementEntity, MortarEmplacementModel> {
	private static final Identifier TEXTURE = Program7.id("textures/entity/mortar_emplacement.png");

	public MortarEmplacementRenderer(EntityRendererFactory.Context context) {
		super(context, new MortarEmplacementModel(context.getPart(MortarEmplacementModel.LAYER)), 0.45f);
		// Static emplacement: vanilla's small circular blob shadow reads wrong
		// under a wide mortar carriage, so drop it to effectively nothing.
		this.shadowRadius = 0.0f;
	}

	@Override
	public Identifier getTexture(MortarEmplacementEntity entity) {
		return TEXTURE;
	}
}
