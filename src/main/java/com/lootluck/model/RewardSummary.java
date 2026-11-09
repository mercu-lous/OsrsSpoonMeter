package com.lootluck.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Profile statistics for each scope, calculated from the same saved events. */
public final class RewardSummary
{
	private final Map<RewardScope, RewardStatistics> scopes;
	private final Map<RewardScope, Integer> confirmedNoLoot;
	private final ZoneId calendarZone;

	private RewardSummary(Map<RewardScope, RewardStatistics> scopes,
		Map<RewardScope, Integer> confirmedNoLoot, ZoneId calendarZone)
	{
		this.scopes = Collections.unmodifiableMap(new EnumMap<>(scopes));
		this.confirmedNoLoot = Collections.unmodifiableMap(new EnumMap<>(confirmedNoLoot));
		this.calendarZone = calendarZone;
	}

	public static RewardSummary fromEvents(List<SavedLootEvent> events, String sessionId,
		Instant asOfUtc, ZoneId calendarZone)
	{
		return fromObservations(events, Collections.emptyList(), sessionId, asOfUtc, calendarZone);
	}

	public static RewardSummary fromObservations(List<SavedLootEvent> events,
		List<NoLootConfirmation> confirmations, String sessionId,
		Instant asOfUtc, ZoneId calendarZone)
	{
		Objects.requireNonNull(events);
		Objects.requireNonNull(confirmations);
		Objects.requireNonNull(sessionId);
		Objects.requireNonNull(asOfUtc);
		Objects.requireNonNull(calendarZone);
		LocalDate today = asOfUtc.atZone(calendarZone).toLocalDate();
		LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		Map<String, NoLootConfirmation> uniqueConfirmations = new LinkedHashMap<>();
		for (NoLootConfirmation confirmation : confirmations)
		{
			uniqueConfirmations.putIfAbsent(confirmation.getClick().getClickId(), confirmation);
		}
		Set<String> noLootClickIds = uniqueConfirmations.keySet();
		EnumMap<RewardScope, RewardStatistics> results = new EnumMap<>(RewardScope.class);
		results.put(RewardScope.ALL_TIME, RewardStatistics.summarize(events,
			event -> true, noLootClickIds));
		results.put(RewardScope.CURRENT_RUN, RewardStatistics.summarize(events,
			event -> sessionId.equals(event.getSessionId()), noLootClickIds));
		results.put(RewardScope.TODAY, calendarStats(events, today, asOfUtc, calendarZone, noLootClickIds));
		results.put(RewardScope.THIS_WEEK, calendarStats(events,
			weekStart, asOfUtc, calendarZone, noLootClickIds));
		results.put(RewardScope.THIS_MONTH, calendarStats(events,
			today.withDayOfMonth(1), asOfUtc, calendarZone, noLootClickIds));
		results.put(RewardScope.THIS_YEAR, calendarStats(events,
			today.withDayOfYear(1), asOfUtc, calendarZone, noLootClickIds));
		EnumMap<RewardScope, Integer> noLootCounts = new EnumMap<>(RewardScope.class);
		noLootCounts.put(RewardScope.ALL_TIME, uniqueConfirmations.size());
		noLootCounts.put(RewardScope.CURRENT_RUN, countNoLoot(uniqueConfirmations,
			confirmation -> sessionId.equals(confirmation.getSessionId())));
		noLootCounts.put(RewardScope.TODAY, calendarNoLoot(uniqueConfirmations,
			today, asOfUtc, calendarZone));
		noLootCounts.put(RewardScope.THIS_WEEK, calendarNoLoot(uniqueConfirmations,
			weekStart, asOfUtc, calendarZone));
		noLootCounts.put(RewardScope.THIS_MONTH, calendarNoLoot(uniqueConfirmations,
			today.withDayOfMonth(1), asOfUtc, calendarZone));
		noLootCounts.put(RewardScope.THIS_YEAR, calendarNoLoot(uniqueConfirmations,
			today.withDayOfYear(1), asOfUtc, calendarZone));
		return new RewardSummary(results, noLootCounts, calendarZone);
	}

	private static RewardStatistics calendarStats(List<SavedLootEvent> events, LocalDate startDate,
		Instant asOfUtc, ZoneId zone, Set<String> confirmedNoLootClickIds)
	{
		Instant start = startDate.atStartOfDay(zone).toInstant();
		return RewardStatistics.summarize(events, event -> {
			Instant observedAt = event.getObservedAtUtc();
			return !observedAt.isBefore(start) && !observedAt.isAfter(asOfUtc);
		}, confirmedNoLootClickIds);
	}

	private static int calendarNoLoot(Map<String, NoLootConfirmation> confirmations,
		LocalDate startDate, Instant asOfUtc, ZoneId zone)
	{
		Instant start = startDate.atStartOfDay(zone).toInstant();
		return countNoLoot(confirmations, confirmation -> {
			Instant observedAt = confirmation.getObservedAtUtc();
			return !observedAt.isBefore(start) && !observedAt.isAfter(asOfUtc);
		});
	}

	private static int countNoLoot(Map<String, NoLootConfirmation> confirmations,
		Predicate<NoLootConfirmation> include)
	{
		int count = 0;
		for (NoLootConfirmation confirmation : confirmations.values())
		{
			if (include.test(confirmation))
			{
				count++;
			}
		}
		return count;
	}

	public RewardStatistics get(RewardScope scope) { return scopes.get(scope); }
	public int getConfirmedNoLoot(RewardScope scope) { return confirmedNoLoot.get(scope); }
	public ZoneId getCalendarZone() { return calendarZone; }
}
