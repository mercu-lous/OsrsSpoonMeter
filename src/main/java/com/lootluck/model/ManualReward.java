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
	private final Long expectedGpPerKill;
	private final Long expectedGpTotal;
	private final int killCountIncrease;
	private final boolean usesBossDefault;
	private final String note;
	private final boolean included;

	public ManualReward(String id, String profileId, String bossId, String sessionId,
		Instant recordedAtUtc, long actualGp, Long expectedGp, String note, boolean included)
	{
		this(id, profileId, bossId, sessionId, recordedAtUtc, actualGp, expectedGp,
			1, note, included);
	}

	public ManualReward(String id, String profileId, String bossId, String sessionId,
		Instant recordedAtUtc, long actualGp, Long enteredExpectedGpPerKill,
		int killCountIncrease, String note, boolean included)
	{
		this(id, profileId, bossId, sessionId, recordedAtUtc, actualGp,
			enteredExpectedGpPerKill == null ? defaultExpectedGpPerKill(bossId) : enteredExpectedGpPerKill,
			killCountIncrease, enteredExpectedGpPerKill == null, note, included);
	}

	public static ManualReward fromStored(String id, String profileId, String bossId, String sessionId,
		Instant recordedAtUtc, long actualGp, Long expectedGpPerKill,
		int killCountIncrease, boolean usesBossDefault, String note, boolean included)
	{
		return new ManualReward(id, profileId, bossId, sessionId, recordedAtUtc, actualGp,
			expectedGpPerKill, killCountIncrease, usesBossDefault, note, included);
	}

	private ManualReward(String id, String profileId, String bossId, String sessionId,
		Instant recordedAtUtc, long actualGp, Long expectedGpPerKill,
		int killCountIncrease, boolean usesBossDefault, String note, boolean included)
	{
		UUID.fromString(id);
		this.id = id;
		this.profileId = Objects.requireNonNull(profileId);
		if (!profileId.matches("[0-9a-f]{64}"))
		{
			throw new IllegalArgumentException("Invalid profile ID");
		}
		this.bossId = Objects.requireNonNull(bossId);
		findBoss(bossId);
		this.sessionId = Objects.requireNonNull(sessionId);
		this.recordedAtUtc = Objects.requireNonNull(recordedAtUtc);
		if (actualGp < 0 || (expectedGpPerKill != null && expectedGpPerKill <= 0))
		{
			throw new IllegalArgumentException("Actual GP cannot be negative; expected GP per kill must be positive");
		}
		if (killCountIncrease < 1 || killCountIncrease > 1_000_000)
		{
			throw new IllegalArgumentException("Manual kill count must be between 1 and 1,000,000");
		}
		this.actualGp = actualGp;
		this.expectedGpPerKill = expectedGpPerKill;
		this.killCountIncrease = killCountIncrease;
		this.usesBossDefault = usesBossDefault;
		try
		{
			this.expectedGpTotal = expectedGpPerKill == null ? null
				: Math.multiplyExact(expectedGpPerKill, killCountIncrease);
		}
		catch (ArithmeticException overflow)
		{
			throw new IllegalArgumentException("Expected GP times kill count is too large", overflow);
		}
		this.note = Objects.requireNonNull(note).trim();
		if (this.note.length() > 160)
		{
			throw new IllegalArgumentException("Reason must be 160 characters or fewer");
		}
		this.included = included;
	}

	public ManualReward withIncluded(boolean newIncluded)
	{
		return fromStored(id, profileId, bossId, sessionId, recordedAtUtc,
			actualGp, expectedGpPerKill, killCountIncrease, usesBossDefault, note, newIncluded);
	}

	public static Long defaultExpectedGpPerKill(BossDefinition boss)
	{
		BossRules rules = boss.getRules();
		return rules instanceof FixedExpectedBossRules
			? ((FixedExpectedBossRules) rules).getExpectedGp() : null;
	}

	private static Long defaultExpectedGpPerKill(String bossId)
	{
		return defaultExpectedGpPerKill(findBoss(bossId));
	}

	private static BossDefinition findBoss(String bossId)
	{
		return BossRegistry.bosses().stream()
			.filter(boss -> boss.getId().equals(bossId))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown boss ID"));
	}

	public String getId() { return id; }
	public String getProfileId() { return profileId; }
	public String getBossId() { return bossId; }
	public String getSessionId() { return sessionId; }
	public Instant getRecordedAtUtc() { return recordedAtUtc; }
	public long getActualGp() { return actualGp; }
	public Long getExpectedGp() { return expectedGpTotal; }
	public Long getExpectedGpPerKill() { return expectedGpPerKill; }
	public int getKillCountIncrease() { return killCountIncrease; }
	public boolean usesBossDefault() { return usesBossDefault; }
	public String getNote() { return note; }
	public boolean isIncluded() { return included; }
}
