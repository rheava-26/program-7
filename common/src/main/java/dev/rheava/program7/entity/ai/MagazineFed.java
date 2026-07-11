package dev.rheava.program7.entity.ai;

/**
 * Firing-side companion to {@code dev.rheava.program7.entity.ReloadableWeapon}
 * (the cross-package resupply contract a logistics drone codes against).
 * That interface only covers checking/topping up a magazine; this one is the
 * other half — letting a shared attack goal that only knows its shooter as
 * the base {@code ProgramDroneEntity} ask "spend a round, did you actually
 * have one" without needing to know the concrete mount type.
 *
 * <p>Every mount that implements {@code ReloadableWeapon} should implement
 * this too, backed by the same round counter, so
 * {@link GunAttackGoal}/{@link BurstGunAttackGoal} (and the artillery goals)
 * can gate firing generically with a single {@code instanceof} check instead
 * of every goal needing to know every reloadable mount's concrete type.
 */
public interface MagazineFed {
	/**
	 * Spends one round if the magazine has any left.
	 *
	 * @return {@code true} if a round was available and has been deducted —
	 *         the shot may proceed; {@code false} if the magazine was
	 *         already empty — no shot goes out.
	 */
	boolean consumeRound();
}
