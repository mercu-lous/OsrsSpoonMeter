package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lootluck.model.BossDefinition;
import com.lootluck.model.BossRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses a snapshot of RuneLite's profile-scoped Loot Tracker aggregate records. */
public final class LootTrackerImportReader
{
	private LootTrackerImportReader() {}

	public static Snapshot read(Gson gson, List<String> records, BossDefinition selectedBoss)
	{
		int kills = 0;
		Map<Integer, Long> quantities = new LinkedHashMap<>();
		for (String json : records)
		{
			if (json == null || json.length() > 1_000_000)
			{
				throw new IllegalArgumentException("Loot Tracker record is unavailable or too large");
			}
			JsonObject record = gson.fromJson(json, JsonObject.class);
			if (record == null || !record.has("name") || !record.has("type")
				|| !record.has("kills") || !record.has("drops")
				|| !record.get("drops").isJsonArray())
			{
				throw new IllegalArgumentException("Unrecognized Loot Tracker record");
			}
			String source = record.get("name").getAsString();
			if (BossRegistry.findBySource(source) != selectedBoss
				|| !("NPC".equals(record.get("type").getAsString())
					|| "EVENT".equals(record.get("type").getAsString())))
			{
				continue;
			}
			int count = record.get("kills").getAsInt();
			if (count < 0)
			{
				throw new IllegalArgumentException("Invalid Loot Tracker kill count");
			}
			kills = Math.addExact(kills, count);
			JsonArray drops = record.getAsJsonArray("drops");
			if (drops.size() % 2 != 0 || drops.size() > 20_000)
			{
				throw new IllegalArgumentException("Invalid Loot Tracker item list");
			}
			for (int index = 0; index < drops.size(); index += 2)
			{
				int id = drops.get(index).getAsInt();
				long quantity = drops.get(index + 1).getAsLong();
				if (id <= 0 || quantity < 0)
				{
					throw new IllegalArgumentException("Invalid Loot Tracker item");
				}
				if (quantity > 0)
				{
					quantities.merge(id, quantity, Math::addExact);
				}
			}
		}
		return new Snapshot(kills, quantities);
	}

	public static final class Snapshot
	{
		private final int kills;
		private final Map<Integer, Long> quantities;

		private Snapshot(int kills, Map<Integer, Long> quantities)
		{
			this.kills = kills;
			this.quantities = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(quantities));
		}

		public int getKills() { return kills; }
		public Map<Integer, Long> getQuantities() { return quantities; }
	}
}
