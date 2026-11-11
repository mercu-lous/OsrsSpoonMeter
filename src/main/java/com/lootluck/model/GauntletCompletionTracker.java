package com.lootluck.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Matches a rising game completion counter to the next chest loot callback. */
public final class GauntletCompletionTracker
{
	private static final Duration GROUP_WINDOW = Duration.ofSeconds(5);
	private String profileId;
	private Integer regularCount;
	private Integer corruptedCount;
	private boolean pendingRegular;
	private boolean pendingCorrupted;
	private BossDefinition lastBoss;
	private GauntletOutcome lastOutcome;
	private Instant lastOutcomeAt;

	public void reset()
	{
		profileId = null;
		regularCount = null;
		corruptedCount = null;
		pendingRegular = false;
		pendingCorrupted = false;
		lastBoss = null;
		lastOutcome = null;
		lastOutcomeAt = null;
	}

	public void observe(String activeProfileId, int regular, int corrupted)
	{
		Objects.requireNonNull(activeProfileId);
		if (!activeProfileId.equals(profileId) || regularCount == null || corruptedCount == null
			|| regular < regularCount || corrupted < corruptedCount)
		{
			reset();
			profileId = activeProfileId;
			regularCount = regular;
			corruptedCount = corrupted;
			return;
		}
		boolean newCorrupted = corrupted > corruptedCount;
		if (newCorrupted)
		{
			pendingCorrupted = true;
			pendingRegular = false; // A corrupted completion may also advance the regular counter.
		}
		else if (regular > regularCount)
		{
			pendingRegular = true;
		}
		if (newCorrupted || regular > regularCount)
		{
			lastBoss = null;
			lastOutcome = null;
			lastOutcomeAt = null;
		}
		regularCount = regular;
		corruptedCount = corrupted;
	}

	public GauntletOutcome classify(String activeProfileId, BossDefinition boss,
		List<LootItemPreview> items, Instant observedAtUtc)
	{
		if (!GauntletOutcome.isGauntlet(boss))
		{
			return GauntletOutcome.NOT_APPLICABLE;
		}
		GauntletOutcome outcome;
		if (GauntletOutcome.isKnownPartialReward(items))
		{
			outcome = GauntletOutcome.PARTIAL;
			if (boss == BossRegistry.CORRUPTED_GAUNTLET)
			{
				pendingCorrupted = false;
			}
			else
			{
				pendingRegular = false;
			}
		}
		else if (activeProfileId.equals(profileId)
			&& boss == lastBoss && lastOutcomeAt != null
			&& !observedAtUtc.isBefore(lastOutcomeAt)
			&& Duration.between(lastOutcomeAt, observedAtUtc).compareTo(GROUP_WINDOW) <= 0)
		{
			return lastOutcome;
		}
		else if (activeProfileId.equals(profileId) && boss == BossRegistry.CORRUPTED_GAUNTLET
			&& pendingCorrupted)
		{
			outcome = GauntletOutcome.COMPLETED;
			pendingCorrupted = false;
		}
		else if (activeProfileId.equals(profileId) && boss == BossRegistry.GAUNTLET && pendingRegular)
		{
			outcome = GauntletOutcome.COMPLETED;
			pendingRegular = false;
		}
		else
		{
			outcome = GauntletOutcome.UNKNOWN;
		}
		lastBoss = boss;
		lastOutcome = outcome;
		lastOutcomeAt = observedAtUtc;
		return outcome;
	}
}
