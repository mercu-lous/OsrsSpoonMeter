package com.lootluck.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-boss views of one profile's saved observations. */
public final class ProfileDashboard
{
	private final Map<BossDefinition, RewardSummary> summaries;

	private ProfileDashboard(Map<BossDefinition, RewardSummary> summaries)
	{
		this.summaries = Collections.unmodifiableMap(new LinkedHashMap<>(summaries));
	}

	public static ProfileDashboard empty(String sessionId, Instant asOfUtc, ZoneId calendarZone)
	{
		return fromObservations(Collections.emptyList(), Collections.emptyList(),
			sessionId, asOfUtc, calendarZone);
	}

	public static ProfileDashboard fromObservations(List<SavedLootEvent> events,
		List<NoLootConfirmation> confirmations, String sessionId, Instant asOfUtc, ZoneId calendarZone)
	{
		return fromObservations(events, confirmations, RewardEdits.empty(),
			sessionId, asOfUtc, calendarZone);
	}

	public static ProfileDashboard fromObservations(List<SavedLootEvent> events,
		List<NoLootConfirmation> confirmations, RewardEdits edits,
		String sessionId, Instant asOfUtc, ZoneId calendarZone)
	{
		Map<BossDefinition, List<SavedLootEvent>> byBoss = new LinkedHashMap<>();
		for (BossDefinition boss : BossRegistry.bosses())
		{
			byBoss.put(boss, new ArrayList<>());
		}
		for (SavedLootEvent event : events)
		{
			BossDefinition boss = BossRegistry.findBySource(event.getSource());
			if (boss != null)
			{
				byBoss.get(boss).add(event);
			}
		}
		Map<BossDefinition, RewardSummary> summaries = new LinkedHashMap<>();
		for (BossDefinition boss : BossRegistry.bosses())
		{
			summaries.put(boss, RewardSummary.fromObservations(byBoss.get(boss),
				boss == BossRegistry.MAGGOT_KING ? confirmations : Collections.emptyList(),
				edits, sessionId, asOfUtc, calendarZone));
		}
		return new ProfileDashboard(summaries);
	}

	public RewardSummary get(BossDefinition boss)
	{
		return summaries.get(boss);
	}

	/** Ranked by GP percentage versus the matched expected value in this scope. */
	public List<BossRankingEntry> getRankings(RewardScope scope)
	{
		List<BossRankingEntry> rankings = new ArrayList<>();
		for (Map.Entry<BossDefinition, RewardSummary> entry : summaries.entrySet())
		{
			RewardStatistics statistics = entry.getValue().get(scope);
			if (statistics.getValuedNormalRewards() > 0 && statistics.getPercentVsExpected() != null)
			{
				rankings.add(new BossRankingEntry(entry.getKey(), statistics));
			}
		}
		rankings.sort(Comparator.comparing(BossRankingEntry::getPercentVsExpected).reversed()
			.thenComparing(ranking -> ranking.getBoss().getName()));
		return Collections.unmodifiableList(rankings);
	}
}
