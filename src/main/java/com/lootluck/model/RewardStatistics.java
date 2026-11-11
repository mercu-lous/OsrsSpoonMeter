package com.lootluck.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Counts observed loot groups; a click is optional evidence for reward type. */
public final class RewardStatistics
{
	private final int observedNormalRewards;
	private final int observedEggRewards;
	private final int excludedRewards;
	private final int correctedRewards;
	private final Long appliedCorrectionGp;
	private final int partialGauntletRewards;
	private final int unclassifiedGauntletRewards;
	private final int pricedNormalRewards;
	private final Long observedConvertedGp;
	private final int manualRewardCount;
	private final long manualKillCount;
	private final int manualComparableRewardCount;
	private final Long manualActualGp;
	private final int valuedNormalRewards;
	private final long convertedGp;
	private final long expectedGp;
	private final Long netLuckGp;
	private final Long lastRewardDeltaGp;
	private final int importedKills;
	private final long importedActualGp;

	private RewardStatistics(int observedNormalRewards, int observedEggRewards,
		int excludedRewards, int correctedRewards, Long appliedCorrectionGp,
		int partialGauntletRewards, int unclassifiedGauntletRewards,
		int pricedNormalRewards, Long observedConvertedGp,
		int manualRewardCount, long manualKillCount,
		int manualComparableRewardCount, Long manualActualGp,
		int valuedNormalRewards,
		long convertedGp, long expectedGp,
		Long netLuckGp, Long lastRewardDeltaGp)
	{
		this.observedNormalRewards = observedNormalRewards;
		this.observedEggRewards = observedEggRewards;
		this.excludedRewards = excludedRewards;
		this.correctedRewards = correctedRewards;
		this.appliedCorrectionGp = appliedCorrectionGp;
		this.partialGauntletRewards = partialGauntletRewards;
		this.unclassifiedGauntletRewards = unclassifiedGauntletRewards;
		this.pricedNormalRewards = pricedNormalRewards;
		this.observedConvertedGp = observedConvertedGp;
		this.manualRewardCount = manualRewardCount;
		this.manualKillCount = manualKillCount;
		this.manualComparableRewardCount = manualComparableRewardCount;
		this.manualActualGp = manualActualGp;
		this.valuedNormalRewards = valuedNormalRewards;
		this.convertedGp = convertedGp;
		this.expectedGp = expectedGp;
		this.netLuckGp = netLuckGp;
		this.lastRewardDeltaGp = lastRewardDeltaGp;
		this.importedKills = 0;
		this.importedActualGp = 0;
	}

	private RewardStatistics(RewardStatistics recent, ImportedBaseline baseline)
	{
		this.observedNormalRewards = recent.observedNormalRewards;
		this.observedEggRewards = recent.observedEggRewards;
		this.excludedRewards = recent.excludedRewards;
		this.correctedRewards = recent.correctedRewards;
		this.appliedCorrectionGp = recent.appliedCorrectionGp;
		this.partialGauntletRewards = recent.partialGauntletRewards;
		this.unclassifiedGauntletRewards = recent.unclassifiedGauntletRewards;
		this.pricedNormalRewards = recent.pricedNormalRewards;
		this.observedConvertedGp = recent.observedConvertedGp;
		this.manualRewardCount = recent.manualRewardCount;
		this.manualKillCount = recent.manualKillCount;
		this.manualComparableRewardCount = recent.manualComparableRewardCount;
		this.manualActualGp = recent.manualActualGp;
		this.valuedNormalRewards = recent.valuedNormalRewards;
		this.convertedGp = Math.addExact(recent.convertedGp, baseline.getActualGp());
		this.expectedGp = Math.addExact(recent.expectedGp, baseline.getExpectedGp());
		this.netLuckGp = Math.subtractExact(convertedGp, expectedGp);
		this.lastRewardDeltaGp = recent.lastRewardDeltaGp;
		this.importedKills = baseline.getKills();
		this.importedActualGp = baseline.getActualGp();
	}

	public RewardStatistics withImportedBaseline(ImportedBaseline baseline)
	{
		return baseline == null ? this : new RewardStatistics(this, baseline);
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
		return summarize(events, includeReward, confirmedNoLootClickIds, RewardEdits.empty());
	}

	public static RewardStatistics summarize(List<SavedLootEvent> events,
		Predicate<SavedLootEvent> includeReward, Set<String> confirmedNoLootClickIds,
		RewardEdits edits)
	{
		return summarize(events, includeReward, confirmedNoLootClickIds, edits,
			Collections.emptyList(), reward -> true);
	}

	public static RewardStatistics summarize(List<SavedLootEvent> events,
		Predicate<SavedLootEvent> includeReward, Set<String> confirmedNoLootClickIds,
		RewardEdits edits, List<ManualReward> manualRewards,
		Predicate<ManualReward> includeManual)
	{
		int normal = 0;
		int eggs = 0;
		int excluded = 0;
		int corrected = 0;
		Long corrections = 0L;
		int partialGauntlet = 0;
		int unclassifiedGauntlet = 0;
		int priced = 0;
		Long pricedTotal = 0L;
		int manualCount = 0;
		long manualKills = 0;
		int manualComparable = 0;
		Long manualTotal = 0L;
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
			RewardEdit edit = edits.get(reward.getGroupId());
			if (edit != null && edit.isExcluded())
			{
				excluded++;
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
			Long groupActual = reward.getConvertedGp();
			if (groupActual != null && edit != null && edit.getGpCorrection() != 0)
			{
				try
				{
					groupActual = Math.addExact(groupActual, edit.getGpCorrection());
					if (groupActual < 0)
					{
						groupActual = null;
					}
					else
					{
						corrected++;
						if (corrections != null)
						{
							try
							{
								corrections = Math.addExact(corrections, edit.getGpCorrection());
							}
							catch (ArithmeticException overflow)
							{
								corrections = null;
							}
						}
					}
				}
				catch (ArithmeticException overflow)
				{
					groupActual = null;
				}
			}
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
				return new RewardStatistics(normal, eggs, excluded, corrected, corrections,
					partialGauntlet, unclassifiedGauntlet,
					priced, pricedTotal, manualCount, manualKills, manualComparable, manualTotal,
					valued, 0, 0, null, null);
			}
		}
		for (ManualReward reward : manualRewards)
		{
			if (!reward.isIncluded() || !includeManual.test(reward))
			{
				continue;
			}
			manualCount++;
			manualKills += reward.getKillCountIncrease();
			if (manualTotal != null)
			{
				try
				{
					manualTotal = Math.addExact(manualTotal, reward.getActualGp());
				}
				catch (ArithmeticException overflow)
				{
					manualTotal = null;
				}
			}
			boolean newest = latestNormal == null || reward.getRecordedAtUtc().isAfter(latestNormal);
			if (newest)
			{
				latestNormal = reward.getRecordedAtUtc();
				lastDelta = null;
			}
			if (reward.getExpectedGp() == null)
			{
				continue;
			}
			try
			{
				long delta = Math.subtractExact(reward.getActualGp(), reward.getExpectedGp());
				actualTotal = Math.addExact(actualTotal, reward.getActualGp());
				expectedTotal = Math.addExact(expectedTotal, reward.getExpectedGp());
				manualComparable++;
				valued++;
				if (newest)
				{
					lastDelta = delta;
				}
			}
			catch (ArithmeticException overflow)
			{
				return new RewardStatistics(normal, eggs, excluded, corrected, corrections,
					partialGauntlet, unclassifiedGauntlet, priced, pricedTotal,
					manualCount, manualKills, manualComparable, manualTotal,
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
				return new RewardStatistics(normal, eggs, excluded, corrected, corrections,
					partialGauntlet, unclassifiedGauntlet,
					priced, pricedTotal, manualCount, manualKills, manualComparable, manualTotal,
					valued, actualTotal, expectedTotal, null, null);
			}
		}
		return new RewardStatistics(normal, eggs, excluded, corrected, corrections,
			partialGauntlet, unclassifiedGauntlet,
			priced, pricedTotal, manualCount, manualKills, manualComparable, manualTotal,
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

	public int getObservedNormalRewards() { return observedNormalRewards; }
	public int getObservedLootRewards() { return observedNormalRewards + observedEggRewards; }
	public int getObservedEggRewards() { return observedEggRewards; }
	public int getExcludedRewards() { return excludedRewards; }
	public int getCorrectedRewards() { return correctedRewards; }
	public Long getAppliedCorrectionGp() { return appliedCorrectionGp; }
	public int getPartialGauntletRewards() { return partialGauntletRewards; }
	public int getUnclassifiedGauntletRewards() { return unclassifiedGauntletRewards; }
	public int getPricedNormalRewards() { return pricedNormalRewards; }
	public Long getObservedConvertedGp() { return observedConvertedGp; }
	public int getManualRewardCount() { return manualRewardCount; }
	public int getImportedKills() { return importedKills; }
	public long getImportedActualGp() { return importedActualGp; }
	public long getManualKillCount() { return manualKillCount; }
	public int getManualComparableRewardCount() { return manualComparableRewardCount; }
	public Long getManualActualGp() { return manualActualGp; }
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
