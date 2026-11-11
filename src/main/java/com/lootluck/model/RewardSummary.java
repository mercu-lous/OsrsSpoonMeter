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
import java.util.IdentityHashMap;
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
	private final List<SavedLootEvent> events;
	private final String sessionId;
	private final Instant asOfUtc;
	private final RewardEdits edits;
	private final List<ManualReward> manualRewards;
	private final Map<String, String> groupIdByEventId;

	private RewardSummary(Map<RewardScope, RewardStatistics> scopes,
		Map<RewardScope, Integer> confirmedNoLoot,
		Map<RewardScope, List<RewardHistoryEntry>> history, ZoneId calendarZone,
		List<SavedLootEvent> events, List<ManualReward> manualRewards,
		String sessionId, Instant asOfUtc, RewardEdits edits)
	{
		this.scopes = Collections.unmodifiableMap(new EnumMap<>(scopes));
		this.confirmedNoLoot = Collections.unmodifiableMap(new EnumMap<>(confirmedNoLoot));
		this.history = Collections.unmodifiableMap(new EnumMap<>(history));
		this.calendarZone = calendarZone;
		this.events = Collections.unmodifiableList(new ArrayList<>(events));
		this.manualRewards = Collections.unmodifiableList(new ArrayList<>(manualRewards));
		this.sessionId = sessionId;
		this.asOfUtc = asOfUtc;
		this.edits = edits;
		Map<String, String> groups = new HashMap<>();
		for (LootRewardGroup group : LootRewardGrouper.group(events))
		{
			for (SavedLootEvent event : group.getEvents())
			{
				groups.put(event.getEventId(), group.getGroupId());
			}
		}
		this.groupIdByEventId = Collections.unmodifiableMap(groups);
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
		return fromObservations(events, confirmations, RewardEdits.empty(),
			sessionId, asOfUtc, calendarZone);
	}

	public static RewardSummary fromObservations(List<SavedLootEvent> events,
		List<NoLootConfirmation> confirmations, RewardEdits edits, String sessionId,
		Instant asOfUtc, ZoneId calendarZone)
	{
		return fromObservations(events, confirmations, edits, Collections.emptyList(),
			sessionId, asOfUtc, calendarZone);
	}

	public static RewardSummary fromObservations(List<SavedLootEvent> events,
		List<NoLootConfirmation> confirmations, RewardEdits edits, List<ManualReward> manualRewards,
		String sessionId, Instant asOfUtc, ZoneId calendarZone)
	{
		Objects.requireNonNull(events);
		Objects.requireNonNull(confirmations);
		Objects.requireNonNull(sessionId);
		Objects.requireNonNull(asOfUtc);
		Objects.requireNonNull(calendarZone);
		Objects.requireNonNull(edits);
		Objects.requireNonNull(manualRewards);
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
			event -> true, noLootClickIds, edits, manualRewards, reward -> true));
		results.put(RewardScope.CURRENT_RUN, RewardStatistics.summarize(events,
			event -> sessionId.equals(event.getSessionId()), noLootClickIds, edits,
			manualRewards, reward -> sessionId.equals(reward.getSessionId())));
		results.put(RewardScope.TODAY, calendarStats(events, manualRewards,
			today, asOfUtc, calendarZone, noLootClickIds, edits));
		results.put(RewardScope.THIS_WEEK, calendarStats(events, manualRewards,
			weekStart, asOfUtc, calendarZone, noLootClickIds, edits));
		results.put(RewardScope.THIS_MONTH, calendarStats(events, manualRewards,
			today.withDayOfMonth(1), asOfUtc, calendarZone, noLootClickIds, edits));
		results.put(RewardScope.THIS_YEAR, calendarStats(events, manualRewards,
			today.withDayOfYear(1), asOfUtc, calendarZone, noLootClickIds, edits));
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
		EnumMap<RewardScope, List<RewardHistoryEntry>> histories = buildHistory(events, manualRewards,
			uniqueConfirmations, sessionId, asOfUtc, calendarZone, today, weekStart);
		return new RewardSummary(results, noLootCounts, histories, calendarZone,
			events, manualRewards, sessionId, asOfUtc, edits);
	}

	private static EnumMap<RewardScope, List<RewardHistoryEntry>> buildHistory(
		List<SavedLootEvent> events, List<ManualReward> manualRewards,
		Map<String, NoLootConfirmation> confirmations,
		String sessionId, Instant asOfUtc, ZoneId zone, LocalDate today, LocalDate weekStart)
	{
		Map<String, Instant> clickTimes = new HashMap<>();
		Map<SavedLootEvent, Instant> groupTimes = new IdentityHashMap<>();
		for (LootRewardGroup group : LootRewardGrouper.group(events))
		{
			for (SavedLootEvent event : group.getEvents())
			{
				groupTimes.put(event, group.getObservedAtUtc());
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
			Instant scopeTime = groupTimes.getOrDefault(event, event.getObservedAtUtc());
			allEntries.add(RewardHistoryEntry.loot(event, scopeTime));
		}
		for (NoLootConfirmation confirmation : confirmations.values())
		{
			allEntries.add(RewardHistoryEntry.noLoot(confirmation,
				clickTimes.get(confirmation.getClick().getClickId())));
		}
		for (ManualReward reward : manualRewards)
		{
			allEntries.add(RewardHistoryEntry.manual(reward));
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

	private static RewardStatistics calendarStats(List<SavedLootEvent> events,
		List<ManualReward> manualRewards, LocalDate startDate,
		Instant asOfUtc, ZoneId zone, Set<String> confirmedNoLootClickIds, RewardEdits edits)
	{
		Instant start = startDate.atStartOfDay(zone).toInstant();
		return RewardStatistics.summarize(events, event -> {
			Instant observedAt = event.getObservedAtUtc();
			return !observedAt.isBefore(start) && !observedAt.isAfter(asOfUtc);
		}, confirmedNoLootClickIds, edits, manualRewards, reward -> {
			Instant recordedAt = reward.getRecordedAtUtc();
			return !recordedAt.isBefore(start) && !recordedAt.isAfter(asOfUtc);
		});
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
	public RewardEdit getEdit(String groupId) { return edits.get(groupId); }
	public RewardEdit getEditForEvent(String eventId)
	{
		String groupId = groupIdByEventId.get(eventId);
		return groupId == null ? null : edits.get(groupId);
	}

	public List<LootRewardGroup> getEditableGroups(RewardScope scope)
	{
		List<LootRewardGroup> groups = new ArrayList<>();
		LocalDate today = asOfUtc.atZone(calendarZone).toLocalDate();
		Instant start;
		switch (scope)
		{
			case TODAY:
				start = today.atStartOfDay(calendarZone).toInstant();
				break;
			case THIS_WEEK:
				start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
					.atStartOfDay(calendarZone).toInstant();
				break;
			case THIS_MONTH:
				start = today.withDayOfMonth(1).atStartOfDay(calendarZone).toInstant();
				break;
			case THIS_YEAR:
				start = today.withDayOfYear(1).atStartOfDay(calendarZone).toInstant();
				break;
			default:
				start = Instant.MIN;
		}
		for (LootRewardGroup group : LootRewardGrouper.group(events))
		{
			if (scope == RewardScope.CURRENT_RUN
				&& !sessionId.equals(group.getLastEvent().getSessionId()))
			{
				continue;
			}
			if (scope.isCalendar()
				&& (group.getObservedAtUtc().isBefore(start)
					|| group.getObservedAtUtc().isAfter(asOfUtc)))
			{
				continue;
			}
			groups.add(group);
		}
		groups.sort(Comparator.comparing(LootRewardGroup::getObservedAtUtc).reversed());
		return Collections.unmodifiableList(groups);
	}

	public List<ManualReward> getManualRewards(RewardScope scope)
	{
		List<ManualReward> included = new ArrayList<>();
		LocalDate today = asOfUtc.atZone(calendarZone).toLocalDate();
		Instant start = scopeStart(scope, today, calendarZone);
		for (ManualReward reward : manualRewards)
		{
			if (scope == RewardScope.CURRENT_RUN && !sessionId.equals(reward.getSessionId()))
			{
				continue;
			}
			if (scope.isCalendar()
				&& (reward.getRecordedAtUtc().isBefore(start) || reward.getRecordedAtUtc().isAfter(asOfUtc)))
			{
				continue;
			}
			included.add(reward);
		}
		included.sort(Comparator.comparing(ManualReward::getRecordedAtUtc).reversed());
		return Collections.unmodifiableList(included);
	}

	private static Instant scopeStart(RewardScope scope, LocalDate today, ZoneId zone)
	{
		switch (scope)
		{
			case TODAY: return today.atStartOfDay(zone).toInstant();
			case THIS_WEEK: return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.atStartOfDay(zone).toInstant();
			case THIS_MONTH: return today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
			case THIS_YEAR: return today.withDayOfYear(1).atStartOfDay(zone).toInstant();
			default: return Instant.MIN;
		}
	}
}
