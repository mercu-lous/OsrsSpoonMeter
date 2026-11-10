package com.lootluck.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BossRegistryAndGroupingTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final String OTHER_PROFILE = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
	private static final Instant START = Instant.parse("2026-11-09T12:00:00Z");

	@Test
	public void registryUsesBossSourcesAndRules()
	{
		assertSame(BossRegistry.MAGGOT_KING, BossRegistry.findBySource(" Maggot King "));
		assertSame(BossRegistry.VORKATH, BossRegistry.findBySource("vorkath"));
		assertSame(BossRegistry.ZULRAH, BossRegistry.findBySource("ZULRAH"));
		assertSame(BossRegistry.CORRUPTED_GAUNTLET, BossRegistry.findBySource("Corrupted Hunllef"));
		assertSame(BossRegistry.GAUNTLET, BossRegistry.findBySource("Crystalline Hunllef"));
		assertNull(BossRegistry.findBySource("Guard"));
		assertEquals(5, BossRegistry.bosses().size());
		assertEquals(DirectGeConversion.VERSION, BossRegistry.VORKATH.getRules().conversionVersion());
		assertEquals(Long.valueOf(144404), BossRegistry.VORKATH.getRules().expected(null,
			id -> 1L, "RUNELITE_JAGEX", START).getExpectedGp());
		assertEquals(Long.valueOf(116535), BossRegistry.ZULRAH.getRules().expected(null,
			id -> 1L, "RUNELITE_JAGEX", START).getExpectedGp());
		assertEquals(Long.valueOf(550380), BossRegistry.CORRUPTED_GAUNTLET.getRules().expected(null,
			id -> 1L, "RUNELITE_JAGEX", START).getExpectedGp());
		assertEquals(Long.valueOf(150815), BossRegistry.GAUNTLET.getRules().expected(null,
			id -> 1L, "RUNELITE_JAGEX", START).getExpectedGp());
		assertEquals(FixedExpectedBossRules.PRICE_SOURCE,
			BossRegistry.GAUNTLET.getRules().expected(null, id -> 1L,
				"RUNELITE_JAGEX", START).getPriceSource());
	}

	@Test
	public void genericUntradeablesContributeZeroDirectGe()
	{
		ConversionQuote quote = BossRegistry.GAUNTLET.getRules().quote(23951, 7, 0L,
			false, id -> 0L, "RUNELITE_JAGEX");
		assertEquals("NO_GE_CONVERSION", quote.getMethod());
		assertEquals(Long.valueOf(0), quote.getStackGp());
	}

	@Test
	public void groupsCallbacksWithinFiveSecondsFromFirstCallback()
	{
		SavedLootEvent first = event("first", PROFILE, "run", "Vorkath", 0, null);
		SavedLootEvent near = event("near", PROFILE, "run", "Vorkath", 4, null);
		SavedLootEvent beyondAnchor = event("beyond", PROFILE, "run", "Vorkath", 6, null);
		List<LootRewardGroup> groups = LootRewardGrouper.group(Arrays.asList(beyondAnchor, near, first));
		assertEquals(2, groups.size());
		assertEquals(2, groups.get(0).getEvents().size());
		assertEquals(first.getEventId(), groups.get(0).getEvents().get(0).getEventId());
		assertEquals(1, groups.get(1).getEvents().size());
		assertEquals(2, RewardStatistics.summarize(Arrays.asList(first, near, beyondAnchor))
			.getObservedLootRewards());
	}

	@Test
	public void keepsBossProfileSessionAndDifferentClicksSeparate()
	{
		RewardClickCandidate firstClick = click("first-click", 100);
		RewardClickCandidate secondClick = click("second-click", 101);
		List<LootRewardGroup> groups = LootRewardGrouper.group(Arrays.asList(
			event("m1", PROFILE, "run", "Maggot King", 0, firstClick),
			event("m2", PROFILE, "run", "Maggot King", 1, secondClick),
			event("v1", PROFILE, "run", "Vorkath", 1, null),
			event("v2", OTHER_PROFILE, "run", "Vorkath", 2, null),
			event("v3", PROFILE, "other-run", "Vorkath", 2, null)));
		assertEquals(5, groups.size());
	}

	@Test
	public void dashboardSeparatesBossHistoryAndRewardCounts()
	{
		List<SavedLootEvent> events = Arrays.asList(
			event("m", PROFILE, "run", "Maggot King", 0, null),
			event("v1", PROFILE, "run", "Vorkath", 1, null),
			event("v2", PROFILE, "run", "Vorkath", 2, null),
			event("z", PROFILE, "run", "Zulrah", 3, null),
			event("cg", PROFILE, "run", "Corrupted Hunllef", 4, null),
			event("rg", PROFILE, "run", "Crystalline Hunllef", 5, null));
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(events,
			Collections.emptyList(), "run", START.plusSeconds(10), ZoneOffset.UTC);
		assertEquals(1, dashboard.get(BossRegistry.MAGGOT_KING).get(RewardScope.ALL_TIME)
			.getObservedLootRewards());
		assertEquals(1, dashboard.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME)
			.getObservedLootRewards());
		assertEquals(1, dashboard.get(BossRegistry.ZULRAH).get(RewardScope.ALL_TIME)
			.getObservedLootRewards());
		assertEquals(1, dashboard.get(BossRegistry.CORRUPTED_GAUNTLET).get(RewardScope.ALL_TIME)
			.getObservedLootRewards());
		assertEquals(1, dashboard.get(BossRegistry.GAUNTLET).get(RewardScope.ALL_TIME)
			.getObservedLootRewards());
		assertEquals(2, dashboard.get(BossRegistry.VORKATH).getHistory(RewardScope.ALL_TIME).size());
		assertEquals("Vorkath", dashboard.get(BossRegistry.VORKATH).getHistory(RewardScope.ALL_TIME)
			.get(0).getLootEvent().getSource());
		assertNull(dashboard.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getNetLuckGp());
	}

	@Test
	public void futureBossCanCompareExpectedGpWithoutMenuClick()
	{
		ExpectedValueSnapshot expected = new ExpectedValueSnapshot(1000L, "TEST_TABLE_V1",
			BossRegistry.VORKATH.getWikiUrl(), DirectGeConversion.VERSION,
			"RUNELITE_JAGEX", START, Collections.emptyMap());
		ConversionQuote conversion = new ConversionQuote("DIRECT_GE", 100L, null,
			"RUNELITE_JAGEX", 1, Collections.emptyList());
		SavedLootEvent event = new SavedLootEvent(id("valued-vorkath"), PROFILE, "run", START,
			"Vorkath", "NPC", 0, 1, 102, Collections.singletonList(
				new LootItemPreview(450, 1, "Test", 100L, "RUNELITE_JAGEX", null, conversion)),
			null, expected);
		RewardStatistics stats = RewardStatistics.summarize(Collections.singletonList(event));
		assertEquals(1, stats.getObservedLootRewards());
		assertEquals(Long.valueOf(-900), stats.getNetLuckGp());
	}

	private static SavedLootEvent event(String name, String profile, String session,
		String source, long second, RewardClickCandidate click)
	{
		int tick = 102;
		return new SavedLootEvent(id(name), profile, session, START.plusSeconds(second),
			source, "NPC", 0, 1, tick, Collections.singletonList(
			new LootItemPreview(450, 1, "Test", 100L, "RUNELITE_JAGEX")), click);
	}

	private static RewardClickCandidate click(String name, int tick)
	{
		return new RewardClickCandidate(id(name), PROFILE, "OPEN_STOMACH", tick, 42, START);
	}

	private static String id(String value)
	{
		return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
	}
}
