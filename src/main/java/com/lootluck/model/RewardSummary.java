package com.lootluck.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Profile statistics for each scope, calculated from the same saved events. */
public final class RewardSummary
{
	private final Map<RewardScope, RewardStatistics> scopes;
	private final ZoneId calendarZone;

	private RewardSummary(Map<RewardScope, RewardStatistics> scopes, ZoneId calendarZone)
	{
		this.scopes = Collections.unmodifiableMap(new EnumMap<>(scopes));
		this.calendarZone = calendarZone;
	}

	public static RewardSummary fromEvents(List<SavedLootEvent> events, String sessionId,
		Instant asOfUtc, ZoneId calendarZone)
	{
		Objects.requireNonNull(events);
		Objects.requireNonNull(sessionId);
		Objects.requireNonNull(asOfUtc);
		Objects.requireNonNull(calendarZone);
		LocalDate today = asOfUtc.atZone(calendarZone).toLocalDate();
		LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		EnumMap<RewardScope, RewardStatistics> results = new EnumMap<>(RewardScope.class);
		results.put(RewardScope.ALL_TIME, RewardStatistics.summarize(events));
		results.put(RewardScope.CURRENT_RUN, RewardStatistics.summarize(events,
			event -> sessionId.equals(event.getSessionId())));
		results.put(RewardScope.TODAY, calendarStats(events, today, asOfUtc, calendarZone));
		results.put(RewardScope.THIS_WEEK, calendarStats(events, weekStart, asOfUtc, calendarZone));
		results.put(RewardScope.THIS_MONTH, calendarStats(events,
			today.withDayOfMonth(1), asOfUtc, calendarZone));
		results.put(RewardScope.THIS_YEAR, calendarStats(events,
			today.withDayOfYear(1), asOfUtc, calendarZone));
		return new RewardSummary(results, calendarZone);
	}

	private static RewardStatistics calendarStats(List<SavedLootEvent> events, LocalDate startDate,
		Instant asOfUtc, ZoneId zone)
	{
		Instant start = startDate.atStartOfDay(zone).toInstant();
		return RewardStatistics.summarize(events, event -> {
			Instant observedAt = event.getObservedAtUtc();
			return !observedAt.isBefore(start) && !observedAt.isAfter(asOfUtc);
		});
	}

	public RewardStatistics get(RewardScope scope) { return scopes.get(scope); }
	public ZoneId getCalendarZone() { return calendarZone; }
}
