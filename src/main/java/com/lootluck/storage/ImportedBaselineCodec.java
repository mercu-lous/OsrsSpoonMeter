package com.lootluck.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lootluck.model.ImportedBaseline;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImportedBaselineCodec
{
	private final Gson gson;

	public ImportedBaselineCodec(Gson gson) { this.gson = gson; }

	public String encode(ImportedBaseline baseline)
	{
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", 1);
		JsonObject data = new JsonObject();
		data.addProperty("profileId", baseline.getProfileId());
		data.addProperty("bossId", baseline.getBossId());
		data.addProperty("importedAtUtc", baseline.getImportedAtUtc().toString());
		data.addProperty("lootTrackerCount", baseline.getLootTrackerCount());
		data.addProperty("kills", baseline.getKills());
		data.addProperty("actualGp", baseline.getActualGp());
		data.addProperty("calculatedItemGp", baseline.getCalculatedItemGp());
		data.addProperty("expectedGpPerKill", baseline.getExpectedGpPerKill());
		data.add("itemQuantities", gson.toJsonTree(baseline.getItemQuantities()));
		data.add("itemUnitGp", gson.toJsonTree(baseline.getItemUnitGp()));
		root.add("baseline", data);
		return gson.toJson(root);
	}

	public ImportedBaseline decode(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("schemaVersion")
			|| root.get("schemaVersion").getAsInt() != 1 || !root.has("baseline"))
		{
			throw new IllegalArgumentException("Unsupported import file");
		}
		JsonObject data = root.getAsJsonObject("baseline");
		Map<Integer, Long> quantities = readMap(data.getAsJsonObject("itemQuantities"));
		Map<Integer, Long> unitGp = readMap(data.getAsJsonObject("itemUnitGp"));
		return new ImportedBaseline(data.get("profileId").getAsString(),
			data.get("bossId").getAsString(), Instant.parse(data.get("importedAtUtc").getAsString()),
			data.has("lootTrackerCount") ? data.get("lootTrackerCount").getAsInt()
				: data.get("kills").getAsInt(), data.get("kills").getAsInt(),
			data.get("actualGp").getAsLong(),
			data.get("calculatedItemGp").getAsLong(), data.get("expectedGpPerKill").getAsLong(),
			quantities, unitGp);
	}

	private static Map<Integer, Long> readMap(JsonObject object)
	{
		Map<Integer, Long> values = new LinkedHashMap<>();
		for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet())
		{
			values.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsLong());
		}
		return values;
	}
}
