package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.SavedLootEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Explicit versioned JSON avoids serializing RuneLite or JDK internals. */
public final class LootEventCodec
{
	public static final int SCHEMA_VERSION = 1;
	private final Gson gson;

	public LootEventCodec(Gson gson)
	{
		this.gson = gson;
	}

	public String encode(SavedLootEvent event)
	{
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", SCHEMA_VERSION);
		root.addProperty("eventId", event.getEventId());
		root.addProperty("profileId", event.getProfileId());
		root.addProperty("sessionId", event.getSessionId());
		root.addProperty("observedAtUtc", event.getObservedAtUtc().toString());
		root.addProperty("source", event.getSource());
		root.addProperty("eventType", event.getEventType());
		root.addProperty("combatLevel", event.getCombatLevel());
		root.addProperty("reportedAmount", event.getReportedAmount());
		root.addProperty("clientTick", event.getClientTick());
		root.addProperty("attributionStatus", "UNATTRIBUTED");
		root.addProperty("rewardMethod", "UNKNOWN");
		root.addProperty("valuationStatus", "UNVALUED");
		JsonArray items = new JsonArray();
		for (LootItemPreview item : event.getItems())
		{
			JsonObject entry = new JsonObject();
			entry.addProperty("itemId", item.getItemId());
			entry.addProperty("quantity", item.getQuantity());
			items.add(entry);
		}
		root.add("items", items);
		return gson.toJson(root);
	}

	public SavedLootEvent decode(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null)
		{
			throw new IllegalArgumentException("Empty loot event JSON");
		}
		if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION)
		{
			throw new IllegalArgumentException("Unsupported loot event schema");
		}
		String eventId = requiredString(root, "eventId");
		UUID.fromString(eventId);
		List<LootItemPreview> items = new ArrayList<>();
		JsonArray itemArray = root.getAsJsonArray("items");
		if (itemArray == null)
		{
			throw new IllegalArgumentException("Missing items");
		}
		for (JsonElement element : itemArray)
		{
			JsonObject item = element.getAsJsonObject();
			items.add(new LootItemPreview(requiredInt(item, "itemId"), requiredInt(item, "quantity")));
		}
		return new SavedLootEvent(eventId, requiredString(root, "profileId"),
			requiredString(root, "sessionId"), Instant.parse(requiredString(root, "observedAtUtc")),
			requiredString(root, "source"), requiredString(root, "eventType"),
			requiredInt(root, "combatLevel"), requiredInt(root, "reportedAmount"),
			requiredInt(root, "clientTick"), items);
	}

	private static String requiredString(JsonObject object, String key)
	{
		JsonElement value = object.get(key);
		if (value == null || value.isJsonNull())
		{
			throw new IllegalArgumentException("Missing " + key);
		}
		return value.getAsString();
	}

	private static int requiredInt(JsonObject object, String key)
	{
		JsonElement value = object.get(key);
		if (value == null || value.isJsonNull())
		{
			throw new IllegalArgumentException("Missing " + key);
		}
		return value.getAsInt();
	}
}
