package com.lootluck.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** A Take-eggs no-loot result supported by the game's specific chat message. */
public final class NoLootConfirmation
{
	public static final String GAME_MESSAGE = "The eggs pop as you try to take them.";
	private final RewardClickCandidate click;
	private final String sessionId;
	private final Instant observedAtUtc;
	private final int clientTick;
	private final String chatType;

	public NoLootConfirmation(RewardClickCandidate click, String sessionId,
		Instant observedAtUtc, int clientTick, String chatType)
	{
		this.click = Objects.requireNonNull(click);
		if (!"TAKE_EGGS".equals(click.getMethod()) || !click.matches(click.getProfileId(), clientTick))
		{
			throw new IllegalArgumentException("No-loot message is not linked to a recent Take-eggs click");
		}
		if (!isSupportedChatType(chatType))
		{
			throw new IllegalArgumentException("Unsupported no-loot chat type");
		}
		this.sessionId = Objects.requireNonNull(sessionId);
		this.observedAtUtc = Objects.requireNonNull(observedAtUtc);
		this.clientTick = clientTick;
		this.chatType = chatType;
	}

	public static boolean isNoLootText(String message)
	{
		return message != null && GAME_MESSAGE.toLowerCase(Locale.ROOT)
			.equals(message.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT));
	}

	public static boolean isSupportedChatType(String chatType)
	{
		return "GAMEMESSAGE".equals(chatType) || "SPAM".equals(chatType);
	}

	public RewardClickCandidate getClick() { return click; }
	public String getProfileId() { return click.getProfileId(); }
	public String getSessionId() { return sessionId; }
	public Instant getObservedAtUtc() { return observedAtUtc; }
	public int getClientTick() { return clientTick; }
	public String getChatType() { return chatType; }
}
