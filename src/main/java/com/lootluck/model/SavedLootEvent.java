package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** An observed loot event, independent of kill or reward attribution. */
public final class SavedLootEvent
{
	private final String eventId;
	private final String profileId;
	private final String sessionId;
	private final Instant observedAtUtc;
	private final String source;
	private final String eventType;
	private final int combatLevel;
	private final int reportedAmount;
	private final int clientTick;
	private final List<LootItemPreview> items;
	private final RewardClickCandidate rewardClick;
	private final ExpectedValueSnapshot expectedSnapshot;
	private final GauntletOutcome gauntletOutcome;

	public SavedLootEvent(String eventId, String profileId, String sessionId, Instant observedAtUtc,
		String source, String eventType, int combatLevel, int reportedAmount, int clientTick,
		List<LootItemPreview> items)
	{
		this(eventId, profileId, sessionId, observedAtUtc, source, eventType, combatLevel,
			reportedAmount, clientTick, items, null);
	}

	public SavedLootEvent(String eventId, String profileId, String sessionId, Instant observedAtUtc,
		String source, String eventType, int combatLevel, int reportedAmount, int clientTick,
		List<LootItemPreview> items, RewardClickCandidate rewardClick)
	{
		this(eventId, profileId, sessionId, observedAtUtc, source, eventType, combatLevel,
			reportedAmount, clientTick, items, rewardClick, null);
	}

	public SavedLootEvent(String eventId, String profileId, String sessionId, Instant observedAtUtc,
		String source, String eventType, int combatLevel, int reportedAmount, int clientTick,
		List<LootItemPreview> items, RewardClickCandidate rewardClick,
		ExpectedValueSnapshot expectedSnapshot)
	{
		this(eventId, profileId, sessionId, observedAtUtc, source, eventType, combatLevel,
			reportedAmount, clientTick, items, rewardClick, expectedSnapshot,
			GauntletOutcome.fromLegacy(source, items));
	}

	public SavedLootEvent(String eventId, String profileId, String sessionId, Instant observedAtUtc,
		String source, String eventType, int combatLevel, int reportedAmount, int clientTick,
		List<LootItemPreview> items, RewardClickCandidate rewardClick,
		ExpectedValueSnapshot expectedSnapshot, GauntletOutcome gauntletOutcome)
	{
		this.eventId = Objects.requireNonNull(eventId);
		this.profileId = Objects.requireNonNull(profileId);
		this.sessionId = Objects.requireNonNull(sessionId);
		this.observedAtUtc = Objects.requireNonNull(observedAtUtc);
		this.source = Objects.requireNonNull(source);
		this.eventType = Objects.requireNonNull(eventType);
		this.combatLevel = combatLevel;
		this.reportedAmount = reportedAmount;
		this.clientTick = clientTick;
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
		if (rewardClick != null && !rewardClick.matches(profileId, clientTick))
		{
			throw new IllegalArgumentException("Reward click is not a recent click on this profile");
		}
		this.rewardClick = rewardClick;
		this.expectedSnapshot = expectedSnapshot;
		this.gauntletOutcome = Objects.requireNonNull(gauntletOutcome);
		boolean gauntlet = GauntletOutcome.isGauntlet(BossRegistry.findBySource(source));
		if ((gauntlet && gauntletOutcome == GauntletOutcome.NOT_APPLICABLE)
			|| (!gauntlet && gauntletOutcome != GauntletOutcome.NOT_APPLICABLE))
		{
			throw new IllegalArgumentException("Gauntlet outcome does not match loot source");
		}
	}

	public String getEventId() { return eventId; }
	public String getProfileId() { return profileId; }
	public String getSessionId() { return sessionId; }
	public Instant getObservedAtUtc() { return observedAtUtc; }
	public String getSource() { return source; }
	public String getEventType() { return eventType; }
	public int getCombatLevel() { return combatLevel; }
	public int getReportedAmount() { return reportedAmount; }
	public int getClientTick() { return clientTick; }
	public List<LootItemPreview> getItems() { return items; }
	public RewardClickCandidate getRewardClick() { return rewardClick; }
	public ExpectedValueSnapshot getExpectedSnapshot() { return expectedSnapshot; }
	public GauntletOutcome getGauntletOutcome() { return gauntletOutcome; }
	public Long getActualGp() { return LootValuation.totalGpIfComplete(items); }
	public Long getConvertedGp() { return LootValuation.totalConvertedGpIfComplete(items); }
}
