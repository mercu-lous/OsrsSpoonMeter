package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Counts observed reward clicks once, even when a click produces multiple loot callbacks. */
public final class RewardStatistics
{
	private final int observedNormalRewards;
	private final int observedEggRewards;
	private final int valuedNormalRewards;
	private final long convertedGp;
	private final long expectedGp;
	private final Long netLuckGp;
	private final Long lastRewardDeltaGp;

	private RewardStatistics(int observedNormalRewards, int observedEggRewards,
		int valuedNormalRewards, long convertedGp, long expectedGp,
		Long netLuckGp, Long lastRewardDeltaGp)
	{
		this.observedNormalRewards = observedNormalRewards;
		this.observedEggRewards = observedEggRewards;
		this.valuedNormalRewards = valuedNormalRewards;
		this.convertedGp = convertedGp;
		this.expectedGp = expectedGp;
		this.netLuckGp = netLuckGp;
		this.lastRewardDeltaGp = lastRewardDeltaGp;
	}

	public static RewardStatistics summarize(List<SavedLootEvent> events)
	{
		return summarize(events, event -> true);
	}

	/** Includes whole reward groups using the time and session of their final callback. */
	public static RewardStatistics summarize(List<SavedLootEvent> events,
		Predicate<SavedLootEvent> includeReward)
	{
		return summarize(events, includeReward, Collections.emptySet());
	}

	public static RewardStatistics summarize(List<SavedLootEvent> events,
		Predicate<SavedLootEvent> includeReward, Set<String> confirmedNoLootClickIds)
	{
		Map<String, List<SavedLootEvent>> groups = new LinkedHashMap<>();
		for (SavedLootEvent event : events)
		{
			RewardClickCandidate click = event.getRewardClick();
			if (click != null)
			{
				groups.computeIfAbsent(click.getClickId(), ignored -> new ArrayList<>()).add(event);
			}
		}
		int normal = 0;
		int eggs = 0;
		int valued = 0;
		long actualTotal = 0;
		long expectedTotal = 0;
		Long lastDelta = null;
		Instant latestNormal = null;
		for (List<SavedLootEvent> group : groups.values())
		{
			group.sort(Comparator.comparing(SavedLootEvent::getObservedAtUtc));
			if (!includeReward.test(group.get(group.size() - 1)))
			{
				continue;
			}
			String method = group.get(0).getRewardClick().getMethod();
			if ("TAKE_EGGS".equals(method))
			{
				if (!confirmedNoLootClickIds.contains(group.get(0).getRewardClick().getClickId()))
				{
					eggs++;
				}
				continue;
			}
			if (!"OPEN_STOMACH".equals(method))
			{
				continue;
			}
			normal++;
			Instant groupTime = group.get(group.size() - 1).getObservedAtUtc();
			boolean newest = latestNormal == null || groupTime.isAfter(latestNormal);
			if (newest)
			{
				latestNormal = groupTime;
				lastDelta = null;
			}
			Long groupActual = sumDistinctCallbacks(group);
			ExpectedValueSnapshot firstExpected = group.get(0).getExpectedSnapshot();
			Long groupExpected = firstExpected == null ? null : firstExpected.getExpectedGp();
			if (groupActual == null || groupExpected == null || !hasConsistentExpectedValue(group, firstExpected))
			{
				continue;
			}
			try
			{
				long delta = Math.subtractExact(groupActual, groupExpected);
				actualTotal = Math.addExact(actualTotal, groupActual);
				expectedTotal = Math.addExact(expectedTotal, groupExpected);
				valued++;
				if (newest)
				{
					lastDelta = delta;
				}
			}
			catch (ArithmeticException overflow)
			{
				return new RewardStatistics(normal, eggs, valued, 0, 0, null, null);
			}
		}
		Long net = null;
		if (valued > 0)
		{
			try
			{
				net = Math.subtractExact(actualTotal, expectedTotal);
			}
			catch (ArithmeticException overflow)
			{
				return new RewardStatistics(normal, eggs, valued, actualTotal, expectedTotal, null, null);
			}
		}
		return new RewardStatistics(normal, eggs, valued, actualTotal, expectedTotal, net, lastDelta);
	}

	private static boolean hasConsistentExpectedValue(List<SavedLootEvent> group, ExpectedValueSnapshot first)
	{
		for (SavedLootEvent event : group)
		{
			ExpectedValueSnapshot other = event.getExpectedSnapshot();
			if (other == null || other.getExpectedGp() == null
				|| !first.getExpectedGp().equals(other.getExpectedGp())
				|| !first.getSourceVersion().equals(other.getSourceVersion())
				|| !first.getPriceSource().equals(other.getPriceSource()))
			{
				return false;
			}
		}
		return true;
	}

	private static Long sumDistinctCallbacks(List<SavedLootEvent> group)
	{
		long total = 0;
		Set<String> seen = new HashSet<>();
		try
		{
			for (SavedLootEvent event : group)
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

	public int getObservedNormalRewards() { return observedNormalRewards; }
	public int getObservedEggRewards() { return observedEggRewards; }
	public int getValuedNormalRewards() { return valuedNormalRewards; }
	public long getConvertedGp() { return convertedGp; }
	public long getExpectedGp() { return expectedGp; }
	public Long getNetLuckGp() { return netLuckGp; }
	public Long getLastRewardDeltaGp() { return lastRewardDeltaGp; }
}
