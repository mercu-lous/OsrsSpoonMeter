package com.lootluck.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
		assertEquals(2, stats.getPricedNormalRewards());
		assertEquals(Long.valueOf(2300), stats.getObservedConvertedGp());
		assertEquals(Long.valueOf(1150), stats.getAverageGpPerPricedReward());
		assertEquals(2300, stats.getConvertedGp());
		assertEquals(2000, stats.getExpectedGp());
		assertEquals(Long.valueOf(300), stats.getNetLuckGp());
		assertEquals(new BigDecimal("15.0"), stats.getPercentVsExpected());
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
		assertEquals(2, stats.getPricedNormalRewards());
		assertEquals(Long.valueOf(300), stats.getAverageGpPerPricedReward());
		assertEquals(Long.valueOf(-500), stats.getNetLuckGp());
		assertEquals(new BigDecimal("-50.0"), stats.getPercentVsExpected());
		assertNull(stats.getLastRewardDeltaGp());
		RewardStatistics onlyOldReward = RewardStatistics.summarize(Collections.singletonList(unvalued));
		assertEquals(Long.valueOf(100), onlyOldReward.getAverageGpPerPricedReward());
		assertNull(onlyOldReward.getPercentVsExpected());
	}

	@Test
	public void zeroExpectedValueDoesNotProduceAPercentage()
	{
		RewardStatistics stats = RewardStatistics.summarize(Collections.singletonList(
			event(8, 8, "OPEN_STOMACH", 400, 402, 458, 100, 0L)));
		assertEquals(Long.valueOf(100), stats.getNetLuckGp());
		assertNull(stats.getPercentVsExpected());
	}

	@Test
	public void filtersCurrentPluginRunWithoutChangingAllTimeTotals()
	{
		SavedLootEvent previous = event(1, 1, "OPEN_STOMACH", 100, 102, 450, 500, 1000L, "previous-run");
		SavedLootEvent current = event(2, 2, "OPEN_STOMACH", 200, 202, 451, 1500, 1000L, "current-run");
		RewardSummary summary = RewardSummary.fromEvents(Arrays.asList(previous, current),
			"current-run", Instant.ofEpochSecond(300), ZoneOffset.UTC);
		assertEquals(2, summary.get(RewardScope.ALL_TIME).getObservedNormalRewards());
		assertEquals(2, summary.get(RewardScope.ALL_TIME).getValuedNormalRewards());
		assertEquals(Long.valueOf(0), summary.get(RewardScope.ALL_TIME).getNetLuckGp());
		assertEquals(1, summary.get(RewardScope.CURRENT_RUN).getObservedNormalRewards());
		assertEquals(1, summary.get(RewardScope.CURRENT_RUN).getValuedNormalRewards());
		assertEquals(Long.valueOf(500), summary.get(RewardScope.CURRENT_RUN).getNetLuckGp());
		assertEquals(2, summary.getHistory(RewardScope.ALL_TIME).size());
		assertEquals(1, summary.getHistory(RewardScope.CURRENT_RUN).size());
		assertEquals(current.getEventId(), summary.getHistory(RewardScope.ALL_TIME).get(0)
			.getLootEvent().getEventId());
	}

	@Test
	public void calendarScopesUseLocalMidnightMondayAndSelectedZone()
	{
		Instant asOf = Instant.parse("2026-11-09T15:00:00Z"); // Monday 09:00 in Chicago.
		List<SavedLootEvent> events = Arrays.asList(
			eventAt(1, 1, Instant.parse("2025-12-31T18:00:00Z"), 100, 1000L),
			eventAt(2, 2, Instant.parse("2026-10-31T18:00:00Z"), 100, 1000L),
			eventAt(3, 3, Instant.parse("2026-11-09T05:59:59Z"), 100, 1000L),
			eventAt(4, 4, Instant.parse("2026-11-09T06:00:00Z"), 100, 1000L),
			eventAt(5, 5, Instant.parse("2026-11-09T20:00:00Z"), 100, 1000L));
		RewardSummary chicago = RewardSummary.fromEvents(events, "session", asOf,
			ZoneId.of("America/Chicago"));
		assertEquals(5, chicago.get(RewardScope.ALL_TIME).getObservedNormalRewards());
		assertEquals(1, chicago.get(RewardScope.TODAY).getObservedNormalRewards());
		assertEquals(1, chicago.get(RewardScope.THIS_WEEK).getObservedNormalRewards());
		assertEquals(2, chicago.get(RewardScope.THIS_MONTH).getObservedNormalRewards());
		assertEquals(3, chicago.get(RewardScope.THIS_YEAR).getObservedNormalRewards());
		RewardSummary utc = RewardSummary.fromEvents(events, "session", asOf, ZoneOffset.UTC);
		assertEquals(2, utc.get(RewardScope.TODAY).getObservedNormalRewards());
	}

	@Test
	public void calendarFilterKeepsCallbacksFromOneRewardTogetherAcrossMidnight()
	{
		Instant asOf = Instant.parse("2026-11-09T06:01:00Z");
		SavedLootEvent beforeMidnight = eventAt(1, 1, Instant.parse("2026-11-09T05:59:59Z"), 100, 1000L);
		SavedLootEvent afterMidnight = eventAt(2, 1, Instant.parse("2026-11-09T06:00:01Z"), 200, 1000L);
		RewardSummary summary = RewardSummary.fromEvents(Arrays.asList(beforeMidnight, afterMidnight),
			"session", asOf, ZoneId.of("America/Chicago"));
		RewardStatistics today = summary.get(RewardScope.TODAY);
		assertEquals(1, today.getObservedNormalRewards());
		assertEquals(300, today.getConvertedGp());
		assertEquals(1000, today.getExpectedGp());
		assertEquals(2, summary.getHistory(RewardScope.TODAY).size());
	}

	@Test
	public void noLootConfirmationCountsOnceEvenWithTertiaryLootCallback()
	{
		SavedLootEvent tertiary = event(1, 1, "TAKE_EGGS", 100, 102, 450, 0, null);
		NoLootConfirmation confirmed = new NoLootConfirmation(tertiary.getRewardClick(), "session",
			tertiary.getObservedAtUtc(), 102, "GAMEMESSAGE");
		RewardSummary summary = RewardSummary.fromObservations(Collections.singletonList(tertiary),
			Arrays.asList(confirmed, confirmed), "session", Instant.ofEpochSecond(300), ZoneOffset.UTC);
		assertEquals(0, summary.get(RewardScope.ALL_TIME).getObservedEggRewards());
		assertEquals(1, summary.getConfirmedNoLoot(RewardScope.ALL_TIME));
		assertEquals(1, summary.getConfirmedNoLoot(RewardScope.CURRENT_RUN));
		assertEquals(1, summary.getConfirmedNoLoot(RewardScope.TODAY));
		assertEquals(2, summary.getHistory(RewardScope.ALL_TIME).size());
		assertEquals(1, summary.getHistory(RewardScope.ALL_TIME).stream()
			.filter(RewardHistoryEntry::isNoLootConfirmation).count());
	}

	@Test
	public void noLootConfirmationUsesItsOwnSessionAndCalendarTime()
	{
		RewardClickCandidate click = new RewardClickCandidate(id("no-loot-click"), PROFILE,
			"TAKE_EGGS", 100, 42, Instant.parse("2026-11-09T05:59:58Z"));
		NoLootConfirmation confirmed = new NoLootConfirmation(click, "previous-run",
			Instant.parse("2026-11-09T05:59:59Z"), 102, "SPAM");
		RewardSummary summary = RewardSummary.fromObservations(Collections.emptyList(),
			Collections.singletonList(confirmed), "current-run", Instant.parse("2026-11-09T15:00:00Z"),
			ZoneId.of("America/Chicago"));
		assertEquals(1, summary.getConfirmedNoLoot(RewardScope.ALL_TIME));
		assertEquals(0, summary.getConfirmedNoLoot(RewardScope.CURRENT_RUN));
		assertEquals(0, summary.getConfirmedNoLoot(RewardScope.TODAY));
		assertEquals(1, summary.getConfirmedNoLoot(RewardScope.THIS_MONTH));
	}

	private static SavedLootEvent eventAt(int eventNumber, int clickNumber, Instant time,
		long actualGp, Long expectedGp)
	{
		RewardClickCandidate click = new RewardClickCandidate(id("dated-click" + clickNumber), PROFILE,
			"OPEN_STOMACH", 100, 42, time.minusSeconds(1));
		ConversionQuote conversion = new ConversionQuote("DIRECT_GE", actualGp,
			null, "RUNELITE_JAGEX", 1, Collections.emptyList());
		LootItemPreview item = new LootItemPreview(450 + eventNumber, 1, "Test", actualGp,
			"RUNELITE_JAGEX", null, conversion);
		ExpectedValueSnapshot expected = new ExpectedValueSnapshot(expectedGp,
			MaggotKingExpectedValueService.SOURCE_VERSION, MaggotKingExpectedValueService.SOURCE_URL,
			MaggotKingConversionCatalog.VERSION, "RUNELITE_JAGEX", time, Collections.emptyMap());
		return new SavedLootEvent(id("dated-event" + eventNumber), PROFILE, "session", time,
			"Maggot King", "NPC", 0, 1, 102,
			Collections.singletonList(item), click, expected);
	}

	private static SavedLootEvent event(int eventNumber, int clickNumber, String method,
		int clickTick, int lootTick, int itemId, long actualGp, Long expectedGp)
	{
		return event(eventNumber, clickNumber, method, clickTick, lootTick, itemId,
			actualGp, expectedGp, "session");
	}

	private static SavedLootEvent event(int eventNumber, int clickNumber, String method,
		int clickTick, int lootTick, int itemId, long actualGp, Long expectedGp, String sessionId)
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
		return new SavedLootEvent(id("event" + eventNumber), PROFILE, sessionId, time,
			"Maggot King", "NPC", 0, 1, lootTick, Collections.singletonList(item), click, expected);
	}

	private static String id(String value)
	{
		return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
	}
}
