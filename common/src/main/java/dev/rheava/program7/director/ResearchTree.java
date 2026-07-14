package dev.rheava.program7.director;

import java.util.List;
import java.util.Map;

import dev.rheava.program7.Program7;
import dev.rheava.program7.block.PsionicResearchBlockEntity;
import dev.rheava.program7.registry.P7Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Player reverse-engineering progression, embodied in a building — see
 * {@code RESEARCH_TREE.md} §1/§3. A sibling organ of {@link
 * FireMissionManager} and {@link TerrainSaturation}: a plain class, owned
 * field on {@link ProgramDirectorState}, ticked from {@code
 * ProgramDirectorState.tick}, with its own NBT round-trip.
 *
 * <p><b>This is the doc's §10 build-order step 2 first cut, not the full
 * tree.</b> It proves the whole loop end to end with a single hardcoded
 * node ({@link #NODE_ID}) rather than the doc's full data-driven node table
 * (§4: id/prerequisites/unlockTarget/stepCost/totalSteps/counterTags/
 * basePriority): a building rises → feeds off the ledger → the meter climbs
 * → damaging the building pauses it (§5, {@link
 * PsionicResearchBlockEntity#isUnderAttack}) → destroying it costs half the
 * progress ({@link #onBuildingDestroyed}) → the node completes and flips
 * {@link NodeStatus#UNLOCKED}. Deliberately <b>not yet wired</b> to
 * anything: {@code tier2Unlocked()}/{@code tier3Unlocked()} still read raw
 * counters exactly as before (doc §7's rewiring is a separate, riskier
 * change against code this pass didn't need to touch), the adaptive-focus
 * scoring pass (§4) doesn't exist yet (nothing to score with only one
 * node), and the building is placed directly rather than grown
 * incrementally through {@code ConstructionSite} (doc §5's "watch it
 * rise" is cosmetic, not required to prove the architecture). All three are
 * the next steps in the doc's own build order (§10 items 3-7), left for a
 * follow-up pass — see BACKLOG.md.
 */
public final class ResearchTree {
	/** The one node this pass models — the doc's own suggested first node (§3 step 2 example). */
	private static final String NODE_ID = "tier2_hardware";
	/** Mirrors {@code ConstructionSite.BUILD_STEP_COST}'s shape — a modest, steady ledger draw per step. */
	private static final Map<String, Integer> STEP_COST = Map.of(Resources.IRON, 1);
	/** How many successful feed steps complete the node — the "how long" knob (doc §4 totalSteps), a first guess pending playtesting. */
	private static final int TOTAL_STEPS = 20;
	/** Ticks between feed-step attempts. */
	private static final int FEED_INTERVAL = 100;
	/** Search radius band (blocks) around a core site for a building placement site. */
	private static final int SITE_MIN_DISTANCE = 12;
	private static final int SITE_MAX_DISTANCE = 28;

	/** The doc's §3 node lifecycle: unmet prerequisites → researchable → the single one actually accruing progress → done. */
	public enum NodeStatus {
		LOCKED, AVAILABLE, FOCUSED, UNLOCKED
	}

	private NodeStatus nodeStatus = NodeStatus.LOCKED;
	private int focusProgress;
	private int feedCooldown;
	@Nullable
	private BlockPos buildingPos;

	/**
	 * Ticked every tick from the Director. Advances the node status, tries to
	 * place the building once a core site exists, and — while a healthy
	 * building stands — attempts a feed step on its own cadence. Returns
	 * whether persistent state changed.
	 */
	public boolean tick(ServerWorld world, ProgramDirectorState director) {
		boolean changed = false;
		if (this.nodeStatus == NodeStatus.LOCKED) {
			// v1: no prerequisite counters modeled yet (doc §7's tier-gate
			// rewiring is a later pass) — the single node is simply available
			// from the start.
			this.nodeStatus = NodeStatus.AVAILABLE;
			changed = true;
		}

		if (this.buildingPos == null) {
			return this.tryPlaceBuilding(world, director) || changed;
		}

		if (!(world.getBlockEntity(this.buildingPos) instanceof PsionicResearchBlockEntity building)) {
			// The block entity is gone without onStateReplaced reporting it
			// (chunk unloaded mid-break, or some other edge case) — treat the
			// building as lost rather than silently never feeding again.
			this.buildingPos = null;
			return true;
		}

		if (this.nodeStatus == NodeStatus.AVAILABLE) {
			// v1: no adaptive-focus scoring pass (doc §4) — with only one
			// node there's nothing to bid against, so it's simply focused the
			// moment the building exists.
			this.nodeStatus = NodeStatus.FOCUSED;
			changed = true;
		}
		if (this.nodeStatus != NodeStatus.FOCUSED) {
			return changed;
		}

		if (building.isUnderAttack(world.getTime())) {
			// Doc §5: "feed steps simply don't attempt while it reads as
			// under attack — research pauses, doesn't reverse."
			return changed;
		}

		if (this.feedCooldown > 0) {
			this.feedCooldown--;
			return changed;
		}
		this.feedCooldown = FEED_INTERVAL;
		if (director.tryConsume(STEP_COST)) {
			this.focusProgress++;
			changed = true;
			if (this.focusProgress >= TOTAL_STEPS) {
				this.nodeStatus = NodeStatus.UNLOCKED;
				Program7.LOGGER.info("[Program 7] Research node '{}' completed", NODE_ID);
			}
		}
		return changed;
	}

	/**
	 * Places the building directly at a site near a random core, once one
	 * exists. A v1 simplification of the doc's "grown incrementally via
	 * ConstructionSite" (§5) — the building simply appears rather than
	 * rising block by block; the meter and disruption model around it are
	 * otherwise the real thing.
	 */
	private boolean tryPlaceBuilding(ServerWorld world, ProgramDirectorState director) {
		List<BlockPos> coreSites = director.getCoreSites();
		if (coreSites.isEmpty()) {
			return false;
		}
		BlockPos anchor = coreSites.get(world.getRandom().nextInt(coreSites.size()));
		BlockPos site = pickBuildingSite(world, anchor);
		if (site == null) {
			return false;
		}
		world.setBlockState(site, P7Blocks.PSIONIC_RESEARCH.get().getDefaultState());
		this.buildingPos = site;
		Program7.LOGGER.info("[Program 7] Psionic research building raised at {}", site.toShortString());
		return true;
	}

	private static BlockPos pickBuildingSite(ServerWorld world, BlockPos anchor) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			double distance = SITE_MIN_DISTANCE + world.getRandom().nextDouble() * (SITE_MAX_DISTANCE - SITE_MIN_DISTANCE);
			int x = anchor.getX() + (int) (Math.cos(angle) * distance);
			int z = anchor.getZ() + (int) (Math.sin(angle) * distance);

			world.getChunk(new BlockPos(x, 64, z)); // load so the heightmap is real
			int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			BlockPos site = new BlockPos(x, y, z);
			if (world.getFluidState(site.down()).isEmpty() && world.getBlockState(site).isAir()) {
				return site;
			}
		}
		return null;
	}

	/**
	 * Reported by {@link dev.rheava.program7.block.PsionicResearchBlock}'s
	 * {@code onStateReplaced} once the building is actually gone. Doc §5:
	 * "halves focusProgress on whatever node was mid-flight (a real cost,
	 * not total annihilation)... the Program must queue a fresh construction
	 * site before research resumes at all."
	 */
	public void onBuildingDestroyed(ServerWorld world, BlockPos pos) {
		if (!pos.equals(this.buildingPos)) {
			return;
		}
		this.buildingPos = null;
		this.focusProgress /= 2;
		if (this.nodeStatus == NodeStatus.FOCUSED) {
			this.nodeStatus = NodeStatus.AVAILABLE;
		}
		Program7.LOGGER.info("[Program 7] Psionic research building destroyed at {} — research stalls, progress halved",
				pos.toShortString());
	}

	/** Whether {@code nodeId} has completed. Only {@link #NODE_ID} exists in this pass. */
	public boolean isUnlocked(String nodeId) {
		return NODE_ID.equals(nodeId) && this.nodeStatus == NodeStatus.UNLOCKED;
	}

	@Nullable
	public BlockPos getBuildingPos() {
		return this.buildingPos;
	}

	public NodeStatus getNodeStatus() {
		return this.nodeStatus;
	}

	public int getFocusProgress() {
		return this.focusProgress;
	}

	public int getTotalSteps() {
		return TOTAL_STEPS;
	}

	/** The focused node's id, or {@code ""} if nothing is currently focused — matches the shape {@code DatapadSnapshotPayload} would read (doc §8), not yet wired there. */
	public String getFocusedNodeId() {
		return this.nodeStatus == NodeStatus.FOCUSED ? NODE_ID : "";
	}

	// ---- Persistence ---------------------------------------------------------------------------

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();
		tag.putString("NodeStatus", this.nodeStatus.name());
		tag.putInt("FocusProgress", this.focusProgress);
		tag.putInt("FeedCooldown", this.feedCooldown);
		if (this.buildingPos != null) {
			tag.putIntArray("BuildingPos",
					new int[] {this.buildingPos.getX(), this.buildingPos.getY(), this.buildingPos.getZ()});
		}
		return tag;
	}

	public void readNbt(NbtCompound nbt) {
		this.nodeStatus = NodeStatus.LOCKED;
		if (nbt.contains("NodeStatus")) {
			try {
				this.nodeStatus = NodeStatus.valueOf(nbt.getString("NodeStatus"));
			} catch (IllegalArgumentException ignored) {
				// Corrupted/unknown value — fall back to LOCKED rather than crash the load.
			}
		}
		this.focusProgress = nbt.getInt("FocusProgress");
		this.feedCooldown = nbt.getInt("FeedCooldown");
		this.buildingPos = null;
		if (nbt.contains("BuildingPos")) {
			int[] pos = nbt.getIntArray("BuildingPos");
			if (pos.length == 3) {
				this.buildingPos = new BlockPos(pos[0], pos[1], pos[2]);
			}
		}
	}
}
