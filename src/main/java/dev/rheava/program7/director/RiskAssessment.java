package dev.rheava.program7.director;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

/**
 * How the Program decides what you are worth.
 *
 * <p>Risk tiers (from the design doc):
 * <ul>
 *   <li>Tier 1 — low risk: no armor, little gear, high deaths. Response: a
 *       single automated explosive attack drone.</li>
 *   <li>Tier 2 — medium risk: iron-grade armor, real weapons, occasional
 *       deaths. Response: a few drones plus base auto-turret fire.</li>
 *   <li>Tier 3 — high risk: diamond-grade and above. Response: full defense
 *       network, skirmishers and creeper-style explosive drones.</li>
 * </ul>
 */
public final class RiskAssessment {
	public static ScanRecord assess(ServerPlayerEntity player) {
		int gearScore = player.getArmor() + weaponScore(player.getMainHandStack())
				+ weaponScore(player.getOffHandStack()) / 2;

		int tier;
		if (gearScore >= 18) {
			tier = 3;
		} else if (gearScore >= 8) {
			tier = 2;
		} else {
			tier = 1;
		}

		int deaths = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
		// A player who dies constantly is not a priority target, however shiny.
		if (deaths >= 10 && tier > 1) {
			tier--;
		}

		return new ScanRecord(tier, weaponProfile(player), hasElytra(player), deaths);
	}

	private static int weaponScore(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem
				|| stack.getItem() instanceof TridentItem) {
			return 4;
		}
		if (stack.getItem() instanceof RangedWeaponItem) {
			return 4;
		}
		return 0;
	}

	private static String weaponProfile(ServerPlayerEntity player) {
		ItemStack main = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		if (main.getItem() instanceof RangedWeaponItem || off.getItem() instanceof RangedWeaponItem) {
			return ScanRecord.PROFILE_RANGED;
		}
		if (main.getItem() instanceof SwordItem || main.getItem() instanceof AxeItem
				|| main.getItem() instanceof TridentItem) {
			return ScanRecord.PROFILE_MELEE;
		}
		return ScanRecord.PROFILE_NONE;
	}

	private static boolean hasElytra(ServerPlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
	}

	private RiskAssessment() {
	}
}
