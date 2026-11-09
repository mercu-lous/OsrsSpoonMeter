package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lootluck.model.NoLootConfirmation;
import com.lootluck.model.RewardClickCandidate;
import java.time.Instant;

/** Separate versioned record for a confirmed reward choice with no loot callback. */
public final class NoLootConfirmationCodec
{
	private static final int SCHEMA_VERSION = 1;
	private final Gson gson;

	public NoLootConfirmationCodec(Gson gson)
	{
		this.gson = gson;
	}

	public String encode(NoLootConfirmation confirmation)
	{
		RewardClickCandidate click = confirmation.getClick();
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", SCHEMA_VERSION);
		root.addProperty("confirmationType", "TAKE_EGGS_NO_LOOT_CHAT");
		root.addProperty("profileId", confirmation.getProfileId());
		root.addProperty("sessionId", confirmation.getSessionId());
		root.addProperty("observedAtUtc", confirmation.getObservedAtUtc().toString());
		root.addProperty("clientTick", confirmation.getClientTick());
		root.addProperty("chatType", confirmation.getChatType());
		root.addProperty("message", NoLootConfirmation.GAME_MESSAGE);
		root.addProperty("rewardClickId", click.getClickId());
		root.addProperty("rewardClickTick", click.getClientTick());
		root.addProperty("rewardClickTargetId", click.getTargetId());
		root.addProperty("rewardClickObservedAtUtc", click.getObservedAtUtc().toString());
		return gson.toJson(root);
	}

	public NoLootConfirmation decode(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || requiredInt(root, "schemaVersion") != SCHEMA_VERSION
			|| !"TAKE_EGGS_NO_LOOT_CHAT".equals(requiredString(root, "confirmationType"))
			|| !NoLootConfirmation.isNoLootText(requiredString(root, "message")))
		{
			throw new IllegalArgumentException("Unsupported no-loot confirmation");
		}
		String profileId = requiredString(root, "profileId");
		RewardClickCandidate click = new RewardClickCandidate(requiredString(root, "rewardClickId"),
			profileId, "TAKE_EGGS", requiredInt(root, "rewardClickTick"),
			requiredInt(root, "rewardClickTargetId"),
			Instant.parse(requiredString(root, "rewardClickObservedAtUtc")));
		return new NoLootConfirmation(click, requiredString(root, "sessionId"),
			Instant.parse(requiredString(root, "observedAtUtc")),
			requiredInt(root, "clientTick"), requiredString(root, "chatType"));
	}

	private static String requiredString(JsonObject root, String key)
	{
		JsonElement value = root.get(key);
		if (value == null || value.isJsonNull())
		{
			throw new IllegalArgumentException("Missing " + key);
		}
		return value.getAsString();
	}

	private static int requiredInt(JsonObject root, String key)
	{
		JsonElement value = root.get(key);
		if (value == null || value.isJsonNull())
		{
			throw new IllegalArgumentException("Missing " + key);
		}
		return value.getAsInt();
	}
}
