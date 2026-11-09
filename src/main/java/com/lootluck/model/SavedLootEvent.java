package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** An observed loot event, independent of kill or reward attribution. */
public final class SavedLootEvent
{
	private final String eventId;
	private final String profileId;
	private final String sessionId;
	private final Instant observedAtUtc;
	private final String source;
	private final String eventType;
	private final int combatLevel;
	private final int reportedAmount;
	private final int clientTick;
	private final List<LootItemPreview> items;

	public SavedLootEvent(String eventId, String profileId, String sessionId, Instant observedAtUtc,
		String source, String eventType, int combatLevel, int reportedAmount, int clientTick,
		List<LootItemPreview> items)
	{
		this.eventId = Objects.requireNonNull(eventId);
		this.profileId = Objects.requireNonNull(profileId);
		this.sessionId = Objects.requireNonNull(sessionId);
		this.observedAtUtc = Objects.requireNonNull(observedAtUtc);
		this.source = Objects.requireNonNull(source);
		this.eventType = Objects.requireNonNull(eventType);
		this.combatLevel = combatLevel;
		this.reportedAmount = reportedAmount;
		this.clientTick = clientTick;
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
	}

	public String getEventId() { return eventId; }
	public String getProfileId() { return profileId; }
	public String getSessionId() { return sessionId; }
	public Instant getObservedAtUtc() { return observedAtUtc; }
	public String getSource() { return source; }
	public String getEventType() { return eventType; }
	public int getCombatLevel() { return combatLevel; }
	public int getReportedAmount() { return reportedAmount; }
	public int getClientTick() { return clientTick; }
	public List<LootItemPreview> getItems() { return items; }
}
