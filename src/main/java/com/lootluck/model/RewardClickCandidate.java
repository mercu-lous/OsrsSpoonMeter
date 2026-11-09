package com.lootluck.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A recent menu click that may explain loot, but does not prove a completed reward. */
public final class RewardClickCandidate
{
	private static final int MAX_LOOT_DELAY_TICKS = 5;
	private final String clickId;
	private final String profileId;
	private final String method;
	private final int clientTick;
	private final int targetId;
	private final Instant observedAtUtc;

	public RewardClickCandidate(String clickId, String profileId, String method, int clientTick,
		int targetId, Instant observedAtUtc)
	{
		UUID.fromString(clickId);
		this.clickId = clickId;
		this.profileId = Objects.requireNonNull(profileId);
		if (!"OPEN_STOMACH".equals(method) && !"TAKE_EGGS".equals(method))
		{
			throw new IllegalArgumentException("Unsupported reward method");
		}
		this.method = method;
		this.clientTick = clientTick;
		this.targetId = targetId;
		this.observedAtUtc = Objects.requireNonNull(observedAtUtc);
	}

	public boolean matches(String lootProfileId, int lootTick)
	{
		long delay = (long) lootTick - clientTick;
		return profileId.equals(lootProfileId) && delay >= 0 && delay <= MAX_LOOT_DELAY_TICKS;
	}

	public String getClickId() { return clickId; }
	public String getProfileId() { return profileId; }
	public String getMethod() { return method; }
	public int getClientTick() { return clientTick; }
	public int getTargetId() { return targetId; }
	public Instant getObservedAtUtc() { return observedAtUtc; }
}
