package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One observed loot reward assembled from nearby callbacks for one boss and profile. */
public final class LootRewardGroup
{
	private final BossDefinition boss;
	private final List<SavedLootEvent> events;

	LootRewardGroup(BossDefinition boss, List<SavedLootEvent> events)
	{
		this.boss = boss;
		this.events = Collections.unmodifiableList(new ArrayList<>(events));
	}

	public BossDefinition getBoss() { return boss; }
	public List<SavedLootEvent> getEvents() { return events; }
	public SavedLootEvent getLastEvent() { return events.get(events.size() - 1); }
	public Instant getObservedAtUtc() { return getLastEvent().getObservedAtUtc(); }
}
