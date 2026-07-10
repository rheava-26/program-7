package dev.rheava.program7.entity;

import java.util.ArrayList;
import java.util.List;

import dev.rheava.program7.advancement.P7Advancements;
import dev.rheava.program7.block.DroneWreckBlock;
import dev.rheava.program7.entity.ArmorProfile.DamageClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Program hardware. Encodes the faction-wide combat feel:
 *
 * <ul>
 *   <li><b>Fragile airframes:</b> swords and axes shred small fliers —
 *       usually a one-hit kill.</li>
 *   <li><b>Knockback scrambles:</b> Knockback/Punch-enchanted hits throw
 *       fliers off course, killing their control for a moment; hitting
 *       terrain while scrambled is a crash.</li>
 *   <li><b>Wrecks, not item sprays:</b> destroyed units crash into a
 *       lootable wreck block holding their salvage (loot table rolls plus
 *       anything the unit was carrying).</li>
 *   <li>Machines: poison-immune, never despawn, no fall damage.</li>
 * </ul>
 */
public abstract class ProgramDroneEntity extends PathAwareEntity {
	/**
	 * Shared perception/behaviour ramp, lowest to highest alert. Any Program
	 * unit can carry one of these; it's read by {@link #getAlertState()} and
	 * meant to eventually surface on the {@link dev.rheava.program7.item.DatapadItem}
	 * readout so a player can tell how "made" they are, not just whether a
	 * unit currently has a target.
	 *
	 * <ul>
	 *   <li>{@link #UNAWARE} — default; nothing has caught the unit's attention.</li>
	 *   <li>{@link #SUSPICIOUS} — noticed a disturbance, hasn't investigated yet.</li>
	 *   <li>{@link #SEARCHING} — actively moving to look at a disturbance.</li>
	 *   <li>{@link #TRACKING} — confirmed a player and is holding contact
	 *       (used by unarmed spotters/scouts, who report rather than fight).</li>
	 *   <li>{@link #ENGAGING} — confirmed a player and is a ranged attacker,
	 *       i.e. weapons are hot.</li>
	 * </ul>
	 */
	public enum AlertState {
		UNAWARE, SUSPICIOUS, SEARCHING, TRACKING, ENGAGING
	}

	/**
	 * Aim high to clip the rotors: a hit landing in this top slice of the
	 * hitbox is treated as a disabling shot to the rotor plane, not a graze.
	 */
	private static final double ROTOR_HIT_SLICE_HEIGHT = 0.25;

	/** Range within which a unit closing on its target starts audibly winding up — see #4. */
	private static final double APPROACH_WHIR_RANGE = 24.0;

	private int scrambledTicks = 0;
	private int retreatTicks = 0;
	@Nullable
	private LivingEntity retreatFrom;
	/** Set when a mace blow shatters this (unarmored) airframe; read by {@link #onDeath}. */
	private boolean maceShattered = false;
	/** Ticks left before the next approach-whir cue is allowed to play. */
	private int approachWhirCooldown = 0;
	/** Ticks left before the next {@link #tickApproachAlarm} scan actually runs. */
	private int alarmTickThrottle = 0;
	/** Latched so the alarm sounds once per approach, not once per tick a player is in range. */
	private boolean alarmLatched = false;
	/** Throttles {@link #tickVisionProjection} to every few ticks instead of every tick. */
	private int visionProjectionCounter = 0;
	/** Last tick's {@link #getTarget()}, used by {@link #tickAcquisitionAlert} to catch the
	 *  no-target -&gt; has-target transition ("freak out on acquisition"). */
	@Nullable
	private LivingEntity lastTarget = null;
	/** See {@link AlertState}; persisted so a reload doesn't silently reset a unit's posture. */
	private AlertState alertState = AlertState.UNAWARE;

	protected ProgramDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Fliers scramble and crash; ground units just get shoved around. */
	protected boolean isFlier() {
		return true;
	}

	/** Fixed-wing airframes (the Air UAV) — see {@code fixed_wing_melee} advancement. */
	protected boolean isFixedWing() {
		return false;
	}

	/**
	 * Gun/missile units the Program can point at a player from range — see
	 * {@code sustained_fire} advancement. Public: queried cross-package by
	 * {@link dev.rheava.program7.director.ProgramDirectorState#tick}.
	 */
	public boolean isRangedAttacker() {
		return false;
	}

	/** Extra stacks (stolen cargo etc.) added to this unit's wreck. */
	protected List<ItemStack> getExtraWreckSalvage() {
		return List.of();
	}

	/**
	 * Per-damage-type resistance profile. Unarmored by default; armored
	 * units override this to give different weapon types a genuinely
	 * different fight against them instead of one flat reduction.
	 */
	protected ArmorProfile armorProfile() {
		return ArmorProfile.UNARMORED;
	}

	public boolean isScrambled() {
		return this.scrambledTicks > 0;
	}

	/** Current position on the {@link AlertState} ramp. */
	public AlertState getAlertState() {
		return this.alertState;
	}

	public void setAlertState(AlertState alertState) {
		this.alertState = alertState;
	}

	/**
	 * Shared "break contact" state used by {@link dev.rheava.program7.entity.ai.RetreatGoal}.
	 * Any drone that needs to bug out — after finishing a job, or just because
	 * it got hurt and it doesn't fight back — calls this instead of each unit
	 * rolling its own flee timer.
	 */
	public void beginRetreat(@Nullable LivingEntity threat, int ticks) {
		this.retreatFrom = threat;
		this.retreatTicks = ticks;
	}

	public boolean isRetreating() {
		return this.retreatTicks > 0;
	}

	@Nullable
	public LivingEntity getRetreatFrom() {
		return this.retreatFrom;
	}

	/** How long a unit that breaks off a losing fight stays in flight before it may re-engage. */
	private static final int FLEE_DURATION_TICKS = 120;

	/**
	 * The fraction of max health at or below which this unit breaks off a fight
	 * and flees its attacker (needs a {@link dev.rheava.program7.entity.ai.RetreatGoal}
	 * registered to actually move). Default {@code 0} — most units fight to the
	 * death; a subclass overrides this to say "I bug out when I'm losing." A
	 * committed suicide unit should leave it at 0.
	 */
	protected float fleeHealthFraction() {
		return 0.0f;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean wasFullHealth = this.getHealth() >= this.getMaxHealth() - 0.01f;
		if (!this.getWorld().isClient) {
			LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
			ItemStack weapon = attacker != null ? attacker.getMainHandStack() : ItemStack.EMPTY;
			boolean mace = weapon.getItem() instanceof MaceItem;
			if (attacker != null) {
				if (this.isFlier()
						&& (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem)) {
					// Blades shred airframes: small drones usually die in one hit.
					amount *= 2.5f;
				}
				if (mace && this.isFlier()) {
					// Heavy airframe impact: a mace's smashing weight is even
					// worse for a fragile flier than a blade's edge.
					amount *= 1.5f;
				}
				int scramblePower = this.getEnchantLevel(weapon, Enchantments.KNOCKBACK)
						+ this.getEnchantLevel(weapon, Enchantments.PUNCH);
				if (scramblePower > 0 && this.isFlier()) {
					this.scramble(attacker, scramblePower);
				}
			}

			if (this.isFlier() && this.isRotorHit(source, attacker)) {
				// Rotor-plane hit: stacks multiplicatively on top of whatever
				// the blade bonus above already did, so a blade swung down
				// from above a small flier is an even cleaner kill.
				amount *= 1.5f;
				if (attacker != null) {
					this.scramble(attacker, 2);
				}
				this.spawnRotorHitEffects();
			}

			// Typed armor: layer the per-damage-class multiplier on top of
			// everything above, so armored units get a real, differently
			// textured fight depending on what's actually hitting them.
			amount *= this.armorProfile().multiplierFor(this.classify(source, weapon));

			if (mace && this.armorProfile() == ArmorProfile.UNARMORED) {
				// The mace-shatter visual stays for light drones; the "Overkill
				// much?" advancement moved to one-shotting a gunship (below).
				this.maceShattered = true;
			}
		}

		boolean took = super.damage(source, amount);

		if (took && !this.getWorld().isClient) {
			LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;

			// "Overkill much?": drop a full-health gunship with a single mace
			// blow — the apex airframe cracked open in one swing. Requires an
			// actual melee hit (PLAYER_ATTACK), so a player-triggered TNT/crystal
			// blast while merely holding a mace doesn't count.
			if (this instanceof GunshipEntity && wasFullHealth && !this.isAlive()
					&& source.isOf(DamageTypes.PLAYER_ATTACK)
					&& attacker instanceof ServerPlayerEntity sp
					&& sp.getMainHandStack().getItem() instanceof MaceItem) {
				P7Advancements.grant(sp, "overkill_much");
			}

			// Break off a fight that's going badly: once a unit built to flee
			// drops to its threshold, it bugs out from whatever just hit it
			// rather than trading down to zero.
			float frac = this.fleeHealthFraction();
			if (attacker != null && frac > 0.0f && this.isAlive() && !this.isRetreating()
					&& this.getHealth() <= this.getMaxHealth() * frac) {
				this.beginRetreat(attacker, FLEE_DURATION_TICKS);
			}
		}
		return took;
	}

	/**
	 * Buckets incoming damage into a {@link DamageClass} so
	 * {@link #armorProfile()} can apply a type-specific multiplier.
	 */
	private DamageClass classify(DamageSource source, ItemStack weapon) {
		if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
			return DamageClass.EXPLOSIVE;
		}
		if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			if (source.getSource() instanceof TridentEntity) {
				return DamageClass.PIERCING;
			}
			if (source.getSource() instanceof ArrowEntity || source.getSource() instanceof SpectralArrowEntity
					|| source.getSource() instanceof PersistentProjectileEntity) {
				return DamageClass.HIGH_VELOCITY_IMPACT;
			}
			// Anything else riding the projectile tag — most gun-mod bullets
			// included — reads as conventional ballistic fire.
			return DamageClass.BALLISTIC;
		}
		if (source.getAttacker() instanceof LivingEntity) {
			int enchantedPower = this.getEnchantLevel(weapon, Enchantments.SHARPNESS)
					+ this.getEnchantLevel(weapon, Enchantments.SMITE)
					+ this.getEnchantLevel(weapon, Enchantments.BANE_OF_ARTHROPODS)
					+ this.getEnchantLevel(weapon, Enchantments.IMPALING);
			return enchantedPower > 0 ? DamageClass.ENCHANTED : DamageClass.MELEE;
		}
		return DamageClass.GENERIC;
	}

	/**
	 * A rotor-plane hit is one that lands in the top slice of the hitbox —
	 * either a melee attacker looking down into it, or a projectile that
	 * struck up there.
	 */
	private boolean isRotorHit(DamageSource source, @Nullable LivingEntity attacker) {
		double sliceMinY = this.getBoundingBox().maxY - ROTOR_HIT_SLICE_HEIGHT;
		if (attacker != null && attacker.getEyeY() >= sliceMinY) {
			return true;
		}
		if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			Vec3d position = source.getPosition();
			if (position != null && position.y >= sliceMinY) {
				return true;
			}
		}
		return false;
	}

	private void spawnRotorHitEffects() {
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			Box box = this.getBoundingBox();
			double x = (box.minX + box.maxX) / 2.0;
			double z = (box.minZ + box.maxZ) / 2.0;
			serverWorld.spawnParticles(ParticleTypes.CRIT, x, box.maxY, z, 8, 0.2, 0.05, 0.2, 0.02);
			serverWorld.spawnParticles(ParticleTypes.SMOKE, x, box.maxY, z, 4, 0.15, 0.05, 0.15, 0.01);
		}
		this.playSound(P7Sounds.DRONE_HURT.get(), 1.0f, 0.6f);
	}

	private int getEnchantLevel(ItemStack weapon, RegistryKey<Enchantment> key) {
		RegistryEntry<Enchantment> entry = this.getWorld().getRegistryManager()
				.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key);
		return EnchantmentHelper.getLevel(entry, weapon);
	}

	private void scramble(LivingEntity attacker, int power) {
		this.scrambledTicks = Math.max(this.scrambledTicks, 20 + 15 * power);
		Vec3d away = this.getPos().subtract(attacker.getPos());
		if (away.lengthSquared() < 1.0E-4) {
			away = new Vec3d(1.0, 0.0, 0.0);
		}
		away = away.normalize().multiply(0.5 * power);
		this.addVelocity(away.x, 0.25 + 0.1 * power, away.z);
		this.playSound(P7Sounds.DRONE_ALERT.get(), 0.8f, 1.7f);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!this.getWorld().isClient) {
			if (this.scrambledTicks > 0) {
				this.scrambledTicks--;
				// Stabilizers are gone: no pathing, wild spin, drifting thrust.
				this.getNavigation().stop();
				this.setYaw(this.getYaw() + (this.random.nextFloat() - 0.5f) * 45.0f);
				this.addVelocity((this.random.nextDouble() - 0.5) * 0.12,
						(this.random.nextDouble() - 0.5) * 0.08,
						(this.random.nextDouble() - 0.5) * 0.12);
				if (this.horizontalCollision) {
					// Slammed into terrain while tumbling.
					this.damage(this.getDamageSources().flyIntoWall(), 5.0f);
					this.scrambledTicks = Math.min(this.scrambledTicks, 4);
				}
			}
			if (this.retreatTicks > 0) {
				this.retreatTicks--;
				if (this.retreatTicks == 0) {
					this.retreatFrom = null;
				}
			}
			this.tickApproachWhir();
			this.tickVisionProjection();
			this.tickAcquisitionAlert();
		}
	}

	/**
	 * Provisional first-pass VFX: while actively hunting a live target, draws
	 * a line of particles from this unit's eye out toward its target so the
	 * player can see the drone's projected line of vision land on them (or
	 * not) and can actually read whether they're hidden.
	 *
	 * TODO(manager): tune vision-beam particle/spacing/color.
	 */
	private void tickVisionProjection() {
		LivingEntity target = this.getTarget();
		if (target == null) {
			return;
		}
		// Throttle to every 3rd tick to keep particle counts sane across a swarm.
		this.visionProjectionCounter++;
		if (this.visionProjectionCounter % 3 != 0) {
			return;
		}
		if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		Vec3d eye = this.getEyePos();
		Vec3d targetEye = target.getEyePos();
		double distance = Math.min(this.distanceTo(target), 20.0);
		if (distance < 1.0E-4) {
			return;
		}
		Vec3d direction = targetEye.subtract(eye).normalize();

		final int samples = 8;
		for (int i = 1; i <= samples; i++) {
			double t = distance * i / samples;
			Vec3d point = eye.add(direction.multiply(t));
			serverWorld.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/**
	 * "Freak out on acquisition": the instant this unit locks a fresh target
	 * (transitioning from no target to a live {@link PlayerEntity}), give the
	 * player a sharp audible+visual tell instead of the drone silently
	 * swinging its aim onto them. This is also the top of the {@link
	 * AlertState} ramp: confirmed contact pushes the unit to {@link
	 * AlertState#ENGAGING} (ranged attackers) or {@link AlertState#TRACKING}
	 * (unarmed spotters/scouts, which report rather than fight) — see {@link
	 * dev.rheava.program7.entity.ai.InvestigateDisturbanceGoal} for the goal
	 * that usually feeds a target in here.
	 *
	 * TODO(manager): a real move-control jolt / erratic-motion tell on
	 * acquisition would sell this even harder — out of scope for this pass,
	 * this is sound+particle only.
	 */
	private void tickAcquisitionAlert() {
		LivingEntity currentTarget = this.getTarget();
		if (currentTarget != this.lastTarget) {
			if (this.lastTarget == null && currentTarget instanceof PlayerEntity) {
				this.playSound(P7Sounds.DRONE_ALERT.get(), 1.2f, 1.0f);
				if (this.getWorld() instanceof ServerWorld serverWorld) {
					Box box = this.getBoundingBox();
					double x = (box.minX + box.maxX) / 2.0;
					double y = (box.minY + box.maxY) / 2.0;
					double z = (box.minZ + box.maxZ) / 2.0;
					serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 10, 0.3, 0.3, 0.3, 0.05);
				}
				this.alertState = this.isRangedAttacker() ? AlertState.ENGAGING : AlertState.TRACKING;
			} else if (currentTarget == null
					&& (this.alertState == AlertState.TRACKING || this.alertState == AlertState.ENGAGING)) {
				// Lost the target: step back down one rung instead of
				// snapping straight to unaware, so a fresh disturbance can
				// still pick the thread back up.
				this.alertState = AlertState.SUSPICIOUS;
			}
			this.lastTarget = currentTarget;
		}
	}

	/**
	 * Grows noisier as a unit closes on its target: a periodic whir cue,
	 * gated by an internal cooldown and scaled louder the nearer the target
	 * gets, so something bearing down on a player doesn't sneak in silent.
	 * Jittered so a swarm doesn't all whir on the same tick.
	 */
	private void tickApproachWhir() {
		if (this.approachWhirCooldown > 0) {
			this.approachWhirCooldown--;
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null) {
			return;
		}
		double distance = this.distanceTo(target);
		if (distance > APPROACH_WHIR_RANGE) {
			return;
		}
		float proximity = (float) ((APPROACH_WHIR_RANGE - distance) / APPROACH_WHIR_RANGE);
		float volume = 0.3f + proximity * 0.7f;
		this.playSound(P7Sounds.DRONE_WHIR.get(), volume, 0.9f + this.random.nextFloat() * 0.2f);
		// 15-25 ticks, jittered so multiple units don't chorus in lockstep.
		this.approachWhirCooldown = 15 + this.random.nextInt(11);
	}

	/**
	 * Fixed-emplacement contact alarm: raises {@link P7Sounds#UNIT_ALARM}
	 * once when a hostile player first enters {@code detectionRange}, and
	 * un-latches once no player remains in range so it can fire again on the
	 * next approach. {@code throttleTicks} bounds how often the (relatively
	 * cheap, but not free) nearest-player scan actually runs; callers can
	 * safely invoke this every tick.
	 */
	protected void tickApproachAlarm(double detectionRange, int throttleTicks) {
		if (this.getWorld().isClient) {
			return;
		}
		if (this.alarmTickThrottle > 0) {
			this.alarmTickThrottle--;
			return;
		}
		this.alarmTickThrottle = throttleTicks;

		PlayerEntity nearest = this.getWorld().getClosestPlayer(this, detectionRange);
		boolean playerNear = nearest != null && !nearest.isSpectator() && !nearest.isCreative();
		if (playerNear && !this.alarmLatched) {
			this.alarmLatched = true;
			this.playSound(P7Sounds.UNIT_ALARM.get(), 1.0f, 1.0f);
		} else if (!playerNear) {
			this.alarmLatched = false;
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putString("AlertState", this.alertState.name());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("AlertState")) {
			try {
				this.alertState = AlertState.valueOf(nbt.getString("AlertState"));
			} catch (IllegalArgumentException e) {
				this.alertState = AlertState.UNAWARE;
			}
		}
	}

	@Override
	protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
		// Salvage goes into the wreck (see onDeath) instead of item sprays.
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		if (this.getWorld() instanceof ServerWorld world) {
			List<ItemStack> salvage = new ArrayList<>(this.getExtraWreckSalvage());
			LootTable lootTable = world.getServer().getReloadableRegistries()
					.getLootTable(this.getLootTable());
			LootContextParameterSet params = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, this)
					.add(LootContextParameters.ORIGIN, this.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, damageSource)
					.build(LootContextTypes.ENTITY);
			lootTable.generateLoot(params, salvage::add);

			// Fliers usually die mid-air; drop the wreck to the ground below
			// instead of leaving it floating, and give the landing a real
			// impact instead of just puffing into place.
			BlockPos landingPos = findGroundBelow(world, this.getBlockPos());
			DroneWreckBlock.placeWreck(world, landingPos, salvage);
			world.spawnParticles(ParticleTypes.SMOKE,
					landingPos.getX() + 0.5, landingPos.getY() + 0.3, landingPos.getZ() + 0.5,
					4, 0.3, 0.05, 0.3, 0.02);
			world.spawnParticles(ParticleTypes.CRIT,
					landingPos.getX() + 0.5, landingPos.getY() + 0.3, landingPos.getZ() + 0.5,
					4, 0.3, 0.1, 0.3, 0.05);
			world.playSound(null, landingPos, P7Sounds.DRONE_IMPACT.get(), SoundCategory.HOSTILE,
					0.9f, 0.9f + world.random.nextFloat() * 0.2f);

			if (this.maceShattered) {
				// A mace doesn't just kill a light airframe, it blows it apart —
				// a bigger, more violent burst than a normal rotor-hit or death.
				Box box = this.getBoundingBox();
				double x = (box.minX + box.maxX) / 2.0;
				double y = (box.minY + box.maxY) / 2.0;
				double z = (box.minZ + box.maxZ) / 2.0;
				world.spawnParticles(ParticleTypes.CRIT, x, y, z, 24, 0.4, 0.3, 0.4, 0.1);
				world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 12, 0.4, 0.3, 0.4, 0.1);
				world.spawnParticles(ParticleTypes.CLOUD, x, y, z, 8, 0.4, 0.3, 0.4, 0.1);
				this.playSound(P7Sounds.DRONE_DEATH.get(), 1.0f, 1.4f);
			}

			if (this.isFixedWing() && damageSource.getAttacker() instanceof ServerPlayerEntity killer
					&& !damageSource.isIn(DamageTypeTags.IS_PROJECTILE) && !killer.isFallFlying()) {
				// A fixed-wing airframe is meant to be unreachable in melee
				// without an elytra/wind-charge assist — this is the "how???" case.
				P7Advancements.grant(killer, "fixed_wing_melee");
			}
		}
	}

	/**
	 * Scans straight down from {@code start} to the first solid ground,
	 * stopping at the world's bottom if it never finds one. If {@code start}
	 * is already on the ground this returns it unchanged, so a ground unit's
	 * wreck lands exactly where it always did.
	 */
	private static BlockPos findGroundBelow(ServerWorld world, BlockPos start) {
		BlockPos.Mutable pos = start.mutableCopy();
		int bottom = world.getBottomY();
		while (pos.getY() > bottom && world.getBlockState(pos.down()).isReplaceable()) {
			pos.move(0, -1, 0);
		}
		return pos.toImmutable();
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		// Machines don't bleed — poison does nothing. Wither, per the design
		// doc, tears through drone plating just fine.
		if (effect.getEffectType() == StatusEffects.POISON) {
			return false;
		}
		return super.canHaveStatusEffect(effect);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public void checkDespawn() {
		// Program hardware is deployed deliberately; it never just despawns.
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.DRONE_AMBIENT.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return P7Sounds.DRONE_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return P7Sounds.DRONE_DEATH.get();
	}
}
