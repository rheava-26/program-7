package dev.rheava.program7.item;

import dev.rheava.program7.entity.GlowStickEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Phase 4's first player-craftable tool: a cheap, throwable light source. The
 * design brief's line in the sand for reverse-engineered gear is that player
 * tools stay cool and fun without trivializing the horror, so this is
 * deliberately NOT a permanent light — see {@link
 * dev.rheava.program7.block.GlowStickBlock} for the burn-down that makes
 * that true. Right-click throws one (consuming it outside creative), same
 * shape as a vanilla snowball/egg toss.
 */
public class GlowStickItem extends Item {
	/** Snowball-ish toss speed, in blocks/tick, along the player's look vector. */
	private static final float THROW_SPEED = 0.9F;
	/** A small cooldown so mashing the button doesn't machine-gun them out. */
	private static final int USE_COOLDOWN_TICKS = 4;

	public GlowStickItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.GLOW_STICK_THROW.get(),
				SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClient) {
			GlowStickEntity entity = new GlowStickEntity(world, user);
			entity.launch(user, THROW_SPEED);
			world.spawnEntity(entity);
			user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
		}
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		if (!user.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		return TypedActionResult.success(stack);
	}
}
