package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lootluck.model.RewardEdit;
import com.lootluck.model.RewardEdits;
import java.time.Instant;

/** Versioned profile-level edits; original loot observations are never rewritten. */
public final class RewardEditCodec
{
	private static final int SCHEMA_VERSION = 1;
	private final Gson gson;

	public RewardEditCodec(Gson gson)
	{
		this.gson = gson;
	}

	public String encode(RewardEdits edits)
	{
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", SCHEMA_VERSION);
		JsonArray entries = new JsonArray();
		for (RewardEdit edit : edits.all())
		{
			JsonObject entry = new JsonObject();
			entry.addProperty("groupId", edit.getGroupId());
			entry.addProperty("excluded", edit.isExcluded());
			entry.addProperty("gpCorrection", edit.getGpCorrection());
			entry.addProperty("note", edit.getNote());
			entry.addProperty("editedAtUtc", edit.getEditedAtUtc().toString());
			entries.add(entry);
		}
		root.add("edits", entries);
		return gson.toJson(root);
	}

	public RewardEdits decode(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("schemaVersion")
			|| root.get("schemaVersion").getAsInt() != SCHEMA_VERSION
			|| !root.has("edits") || !root.get("edits").isJsonArray())
		{
			throw new IllegalArgumentException("Unsupported reward edit file");
		}
		RewardEdits edits = RewardEdits.empty();
		for (JsonElement element : root.getAsJsonArray("edits"))
		{
			JsonObject entry = element.getAsJsonObject();
			RewardEdit edit = new RewardEdit(entry.get("groupId").getAsString(),
				entry.get("excluded").getAsBoolean(), entry.get("gpCorrection").getAsLong(),
				entry.get("note").getAsString(), Instant.parse(entry.get("editedAtUtc").getAsString()));
			if (edits.get(edit.getGroupId()) != null)
			{
				throw new IllegalArgumentException("Duplicate reward edit");
			}
			edits = edits.with(edit);
		}
		return edits;
	}
}
