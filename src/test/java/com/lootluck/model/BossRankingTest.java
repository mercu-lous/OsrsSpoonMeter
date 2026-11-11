package com.lootluck.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BossRankingTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final Instant START = Instant.parse("2026-11-09T12:00:00Z");

	@Test
	public void ranksComparableBossesWithinTheSelectedScope()
	{
		ProfileDashboard dashboard = ProfileDashboard.fromObservations(Arrays.asList(
			event("vorkath", "Vorkath", "older-run", 0, 288808L, 144404L),
			event("zulrah", "Zulrah", "current-run", 60, 116535L, 116535L),
			event("corrupted", "Corrupted Hunllef", "current-run", 120, 275190L, 550380L),
			event("regular-unvalued", "Crystalline Hunllef", "current-run", 180, 500000L, null)),
			Collections.emptyList(), "current-run", START.plusSeconds(300), ZoneOffset.UTC);

		List<BossRankingEntry> all = dashboard.getRankings(RewardScope.ALL_TIME);
		assertEquals(3, all.size());
		assertSame(BossRegistry.VORKATH, all.get(0).getBoss());
		assertEquals(new BigDecimal("100.0"), all.get(0).getPercentVsExpected());
		assertSame(BossRegistry.ZULRAH, all.get(1).getBoss());
		assertEquals(new BigDecimal("0.0"), all.get(1).getPercentVsExpected());
		assertSame(BossRegistry.CORRUPTED_GAUNTLET, all.get(2).getBoss());
		assertEquals(new BigDecimal("-50.0"), all.get(2).getPercentVsExpected());

		List<BossRankingEntry> current = dashboard.getRankings(RewardScope.CURRENT_RUN);
		assertEquals(2, current.size());
		assertSame(BossRegistry.ZULRAH, current.get(0).getBoss());
		assertSame(BossRegistry.CORRUPTED_GAUNTLET, current.get(1).getBoss());
	}

	private static SavedLootEvent event(String name, String source, String session,
		long secondsAfterStart, long actualGp, Long expectedGp)
	{
		Instant time = START.plusSeconds(secondsAfterStart);
		ConversionQuote quote = new ConversionQuote("DIRECT_GE", actualGp, null,
			"RUNELITE_JAGEX", 1, Collections.emptyList());
		ExpectedValueSnapshot expected = expectedGp == null ? null : new ExpectedValueSnapshot(
			expectedGp, "TEST_BASELINE_V1", "https://oldschool.runescape.wiki/w/",
			DirectGeConversion.VERSION, "WIKI_FIXED_AVERAGE", time, Collections.emptyMap());
		return new SavedLootEvent(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString(),
			PROFILE, session, time, source, "NPC", 0, 1, 102,
			Collections.singletonList(new LootItemPreview(450, 1, "Test", actualGp,
				"RUNELITE_JAGEX", null, quote)), null, expected);
	}
}
