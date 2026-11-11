package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.lootluck.model.ManualReward;
import com.lootluck.model.ManualRewards;
import java.time.Instant;

/** Versioned standalone reward entries, independent of captured loot events. */
public final class ManualRewardCodec
{
	private final Gson gson;

	public ManualRewardCodec(Gson gson)
	{
		this.gson = gson;
	}

	public String encode(ManualRewards rewards)
	{
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", 1);
		JsonArray entries = new JsonArray();
		for (ManualReward reward : rewards.all())
		{
			JsonObject entry = new JsonObject();
			entry.addProperty("id", reward.getId());
			entry.addProperty("profileId", reward.getProfileId());
			entry.addProperty("bossId", reward.getBossId());
			entry.addProperty("sessionId", reward.getSessionId());
			entry.addProperty("recordedAtUtc", reward.getRecordedAtUtc().toString());
			entry.addProperty("actualGp", reward.getActualGp());
			entry.add("expectedGp", reward.getExpectedGp() == null ? JsonNull.INSTANCE
				: gson.toJsonTree(reward.getExpectedGp()));
			entry.addProperty("note", reward.getNote());
			entry.addProperty("included", reward.isIncluded());
			entries.add(entry);
		}
		root.add("rewards", entries);
		return gson.toJson(root);
	}

	public ManualRewards decode(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 1
			|| !root.has("rewards") || !root.get("rewards").isJsonArray())
		{
			throw new IllegalArgumentException("Unsupported manual reward file");
		}
		ManualRewards rewards = ManualRewards.empty();
		for (JsonElement element : root.getAsJsonArray("rewards"))
		{
			JsonObject entry = element.getAsJsonObject();
			JsonElement expected = entry.get("expectedGp");
			ManualReward reward = new ManualReward(entry.get("id").getAsString(),
				entry.get("profileId").getAsString(), entry.get("bossId").getAsString(),
				entry.get("sessionId").getAsString(),
				Instant.parse(entry.get("recordedAtUtc").getAsString()),
				entry.get("actualGp").getAsLong(),
				expected == null || expected.isJsonNull() ? null : expected.getAsLong(),
				entry.get("note").getAsString(), entry.get("included").getAsBoolean());
			if (rewards.get(reward.getId()) != null)
			{
				throw new IllegalArgumentException("Duplicate manual reward");
			}
			rewards = rewards.with(reward);
		}
		return rewards;
	}
}
