package com.lootluck.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ManualRewardEntriesTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final Instant DAY_END = Instant.parse("2026-11-10T23:00:00Z");

	@Test
	public void manualRewardsStaySeparateFromLootAndOnlyEnteredBaselinesAffectLuck()
	{
		ManualReward compared = reward("VORKATH", "older", DAY_END, 200, 100L, true);
		ManualReward unvalued = reward("MAGGOT_KING", "current", DAY_END.plusSeconds(7200), 50, null, true);
		ManualReward excluded = reward("CORRUPTED_GAUNTLET", "current",
			DAY_END.plusSeconds(7300), 500, 200L, false);
		ManualRewards rewards = ManualRewards.empty().with(compared).with(unvalued).with(excluded);
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Collections.emptyList(),
			Collections.emptyList(), RewardEdits.empty(), rewards,
			"current", DAY_END.plusSeconds(8000), ZoneOffset.UTC);

		RewardStatistics vorkath = dashboard.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME);
		assertEquals(0, vorkath.getObservedLootRewards());
		assertEquals(1, vorkath.getManualRewardCount());
		assertEquals(1, vorkath.getManualComparableRewardCount());
		assertEquals(Long.valueOf(200), vorkath.getManualActualGp());
		assertEquals(Long.valueOf(100), vorkath.getNetLuckGp());
		assertEquals(new BigDecimal("100.0"), vorkath.getPercentVsExpected());
		assertEquals(0, dashboard.get(BossRegistry.VORKATH).get(RewardScope.CURRENT_RUN)
			.getManualRewardCount());
		assertEquals(0, dashboard.get(BossRegistry.VORKATH).get(RewardScope.TODAY)
			.getManualRewardCount());
		assertEquals(1, dashboard.get(BossRegistry.VORKATH).getHistory(RewardScope.ALL_TIME).size());
		assertTrue(dashboard.get(BossRegistry.VORKATH).getHistory(RewardScope.ALL_TIME)
			.get(0).isManualReward());

		RewardStatistics maggot = dashboard.get(BossRegistry.MAGGOT_KING).get(RewardScope.TODAY);
		assertEquals(1, maggot.getManualRewardCount());
		assertEquals(1, maggot.getManualKillCount());
		assertEquals(Long.valueOf(50), maggot.getManualActualGp());
		assertEquals(0, maggot.getManualComparableRewardCount());
		assertNull(maggot.getNetLuckGp());
		assertEquals(1, dashboard.getRankings(RewardScope.ALL_TIME).size());
		assertSame(BossRegistry.VORKATH, dashboard.getRankings(RewardScope.ALL_TIME).get(0).getBoss());
		assertEquals(0, dashboard.get(BossRegistry.CORRUPTED_GAUNTLET)
			.get(RewardScope.ALL_TIME).getManualRewardCount());
		assertFalse(dashboard.get(BossRegistry.CORRUPTED_GAUNTLET)
			.getManualRewards(RewardScope.ALL_TIME).get(0).isIncluded());
	}

	@Test
	public void excludedManualRewardCanBeRestored()
	{
		ManualReward reward = reward("VORKATH", "current", DAY_END, 75, 100L, false);
		assertFalse(reward.isIncluded());
		assertTrue(reward.withIncluded(true).isIncluded());
		assertEquals(reward.getId(), reward.withIncluded(true).getId());
	}

	@Test
	public void blankExpectedUsesBossDefaultAndMultipliesByManualKills()
	{
		ManualReward reward = new ManualReward(UUID.randomUUID().toString(), PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET.getId(), "current", DAY_END,
			600000, null, 2, "Two missed wins", true);
		assertTrue(reward.usesBossDefault());
		assertEquals(2, reward.getKillCountIncrease());
		assertEquals(Long.valueOf(550380), reward.getExpectedGpPerKill());
		assertEquals(Long.valueOf(1100760), reward.getExpectedGp());
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Collections.emptyList(),
			Collections.emptyList(), RewardEdits.empty(), ManualRewards.empty().with(reward),
			"current", DAY_END.plusSeconds(5), ZoneOffset.UTC);
		RewardStatistics stats = dashboard.get(BossRegistry.CORRUPTED_GAUNTLET)
			.get(RewardScope.ALL_TIME);
		assertEquals(2, stats.getManualKillCount());
		assertEquals(Long.valueOf(-500760), stats.getNetLuckGp());
		assertEquals(1, stats.getManualComparableRewardCount());
	}

	private static ManualReward reward(String boss, String session, Instant time,
		long actual, Long expected, boolean included)
	{
		return new ManualReward(UUID.randomUUID().toString(), PROFILE, boss, session,
			time, actual, expected, "Missed callback", included);
	}
}
