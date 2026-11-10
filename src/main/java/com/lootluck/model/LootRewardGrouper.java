package com.lootluck.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Groups callbacks within a boss-specific window anchored to the first callback. */
public final class LootRewardGrouper
{
	private LootRewardGrouper()
	{
	}

	public static List<LootRewardGroup> group(List<SavedLootEvent> events)
	{
		List<SavedLootEvent> sorted = new ArrayList<>(events);
		sorted.sort(Comparator.comparing(SavedLootEvent::getObservedAtUtc));
		Map<String, WorkingGroup> active = new HashMap<>();
		List<WorkingGroup> all = new ArrayList<>();
		for (SavedLootEvent event : sorted)
		{
			BossDefinition boss = BossRegistry.findBySource(event.getSource());
			if (boss == null)
			{
				continue;
			}
			String key = event.getProfileId() + "|" + boss.getId() + "|" + event.getSessionId();
			WorkingGroup group = active.get(key);
			if (group == null || !group.accepts(event, boss))
			{
				group = new WorkingGroup(boss, event);
				active.put(key, group);
				all.add(group);
			}
			else
			{
				group.events.add(event);
				if (event.getRewardClick() != null)
				{
					group.clickId = event.getRewardClick().getClickId();
				}
			}
		}
		List<LootRewardGroup> result = new ArrayList<>();
		for (WorkingGroup group : all)
		{
			result.add(new LootRewardGroup(group.boss, group.events));
		}
		result.sort(Comparator.comparing(LootRewardGroup::getObservedAtUtc));
		return result;
	}

	private static final class WorkingGroup
	{
		private final BossDefinition boss;
		private final Instant firstAtUtc;
		private final List<SavedLootEvent> events = new ArrayList<>();
		private String clickId;

		private WorkingGroup(BossDefinition boss, SavedLootEvent first)
		{
			this.boss = boss;
			this.firstAtUtc = first.getObservedAtUtc();
			this.events.add(first);
			this.clickId = first.getRewardClick() == null ? null : first.getRewardClick().getClickId();
		}

		private boolean accepts(SavedLootEvent next, BossDefinition definition)
		{
			Duration elapsed = Duration.between(firstAtUtc, next.getObservedAtUtc());
			if (elapsed.isNegative() || elapsed.compareTo(definition.getGroupingWindow()) > 0)
			{
				return false;
			}
			String nextClickId = next.getRewardClick() == null ? null : next.getRewardClick().getClickId();
			return clickId == null || nextClickId == null || clickId.equals(nextClickId);
		}
	}
}
