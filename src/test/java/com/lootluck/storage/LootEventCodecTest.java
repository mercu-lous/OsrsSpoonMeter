package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.SavedLootEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class LootEventCodecTest
{
	private final LootEventCodec codec = new LootEventCodec(new Gson());

	@Test
	public void keepsItemizedEventAcrossJsonRoundTrip()
	{
		List<LootItemPreview> items = new ArrayList<>(Arrays.asList(
			new LootItemPreview(33627, 1, "Test item A", 1000L, "RUNELITE_JAGEX"),
			new LootItemPreview(33628, 4, "Test item B", 2000L, "RUNELITE_JAGEX")));
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1",
			Instant.parse("2026-11-09T04:54:25Z"), "Maggot King", "NPC", 0, 1, 42, items);
		items.clear();

		String json = codec.encode(event);
		SavedLootEvent restored = codec.decode(json);

		assertEquals(2, restored.getItems().size());
		assertEquals(33627, restored.getItems().get(0).getItemId());
		assertEquals(4, restored.getItems().get(1).getQuantity());
		assertEquals(Long.valueOf(2000), restored.getItems().get(1).getUnitPriceGp());
		assertEquals(Long.valueOf(9000), restored.getActualGp());
		assertEquals(event.getProfileId(), restored.getProfileId());
		assertEquals(event.getObservedAtUtc(), restored.getObservedAtUtc());
		assertEquals("Maggot King", restored.getSource());
		assertEquals("NPC", restored.getEventType());
		assertTrue(json.contains("\"schemaVersion\":2"));
		assertTrue(json.contains("\"attributionStatus\":\"UNATTRIBUTED\""));
		assertTrue(json.contains("\"valuationStatus\":\"COMPLETE\""));
		assertTrue(json.contains("\"actualGp\":9000"));
		assertTrue(json.contains("\"valuationCapturedAtUtc\":\"2026-11-09T04:54:25Z\""));
		assertTrue(json.contains("\"priceFeedTimestampUtc\":null"));
		assertTrue(json.contains("\"expectedGp\":null"));
		assertFalse(json.contains("account-one"));
	}

	@Test
	public void readsPriorUnvaluedSchemaWithoutInventingPrices()
	{
		String legacy = "{\"schemaVersion\":1,\"eventId\":\"a798a7a2-7b4b-4d82-8e2b-d16a42db942b\","
			+ "\"profileId\":\"profile\",\"sessionId\":\"session\",\"observedAtUtc\":\"2026-11-09T05:23:32Z\","
			+ "\"source\":\"Maggot King\",\"eventType\":\"NPC\",\"combatLevel\":0,\"reportedAmount\":1,"
			+ "\"clientTick\":42,\"items\":[{\"itemId\":450,\"quantity\":39}]}";
		SavedLootEvent restored = codec.decode(legacy);
		assertEquals(39, restored.getItems().get(0).getQuantity());
		assertEquals(null, restored.getItems().get(0).getUnitPriceGp());
		assertEquals(null, restored.getActualGp());
	}

	@Test
	public void leavesPartialValuationWithoutAnActualTotal()
	{
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1",
			Instant.parse("2026-11-09T04:54:25Z"), "Maggot King", "NPC", 0, 1, 42,
			Arrays.asList(new LootItemPreview(450, 39)));
		String json = codec.encode(event);
		assertTrue(json.contains("\"actualGp\":null"));
		assertTrue(json.contains("\"valuationStatus\":\"INCOMPLETE\""));
		assertEquals(null, codec.decode(json).getActualGp());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnknownSchema()
	{
		codec.decode("{\"schemaVersion\":2}");
	}

	@Test
	public void profileIdsAreStableAndDistinct()
	{
		String first = ProfileIds.fromRuneScapeProfileKey("account-one");
		assertEquals(64, first.length());
		assertEquals(first, ProfileIds.fromRuneScapeProfileKey("account-one"));
		assertNotEquals(first, ProfileIds.fromRuneScapeProfileKey("account-two"));
	}
}
