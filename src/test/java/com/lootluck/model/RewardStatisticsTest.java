package com.lootluck.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RewardStatisticsTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Test
	public void countsOneRewardPerClickAndSumsDistinctLootEvents()
	{
		SavedLootEvent first = event(1, 1, "OPEN_STOMACH", 100, 102, 450, 100, 1000L);
		SavedLootEvent extra = event(2, 1, "OPEN_STOMACH", 100, 103, 451, 200, 1000L);
		SavedLootEvent duplicate = event(3, 1, "OPEN_STOMACH", 100, 103, 451, 200, 1000L);
		SavedLootEvent second = event(4, 2, "OPEN_STOMACH", 200, 202, 452, 2000, 1000L);
		RewardStatistics stats = RewardStatistics.summarize(Arrays.asList(second, duplicate, extra, first));
		assertEquals(2, stats.getObservedNormalRewards());
		assertEquals(2, stats.getValuedNormalRewards());
		assertEquals(2300, stats.getConvertedGp());
		assertEquals(2000, stats.getExpectedGp());
		assertEquals(Long.valueOf(300), stats.getNetLuckGp());
		assertEquals(Long.valueOf(1000), stats.getLastRewardDeltaGp());
	}

	@Test
	public void excludesUnvaluedAndEggRewardsFromNormalLuck()
	{
		SavedLootEvent valued = event(1, 1, "OPEN_STOMACH", 100, 102, 450, 500, 1000L);
		SavedLootEvent unvalued = event(2, 2, "OPEN_STOMACH", 200, 202, 451, 100, null);
		SavedLootEvent eggs = event(3, 3, "TAKE_EGGS", 300, 302, 452, 0, null);
		RewardStatistics stats = RewardStatistics.summarize(Arrays.asList(valued, unvalued, eggs));
		assertEquals(2, stats.getObservedNormalRewards());
		assertEquals(1, stats.getObservedEggRewards());
		assertEquals(1, stats.getValuedNormalRewards());
		assertEquals(Long.valueOf(-500), stats.getNetLuckGp());
		assertNull(stats.getLastRewardDeltaGp());
	}

	private static SavedLootEvent event(int eventNumber, int clickNumber, String method,
		int clickTick, int lootTick, int itemId, long actualGp, Long expectedGp)
	{
		Instant time = Instant.ofEpochSecond(lootTick);
		RewardClickCandidate click = new RewardClickCandidate(id("click" + clickNumber), PROFILE,
			method, clickTick, 42, time.minusSeconds(1));
		ConversionQuote conversion = new ConversionQuote("DIRECT_GE", actualGp,
			null, "RUNELITE_JAGEX", 1, Collections.emptyList());
		LootItemPreview item = new LootItemPreview(itemId, 1, "Test", actualGp,
			"RUNELITE_JAGEX", null, conversion);
		ExpectedValueSnapshot expected = expectedGp == null ? null : new ExpectedValueSnapshot(expectedGp,
			MaggotKingExpectedValueService.SOURCE_VERSION, MaggotKingExpectedValueService.SOURCE_URL,
			MaggotKingConversionCatalog.VERSION, "RUNELITE_JAGEX", time, Collections.emptyMap());
		return new SavedLootEvent(id("event" + eventNumber), PROFILE, "session", time,
			"Maggot King", "NPC", 0, 1, lootTick, Collections.singletonList(item), click, expected);
	}

	private static String id(String value)
	{
		return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
	}
}
