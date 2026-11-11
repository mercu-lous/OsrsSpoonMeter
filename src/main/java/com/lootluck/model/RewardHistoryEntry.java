package com.lootluck.model;

import java.time.Instant;
import java.util.Objects;

/** One saved observation shown in History; scope time keeps click callbacks together. */
public final class RewardHistoryEntry
{
	private final SavedLootEvent lootEvent;
	private final NoLootConfirmation noLootConfirmation;
	private final ManualReward manualReward;
	private final Instant scopeTimeUtc;

	private RewardHistoryEntry(SavedLootEvent lootEvent,
		NoLootConfirmation noLootConfirmation, ManualReward manualReward, Instant scopeTimeUtc)
	{
		this.lootEvent = lootEvent;
		this.noLootConfirmation = noLootConfirmation;
		this.manualReward = manualReward;
		this.scopeTimeUtc = Objects.requireNonNull(scopeTimeUtc);
	}

	public static RewardHistoryEntry loot(SavedLootEvent event, Instant scopeTimeUtc)
	{
		return new RewardHistoryEntry(Objects.requireNonNull(event), null, null, scopeTimeUtc);
	}

	public static RewardHistoryEntry noLoot(NoLootConfirmation confirmation, Instant scopeTimeUtc)
	{
		return new RewardHistoryEntry(null, Objects.requireNonNull(confirmation), null, scopeTimeUtc);
	}

	public static RewardHistoryEntry manual(ManualReward reward)
	{
		return new RewardHistoryEntry(null, null, Objects.requireNonNull(reward), reward.getRecordedAtUtc());
	}

	public boolean isNoLootConfirmation() { return noLootConfirmation != null; }
	public boolean isManualReward() { return manualReward != null; }
	public SavedLootEvent getLootEvent() { return lootEvent; }
	public NoLootConfirmation getNoLootConfirmation() { return noLootConfirmation; }
	public ManualReward getManualReward() { return manualReward; }
	public Instant getObservedAtUtc()
	{
		return lootEvent != null ? lootEvent.getObservedAtUtc()
			: noLootConfirmation != null ? noLootConfirmation.getObservedAtUtc() : manualReward.getRecordedAtUtc();
	}
	public Instant getScopeTimeUtc() { return scopeTimeUtc; }
	public String getSessionId()
	{
		return lootEvent != null ? lootEvent.getSessionId()
			: noLootConfirmation != null ? noLootConfirmation.getSessionId() : manualReward.getSessionId();
	}
}
