package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
	public String getGroupId() { return events.get(0).getEventId(); }
	public SavedLootEvent getLastEvent() { return events.get(events.size() - 1); }
	public Instant getObservedAtUtc() { return getLastEvent().getObservedAtUtc(); }

	/** Sum distinct callbacks; RuneLite supplies no stable callback ID. */
	public Long getConvertedGp()
	{
		long total = 0;
		Set<String> seen = new HashSet<>();
		try
		{
			for (SavedLootEvent event : events)
			{
				Long gp = event.getConvertedGp();
				if (gp == null)
				{
					return null;
				}
				String signature = event.getClientTick() + ":" + event.getItems().stream()
					.map(item -> item.getItemId() + "x" + item.getQuantity())
					.sorted().collect(Collectors.joining(","));
				if (seen.add(signature))
				{
					total = Math.addExact(total, gp);
				}
			}
			return total;
		}
		catch (ArithmeticException overflow)
		{
			return null;
		}
	}
}
