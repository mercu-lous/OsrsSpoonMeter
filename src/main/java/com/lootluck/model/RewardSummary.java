package com.lootluck.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** All-time and current-run statistics calculated from the same saved events. */
public final class RewardSummary
{
	private final RewardStatistics allTime;
	private final RewardStatistics currentRun;

	private RewardSummary(RewardStatistics allTime, RewardStatistics currentRun)
	{
		this.allTime = allTime;
		this.currentRun = currentRun;
	}

	public static RewardSummary fromEvents(List<SavedLootEvent> events, String sessionId)
	{
		Objects.requireNonNull(sessionId);
		List<SavedLootEvent> currentEvents = new ArrayList<>();
		for (SavedLootEvent event : events)
		{
			if (sessionId.equals(event.getSessionId()))
			{
				currentEvents.add(event);
			}
		}
		return new RewardSummary(RewardStatistics.summarize(events),
			RewardStatistics.summarize(currentEvents));
	}

	public RewardStatistics getAllTime() { return allTime; }
	public RewardStatistics getCurrentRun() { return currentRun; }
}
