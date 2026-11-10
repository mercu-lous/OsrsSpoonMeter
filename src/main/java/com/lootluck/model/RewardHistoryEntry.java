package com.lootluck.model;

import java.time.Instant;
import java.util.Objects;

/** One saved observation shown in History; scope time keeps click callbacks together. */
public final class RewardHistoryEntry
{
	private final SavedLootEvent lootEvent;
	private final NoLootConfirmation noLootConfirmation;
	private final Instant scopeTimeUtc;

	private RewardHistoryEntry(SavedLootEvent lootEvent,
		NoLootConfirmation noLootConfirmation, Instant scopeTimeUtc)
	{
		this.lootEvent = lootEvent;
		this.noLootConfirmation = noLootConfirmation;
		this.scopeTimeUtc = Objects.requireNonNull(scopeTimeUtc);
	}

	public static RewardHistoryEntry loot(SavedLootEvent event, Instant scopeTimeUtc)
	{
		return new RewardHistoryEntry(Objects.requireNonNull(event), null, scopeTimeUtc);
	}

	public static RewardHistoryEntry noLoot(NoLootConfirmation confirmation, Instant scopeTimeUtc)
	{
		return new RewardHistoryEntry(null, Objects.requireNonNull(confirmation), scopeTimeUtc);
	}

	public boolean isNoLootConfirmation() { return noLootConfirmation != null; }
	public SavedLootEvent getLootEvent() { return lootEvent; }
	public NoLootConfirmation getNoLootConfirmation() { return noLootConfirmation; }
	public Instant getObservedAtUtc()
	{
		return lootEvent != null ? lootEvent.getObservedAtUtc() : noLootConfirmation.getObservedAtUtc();
	}
	public Instant getScopeTimeUtc() { return scopeTimeUtc; }
	public String getSessionId()
	{
		return lootEvent != null ? lootEvent.getSessionId() : noLootConfirmation.getSessionId();
	}
}
