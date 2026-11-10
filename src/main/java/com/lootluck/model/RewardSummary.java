package com.lootluck.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Profile statistics for each scope, calculated from the same saved events. */
public final class RewardSummary
{
	private static final int HISTORY_LIMIT = 30;
	private final Map<RewardScope, RewardStatistics> scopes;
	private final Map<RewardScope, Integer> confirmedNoLoot;
	private final Map<RewardScope, List<RewardHistoryEntry>> history;
	private final ZoneId calendarZone;

	private RewardSummary(Map<RewardScope, RewardStatistics> scopes,
		Map<RewardScope, Integer> confirmedNoLoot,
		Map<RewardScope, List<RewardHistoryEntry>> history, ZoneId calendarZone)
	{
		this.scopes = Collections.unmodifiableMap(new EnumMap<>(scopes));
		this.confirmedNoLoot = Collections.unmodifiableMap(new EnumMap<>(confirmedNoLoot));
		this.history = Collections.unmodifiableMap(new EnumMap<>(history));
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
		EnumMap<RewardScope, List<RewardHistoryEntry>> histories = buildHistory(events,
			uniqueConfirmations, sessionId, asOfUtc, calendarZone, today, weekStart);
		return new RewardSummary(results, noLootCounts, histories, calendarZone);
	}

	private static EnumMap<RewardScope, List<RewardHistoryEntry>> buildHistory(
		List<SavedLootEvent> events, Map<String, NoLootConfirmation> confirmations,
		String sessionId, Instant asOfUtc, ZoneId zone, LocalDate today, LocalDate weekStart)
	{
		Map<String, Instant> clickTimes = new HashMap<>();
		for (SavedLootEvent event : events)
		{
			if (event.getRewardClick() != null)
			{
				clickTimes.merge(event.getRewardClick().getClickId(), event.getObservedAtUtc(),
					(first, second) -> first.isAfter(second) ? first : second);
			}
		}
		for (NoLootConfirmation confirmation : confirmations.values())
		{
			clickTimes.merge(confirmation.getClick().getClickId(), confirmation.getObservedAtUtc(),
				(first, second) -> first.isAfter(second) ? first : second);
		}
		List<RewardHistoryEntry> allEntries = new ArrayList<>();
		for (SavedLootEvent event : events)
		{
			Instant scopeTime = event.getRewardClick() == null ? event.getObservedAtUtc()
				: clickTimes.get(event.getRewardClick().getClickId());
			allEntries.add(RewardHistoryEntry.loot(event, scopeTime));
		}
		for (NoLootConfirmation confirmation : confirmations.values())
		{
			allEntries.add(RewardHistoryEntry.noLoot(confirmation,
				clickTimes.get(confirmation.getClick().getClickId())));
		}
		allEntries.sort(Comparator.comparing(RewardHistoryEntry::getObservedAtUtc).reversed());
		EnumMap<RewardScope, Instant> starts = new EnumMap<>(RewardScope.class);
		starts.put(RewardScope.TODAY, today.atStartOfDay(zone).toInstant());
		starts.put(RewardScope.THIS_WEEK, weekStart.atStartOfDay(zone).toInstant());
		starts.put(RewardScope.THIS_MONTH, today.withDayOfMonth(1).atStartOfDay(zone).toInstant());
		starts.put(RewardScope.THIS_YEAR, today.withDayOfYear(1).atStartOfDay(zone).toInstant());
		EnumMap<RewardScope, List<RewardHistoryEntry>> histories = new EnumMap<>(RewardScope.class);
		for (RewardScope scope : RewardScope.values())
		{
			List<RewardHistoryEntry> included = new ArrayList<>();
			for (RewardHistoryEntry entry : allEntries)
			{
				if (inScope(scope, entry, sessionId, asOfUtc, starts))
				{
					included.add(entry);
					if (included.size() == HISTORY_LIMIT)
					{
						break;
					}
				}
			}
			histories.put(scope, Collections.unmodifiableList(included));
		}
		return histories;
	}

	private static boolean inScope(RewardScope scope, RewardHistoryEntry entry,
		String sessionId, Instant asOfUtc, Map<RewardScope, Instant> starts)
	{
		if (scope == RewardScope.ALL_TIME)
		{
			return true;
		}
		if (scope == RewardScope.CURRENT_RUN)
		{
			return sessionId.equals(entry.getSessionId());
		}
		Instant time = entry.getScopeTimeUtc();
		return !time.isBefore(starts.get(scope)) && !time.isAfter(asOfUtc);
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
	public List<RewardHistoryEntry> getHistory(RewardScope scope) { return history.get(scope); }
	public ZoneId getCalendarZone() { return calendarZone; }
}
