package dev.rheava.program7.client.render;

import dev.rheava.program7.entity.ArmorPiercingArrowEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * Reuses the vanilla arrow texture rather than shipping a bespoke model —
 * {@link ProjectileEntityRenderer} already draws the standard crossed-quad
 * projectile shape procedurally, so this class only has to say which
 * texture to paint it with.
 */
public class ArmorPiercingArrowRenderer extends ProjectileEntityRenderer<ArmorPiercingArrowEntity> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/projectiles/arrow.png");

	public ArmorPiercingArrowRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(ArmorPiercingArrowEntity entity) {
		return TEXTURE;
	}
}
