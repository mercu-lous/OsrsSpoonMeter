package com.lootluck.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ImportedBaselineStatisticsTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final Instant CUTOFF = Instant.parse("2026-11-11T01:00:00Z");

	@Test
	public void importedTotalReplacesOlderRecordsButHistoryKeepsThem()
	{
		ImportedBaseline baseline = new ImportedBaseline(PROFILE, "VORKATH", CUTOFF,
			10, 1_000_000, 1_000_000, 144404,
			Collections.singletonMap(995, 1_000_000L), Collections.singletonMap(995, 1L));
		ManualReward older = new ManualReward(UUID.randomUUID().toString(), PROFILE, "VORKATH",
			"run", CUTOFF.minusSeconds(10), 3000, null, "Already in Loot Tracker", true);
		ManualReward newer = new ManualReward(UUID.randomUUID().toString(), PROFILE, "VORKATH",
			"run", CUTOFF.plusSeconds(10), 4000, null, "New manual kill", true);
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Collections.emptyList(),
			Collections.emptyList(), RewardEdits.empty(), ManualRewards.empty().with(older).with(newer),
			Collections.singletonMap("VORKATH", baseline), "run", CUTOFF.plusSeconds(20), ZoneOffset.UTC);
		RewardSummary summary = dashboard.get(BossRegistry.VORKATH);
		RewardStatistics allTime = summary.get(RewardScope.ALL_TIME);
		assertEquals(10, allTime.getImportedKills());
		assertEquals(1, allTime.getManualKillCount());
		assertEquals(1_004_000, allTime.getConvertedGp());
		assertEquals(1_588_444, allTime.getExpectedGp());
		assertEquals(Long.valueOf(-584_444), allTime.getNetLuckGp());
		assertEquals(2, summary.getHistory(RewardScope.ALL_TIME).size());
		assertEquals(BossRegistry.VORKATH, dashboard.getRankings(RewardScope.ALL_TIME).get(0).getBoss());
	}

	@Test
	public void rewardGroupCrossingImportTimeIsNotCountedTwice()
	{
		ImportedBaseline baseline = new ImportedBaseline(PROFILE, "VORKATH", CUTOFF,
			10, 1_000_000, 1_000_000, 144404,
			Collections.singletonMap(995, 1_000_000L), Collections.singletonMap(995, 1L));
		SavedLootEvent first = callback("before", CUTOFF.minusSeconds(1), 1);
		SavedLootEvent second = callback("after", CUTOFF.plusSeconds(1), 2);
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Arrays.asList(first, second),
			Collections.emptyList(), RewardEdits.empty(), ManualRewards.empty(),
			Collections.singletonMap("VORKATH", baseline), "run", CUTOFF.plusSeconds(20), ZoneOffset.UTC);
		RewardSummary summary = dashboard.get(BossRegistry.VORKATH);
		assertEquals(0, summary.get(RewardScope.ALL_TIME).getObservedNormalRewards());
		assertEquals(1_000_000, summary.get(RewardScope.ALL_TIME).getConvertedGp());
		assertEquals(2, summary.getHistory(RewardScope.ALL_TIME).size());
	}

	private static SavedLootEvent callback(String name, Instant at, int tick)
	{
		return new SavedLootEvent(UUID.nameUUIDFromBytes(name.getBytes()).toString(), PROFILE,
			"run", at, "Vorkath", "NPC", 0, 1, tick,
			Collections.singletonList(new LootItemPreview(995, 100, "Coins", 1L, "TEST")));
	}
}
