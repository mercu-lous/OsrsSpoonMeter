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
		root.addProperty("schemaVersion", 2);
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
			entry.add("expectedGpPerKill", reward.getExpectedGpPerKill() == null ? JsonNull.INSTANCE
				: gson.toJsonTree(reward.getExpectedGpPerKill()));
			entry.addProperty("killCountIncrease", reward.getKillCountIncrease());
			entry.addProperty("usesBossDefault", reward.usesBossDefault());
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
		if (root == null || !root.has("schemaVersion")
			|| !root.has("rewards") || !root.get("rewards").isJsonArray())
		{
			throw new IllegalArgumentException("Unsupported manual reward file");
		}
		int version = root.get("schemaVersion").getAsInt();
		if (version != 1 && version != 2)
		{
			throw new IllegalArgumentException("Unsupported manual reward file");
		}
		ManualRewards rewards = ManualRewards.empty();
		for (JsonElement element : root.getAsJsonArray("rewards"))
		{
			JsonObject entry = element.getAsJsonObject();
			JsonElement expected = entry.get(version == 1 ? "expectedGp" : "expectedGpPerKill");
			Long expectedGpPerKill = expected == null || expected.isJsonNull()
				? null : expected.getAsLong();
			String id = entry.get("id").getAsString();
			String profileId = entry.get("profileId").getAsString();
			String bossId = entry.get("bossId").getAsString();
			String sessionId = entry.get("sessionId").getAsString();
			Instant recordedAtUtc = Instant.parse(entry.get("recordedAtUtc").getAsString());
			long actualGp = entry.get("actualGp").getAsLong();
			String note = entry.get("note").getAsString();
			boolean included = entry.get("included").getAsBoolean();
			ManualReward reward = version == 1
				? new ManualReward(id, profileId, bossId, sessionId, recordedAtUtc,
					actualGp, expectedGpPerKill, 1, note, included)
				: ManualReward.fromStored(id, profileId, bossId, sessionId, recordedAtUtc,
					actualGp, expectedGpPerKill, entry.get("killCountIncrease").getAsInt(),
					entry.get("usesBossDefault").getAsBoolean(), note, included);
			if (rewards.get(reward.getId()) != null)
			{
				throw new IllegalArgumentException("Duplicate manual reward");
			}
			rewards = rewards.with(reward);
		}
		return rewards;
	}
}
