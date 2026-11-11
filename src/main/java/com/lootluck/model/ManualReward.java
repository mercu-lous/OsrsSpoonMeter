package com.lootluck.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A user-entered reward for a missed loot callback. */
public final class ManualReward
{
	private final String id;
	private final String profileId;
	private final String bossId;
	private final String sessionId;
	private final Instant recordedAtUtc;
	private final long actualGp;
	private final Long expectedGp;
	private final String note;
	private final boolean included;

	public ManualReward(String id, String profileId, String bossId, String sessionId,
		Instant recordedAtUtc, long actualGp, Long expectedGp, String note, boolean included)
	{
		UUID.fromString(id);
		this.id = id;
		this.profileId = Objects.requireNonNull(profileId);
		if (!profileId.matches("[0-9a-f]{64}"))
		{
			throw new IllegalArgumentException("Invalid profile ID");
		}
		this.bossId = Objects.requireNonNull(bossId);
		if (BossRegistry.bosses().stream().noneMatch(boss -> boss.getId().equals(bossId)))
		{
			throw new IllegalArgumentException("Unknown boss ID");
		}
		this.sessionId = Objects.requireNonNull(sessionId);
		this.recordedAtUtc = Objects.requireNonNull(recordedAtUtc);
		if (actualGp < 0 || (expectedGp != null && expectedGp <= 0))
		{
			throw new IllegalArgumentException("Actual GP cannot be negative; expected GP must be positive");
		}
		this.actualGp = actualGp;
		this.expectedGp = expectedGp;
		this.note = Objects.requireNonNull(note).trim();
		if (this.note.length() > 160)
		{
			throw new IllegalArgumentException("Reason must be 160 characters or fewer");
		}
		this.included = included;
	}

	public ManualReward withIncluded(boolean newIncluded)
	{
		return new ManualReward(id, profileId, bossId, sessionId, recordedAtUtc,
			actualGp, expectedGp, note, newIncluded);
	}

	public String getId() { return id; }
	public String getProfileId() { return profileId; }
	public String getBossId() { return bossId; }
	public String getSessionId() { return sessionId; }
	public Instant getRecordedAtUtc() { return recordedAtUtc; }
	public long getActualGp() { return actualGp; }
	public Long getExpectedGp() { return expectedGp; }
	public String getNote() { return note; }
	public boolean isIncluded() { return included; }
}
