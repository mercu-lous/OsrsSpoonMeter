package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
	public static final int SCHEMA_VERSION = 2;
	private final Gson gson;

	public LootEventCodec(Gson gson)
	{
		this.gson = gson.newBuilder().serializeNulls().create();
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
		Long actualGp = event.getActualGp();
		root.addProperty("valuationCapturedAtUtc", event.getObservedAtUtc().toString());
		root.add("priceFeedTimestampUtc", JsonNull.INSTANCE);
		root.addProperty("valuationStatus", actualGp == null ? "INCOMPLETE" : "COMPLETE");
		root.add("actualGp", actualGp == null ? JsonNull.INSTANCE : gson.toJsonTree(actualGp));
		root.add("expectedGp", JsonNull.INSTANCE);
		root.addProperty("expectedSource", "NOT_APPLIED");
		JsonArray items = new JsonArray();
		for (LootItemPreview item : event.getItems())
		{
			JsonObject entry = new JsonObject();
			entry.addProperty("itemId", item.getItemId());
			entry.addProperty("quantity", item.getQuantity());
			entry.addProperty("name", item.getName());
			entry.add("unitPriceGp", item.getUnitPriceGp() == null ? JsonNull.INSTANCE : gson.toJsonTree(item.getUnitPriceGp()));
			entry.addProperty("priceSource", item.getPriceSource());
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
		int version = requiredInt(root, "schemaVersion");
		if (version != 1 && version != SCHEMA_VERSION)
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
			int itemId = requiredInt(item, "itemId");
			int quantity = requiredInt(item, "quantity");
			items.add(version == 1
				? new LootItemPreview(itemId, quantity)
				: new LootItemPreview(itemId, quantity, requiredString(item, "name"),
					optionalLong(item, "unitPriceGp"), requiredString(item, "priceSource")));
		}
		SavedLootEvent event = new SavedLootEvent(eventId, requiredString(root, "profileId"),
			requiredString(root, "sessionId"), Instant.parse(requiredString(root, "observedAtUtc")),
			requiredString(root, "source"), requiredString(root, "eventType"),
			requiredInt(root, "combatLevel"), requiredInt(root, "reportedAmount"),
			requiredInt(root, "clientTick"), items);
		if (version == SCHEMA_VERSION && !java.util.Objects.equals(event.getActualGp(), optionalLong(root, "actualGp")))
		{
			throw new IllegalArgumentException("Loot event total does not match item prices");
		}
		return event;
	}

	private static Long optionalLong(JsonObject object, String key)
	{
		JsonElement value = object.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsLong();
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
