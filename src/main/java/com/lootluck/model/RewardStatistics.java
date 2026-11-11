package com.lootluck.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Counts observed loot groups; a click is optional evidence for reward type. */
public final class RewardStatistics
{
	private final int observedNormalRewards;
	private final int observedEggRewards;
	private final int partialGauntletRewards;
	private final int unclassifiedGauntletRewards;
	private final int pricedNormalRewards;
	private final Long observedConvertedGp;
	private final int valuedNormalRewards;
	private final long convertedGp;
	private final long expectedGp;
	private final Long netLuckGp;
	private final Long lastRewardDeltaGp;

	private RewardStatistics(int observedNormalRewards, int observedEggRewards,
		int partialGauntletRewards, int unclassifiedGauntletRewards,
		int pricedNormalRewards, Long observedConvertedGp, int valuedNormalRewards,
		long convertedGp, long expectedGp,
		Long netLuckGp, Long lastRewardDeltaGp)
	{
		this.observedNormalRewards = observedNormalRewards;
		this.observedEggRewards = observedEggRewards;
		this.partialGauntletRewards = partialGauntletRewards;
		this.unclassifiedGauntletRewards = unclassifiedGauntletRewards;
		this.pricedNormalRewards = pricedNormalRewards;
		this.observedConvertedGp = observedConvertedGp;
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
		int normal = 0;
		int eggs = 0;
		int partialGauntlet = 0;
		int unclassifiedGauntlet = 0;
		int priced = 0;
		Long pricedTotal = 0L;
		int valued = 0;
		long actualTotal = 0;
		long expectedTotal = 0;
		Long lastDelta = null;
		Instant latestNormal = null;
		for (LootRewardGroup reward : LootRewardGrouper.group(events))
		{
			List<SavedLootEvent> group = reward.getEvents();
			if (!includeReward.test(reward.getLastEvent()))
			{
				continue;
			}
			RewardClickCandidate click = null;
			for (SavedLootEvent callback : group)
			{
				if (callback.getRewardClick() != null)
				{
					click = callback.getRewardClick();
					break;
				}
			}
			String method = click == null ? "UNKNOWN" : click.getMethod();
			if ("TAKE_EGGS".equals(method))
			{
				if (!confirmedNoLootClickIds.contains(click.getClickId()))
				{
					eggs++;
				}
				continue;
			}
			normal++;
			Instant groupTime = reward.getObservedAtUtc();
			boolean newest = latestNormal == null || groupTime.isAfter(latestNormal);
			if (newest)
			{
				latestNormal = groupTime;
				lastDelta = null;
			}
			if (GauntletOutcome.isGauntlet(BossRegistry.findBySource(group.get(0).getSource())))
			{
				if (group.stream().anyMatch(event -> event.getGauntletOutcome() == GauntletOutcome.PARTIAL))
				{
					partialGauntlet++;
					continue;
				}
				if (group.stream().anyMatch(event -> event.getGauntletOutcome() != GauntletOutcome.COMPLETED))
				{
					unclassifiedGauntlet++;
					continue;
				}
			}
			Long groupActual = sumDistinctCallbacks(group);
			if (groupActual != null)
			{
				priced++;
				if (pricedTotal != null)
				{
					try
					{
						pricedTotal = Math.addExact(pricedTotal, groupActual);
					}
					catch (ArithmeticException overflow)
					{
						pricedTotal = null;
					}
				}
			}
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
				return new RewardStatistics(normal, eggs, partialGauntlet, unclassifiedGauntlet,
					priced, pricedTotal,
					valued, 0, 0, null, null);
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
				return new RewardStatistics(normal, eggs, partialGauntlet, unclassifiedGauntlet,
					priced, pricedTotal,
					valued, actualTotal, expectedTotal, null, null);
			}
		}
		return new RewardStatistics(normal, eggs, partialGauntlet, unclassifiedGauntlet,
			priced, pricedTotal,
			valued, actualTotal, expectedTotal, net, lastDelta);
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
	public int getObservedLootRewards() { return observedNormalRewards + observedEggRewards; }
	public int getObservedEggRewards() { return observedEggRewards; }
	public int getPartialGauntletRewards() { return partialGauntletRewards; }
	public int getUnclassifiedGauntletRewards() { return unclassifiedGauntletRewards; }
	public int getPricedNormalRewards() { return pricedNormalRewards; }
	public Long getObservedConvertedGp() { return observedConvertedGp; }
	public Long getAverageGpPerPricedReward()
	{
		return pricedNormalRewards == 0 || observedConvertedGp == null
			? null : observedConvertedGp / pricedNormalRewards;
	}
	public int getValuedNormalRewards() { return valuedNormalRewards; }
	public long getConvertedGp() { return convertedGp; }
	public long getExpectedGp() { return expectedGp; }
	public Long getNetLuckGp() { return netLuckGp; }
	public BigDecimal getPercentVsExpected()
	{
		return netLuckGp == null || expectedGp == 0 ? null
			: BigDecimal.valueOf(netLuckGp).multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(expectedGp), 1, RoundingMode.HALF_UP);
	}
	public Long getLastRewardDeltaGp() { return lastRewardDeltaGp; }
}
