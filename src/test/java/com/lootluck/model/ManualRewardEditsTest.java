package com.lootluck.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ManualRewardEditsTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final Instant START = Instant.parse("2026-11-10T18:00:00Z");

	@Test
	public void exclusionAndGpCorrectionUpdateTotalsRankingAndHistory()
	{
		SavedLootEvent older = event("older", START, "older-run", 10000);
		SavedLootEvent current = event("current", START.plusSeconds(60), "current-run", 20000);
		RewardEdits edits = RewardEdits.empty()
			.with(new RewardEdit(older.getEventId(), true, 0, "Failed run", START.plusSeconds(86490)))
			.with(new RewardEdit(current.getEventId(), false, -5000,
				"Left loot on ground", START.plusSeconds(86490)));
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Arrays.asList(older, current),
			Collections.emptyList(), edits, "current-run", START.plusSeconds(86520), ZoneOffset.UTC);
		RewardStatistics all = dashboard.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME);
		assertEquals(1, all.getObservedLootRewards());
		assertEquals(1, all.getExcludedRewards());
		assertEquals(1, all.getCorrectedRewards());
		assertEquals(Long.valueOf(-5000), all.getAppliedCorrectionGp());
		assertEquals(Long.valueOf(15000), all.getObservedConvertedGp());
		assertEquals(Long.valueOf(15000 - 144404), all.getNetLuckGp());
		assertEquals(1, dashboard.getRankings(RewardScope.ALL_TIME).size());
		assertEquals(2, dashboard.get(BossRegistry.VORKATH).getHistory(RewardScope.ALL_TIME).size());
		assertNotNull(dashboard.get(BossRegistry.VORKATH).getEditForEvent(older.getEventId()));
		assertEquals(0, dashboard.get(BossRegistry.VORKATH).get(RewardScope.CURRENT_RUN)
			.getExcludedRewards());
		assertEquals(1, dashboard.get(BossRegistry.VORKATH).get(RewardScope.CURRENT_RUN)
			.getCorrectedRewards());
		assertEquals(0, dashboard.get(BossRegistry.VORKATH).get(RewardScope.TODAY)
			.getCorrectedRewards());
		assertEquals(0, dashboard.get(BossRegistry.VORKATH).getEditableGroups(RewardScope.TODAY).size());
	}

	@Test
	public void oneCorrectionAppliesOnceToMultiCallbackRewardAndCanBeCleared()
	{
		SavedLootEvent first = event("first", START, "run", 1000);
		SavedLootEvent second = event("second", START.plusSeconds(2), "run", 2000);
		RewardEdits edits = RewardEdits.empty().with(new RewardEdit(first.getEventId(),
			false, 500, "Missed item", START.plusSeconds(5)));
		RewardSummary summary = RewardSummary.fromObservations(Arrays.asList(first, second),
			Collections.emptyList(), edits, "run", START.plusSeconds(10), ZoneOffset.UTC);
		RewardStatistics stats = summary.get(RewardScope.ALL_TIME);
		assertEquals(1, stats.getObservedLootRewards());
		assertEquals(1, stats.getCorrectedRewards());
		assertEquals(Long.valueOf(3500), stats.getObservedConvertedGp());
		assertEquals(Long.valueOf(3500 - 144404), stats.getNetLuckGp());
		assertEquals(first.getEventId(), summary.getEditableGroups(RewardScope.ALL_TIME)
			.get(0).getGroupId());
		assertNotNull(summary.getEditForEvent(second.getEventId()));
		RewardEdits cleared = edits.with(new RewardEdit(first.getEventId(), false, 0, "",
			START.plusSeconds(20)));
		assertNull(cleared.get(first.getEventId()));
		RewardStatistics restored = RewardStatistics.summarize(Arrays.asList(first, second),
			event -> true, Collections.emptySet(), cleared);
		assertEquals(Long.valueOf(3000), restored.getObservedConvertedGp());
	}

	private static SavedLootEvent event(String name, Instant time, String session, long gp)
	{
		ConversionQuote quote = new ConversionQuote("DIRECT_GE", gp, null,
			"RUNELITE_JAGEX", 1, Collections.emptyList());
		ExpectedValueSnapshot expected = BossRegistry.VORKATH.getRules().expected(null,
			id -> 1L, "RUNELITE_JAGEX", time);
		return new SavedLootEvent(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString(),
			PROFILE, session, time, "Vorkath", "NPC", 0, 1,
			(int) (102 + time.getEpochSecond() - START.getEpochSecond()),
			Collections.singletonList(new LootItemPreview(450, 1, "Test", gp,
				"RUNELITE_JAGEX", null, quote)), null, expected);
	}
}
