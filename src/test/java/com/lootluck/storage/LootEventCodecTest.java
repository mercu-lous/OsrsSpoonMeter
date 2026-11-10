package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.ConversionComponent;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.DirectGeConversion;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.MaggotKingConversionCatalog;
import com.lootluck.model.MaggotKingExpectedValueService;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.SavedLootEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
		assertTrue(json.contains("\"schemaVersion\":7"));
		assertTrue(json.contains("\"attributionStatus\":\"UNATTRIBUTED\""));
		assertTrue(json.contains("\"valuationStatus\":\"COMPLETE\""));
		assertTrue(json.contains("\"valuationMethod\":\"RUNELITE_DIRECT_GE_UNTRADEABLE_ZERO_V1\""));
		assertTrue(json.contains("\"actualGp\":9000"));
		assertTrue(json.contains("\"valuationCapturedAtUtc\":\"2026-11-09T04:54:25Z\""));
		assertTrue(json.contains("\"priceFeedTimestampUtc\":null"));
		assertTrue(json.contains("\"expectedGp\":null"));
		assertFalse(json.contains("account-one"));
	}

	@Test
	public void storesGenericBossConversionVersionWithoutExpectedGp()
	{
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1",
			Instant.parse("2026-11-09T04:54:25Z"), "Vorkath", "NPC", 0, 1, 42,
			Arrays.asList(new LootItemPreview(450, 1, "Test", 100L, "RUNELITE_JAGEX",
				null, new ConversionQuote("DIRECT_GE", 100L, null, "RUNELITE_JAGEX", 1,
					java.util.Collections.emptyList()))));
		String json = codec.encode(event);
		assertTrue(json.contains("\"conversionCatalogVersion\":\"" + DirectGeConversion.VERSION + "\""));
		assertTrue(json.contains("\"expectedSource\":\"NOT_APPLIED\""));
		assertEquals("Vorkath", codec.decode(json).getSource());
		assertEquals(Long.valueOf(100), codec.decode(json).getConvertedGp());
	}

	@Test
	public void storesFixedBossExpectedValueWithoutMenuClick()
	{
		Instant observedAt = Instant.parse("2026-11-10T12:00:00Z");
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1", observedAt,
			"Crystalline Hunllef", "NPC", 0, 1, 42,
			Arrays.asList(new LootItemPreview(450, 1, "Test", 100L, "RUNELITE_JAGEX",
				null, new ConversionQuote("DIRECT_GE", 100L, null, "RUNELITE_JAGEX", 1,
					java.util.Collections.emptyList()))), null,
			BossRegistry.GAUNTLET.getRules().expected(null, id -> 1L, "RUNELITE_JAGEX", observedAt));
		SavedLootEvent restored = codec.decode(codec.encode(event));
		assertEquals(Long.valueOf(150815), restored.getExpectedSnapshot().getExpectedGp());
		assertEquals(null, restored.getRewardClick());
		assertEquals("Crystalline Hunllef", restored.getSource());
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

	@Test
	public void countsUntradeableItemsAsZeroGeValue()
	{
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1",
			Instant.parse("2026-11-09T04:54:25Z"), "Maggot King", "NPC", 0, 1, 42,
			Arrays.asList(new LootItemPreview(33627, 1, "Dull ancient medal", 0L, "UNTRADEABLE_NO_GE_SALE"),
				new LootItemPreview(33628, 4, "Stymphike tartare", 0L, "UNTRADEABLE_NO_GE_SALE")));
		String json = codec.encode(event);
		assertTrue(json.contains("\"actualGp\":0"));
		assertTrue(json.contains("\"valuationStatus\":\"COMPLETE\""));
		assertEquals(Long.valueOf(0), codec.decode(json).getActualGp());
	}

	@Test
	public void keepsAlchemyReferenceSeparateFromDirectGeValue()
	{
		ConversionQuote conversion = new ConversionQuote("POLISH_EXPECTED", 7210L,
			"https://oldschool.runescape.wiki/w/Tarnished_spear", "RUNELITE_JAGEX", 10,
			Arrays.asList(new ConversionComponent(1247, 7, 10000L),
				new ConversionComponent(1245, 2, 1000L),
				new ConversionComponent(1243, 1, 100L)));
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			ProfileIds.fromRuneScapeProfileKey("account-one"), "session-1",
			Instant.parse("2026-11-09T07:48:33Z"), "Maggot King", "NPC", 0, 1, 730,
			Arrays.asList(new LootItemPreview(33679, 1, "Tarnished spear", 0L,
				"UNTRADEABLE_NO_GE_SALE", 12000L, conversion)));
		String json = codec.encode(event);
		SavedLootEvent restored = codec.decode(json);
		assertEquals(Long.valueOf(0), restored.getActualGp());
		assertEquals(Long.valueOf(12000), restored.getItems().get(0).getHighAlchGp());
		assertEquals(Long.valueOf(7210), restored.getConvertedGp());
		assertEquals(3, restored.getItems().get(0).getConversionQuote().getComponents().size());
		assertEquals(Long.valueOf(10000), restored.getItems().get(0).getConversionQuote()
			.getComponents().get(0).getUnitPriceGp());
		assertEquals(null, codec.decode(json.replace("\"schemaVersion\":7", "\"schemaVersion\":5"))
			.getConvertedGp());
	}

	@Test
	public void readsPriorSchemaTwoWithMissingPricesUnchanged()
	{
		String prior = "{\"schemaVersion\":2,\"eventId\":\"a798a7a2-7b4b-4d82-8e2b-d16a42db942b\","
			+ "\"profileId\":\"profile\",\"sessionId\":\"session\",\"observedAtUtc\":\"2026-11-09T05:23:32Z\","
			+ "\"source\":\"Maggot King\",\"eventType\":\"NPC\",\"combatLevel\":0,\"reportedAmount\":1,"
			+ "\"clientTick\":42,\"actualGp\":null,\"items\":[{\"itemId\":33627,\"quantity\":1,"
			+ "\"name\":\"Dull ancient medal\",\"unitPriceGp\":null,\"priceSource\":\"UNAVAILABLE\"}]}";
		SavedLootEvent restored = codec.decode(prior);
		assertEquals(null, restored.getItems().get(0).getUnitPriceGp());
		assertEquals(null, restored.getActualGp());
		assertEquals(null, codec.decode(prior.replace("\"schemaVersion\":2", "\"schemaVersion\":3")).getRewardClick());
	}

	@Test
	public void correlatesRecentClickWithoutConfirmingKill()
	{
		String profile = ProfileIds.fromRuneScapeProfileKey("account-one");
		RewardClickCandidate click = new RewardClickCandidate("d5f88875-332c-429c-8adc-59b44b669a41",
			profile, "OPEN_STOMACH", 339, 6514, Instant.parse("2026-11-09T06:12:22Z"));
		assertTrue(click.matches(profile, 341));
		assertFalse(click.matches(profile, 338));
		assertFalse(click.matches(profile, 345));
		assertFalse(click.matches(ProfileIds.fromRuneScapeProfileKey("account-two"), 341));

		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			profile, "session-1", Instant.parse("2026-11-09T06:12:23Z"),
			"Maggot King", "NPC", 0, 1, 341,
			Arrays.asList(new LootItemPreview(33627, 1, "Dull ancient medal", 0L, "UNTRADEABLE_NO_GE_SALE")), click);
		String json = codec.encode(event);
		assertTrue(json.contains("\"attributionStatus\":\"CLICK_CORRELATED_UNCONFIRMED\""));
		assertTrue(json.contains("\"rewardMethod\":\"OPEN_STOMACH\""));
		assertEquals(click.getClickId(), codec.decode(json).getRewardClick().getClickId());
		assertEquals(339, codec.decode(json).getRewardClick().getClientTick());
		assertEquals(click.getClickId(), codec.decode(json.replace("\"schemaVersion\":7", "\"schemaVersion\":4"))
			.getRewardClick().getClickId());
	}

	@Test
	public void freezesExpectedValueAndItsPriceInputs()
	{
		String profile = ProfileIds.fromRuneScapeProfileKey("account-one");
		RewardClickCandidate click = new RewardClickCandidate("d5f88875-332c-429c-8adc-59b44b669a41",
			profile, "OPEN_STOMACH", 100, 42, Instant.parse("2026-11-09T09:00:00Z"));
		Map<Integer, Long> prices = new LinkedHashMap<>();
		prices.put(33636, 123456789L);
		prices.put(33631, 50000000L);
		ExpectedValueSnapshot expected = new ExpectedValueSnapshot(489466L,
			MaggotKingExpectedValueService.SOURCE_VERSION, MaggotKingExpectedValueService.SOURCE_URL,
			MaggotKingConversionCatalog.VERSION, "RUNELITE_WIKI_ADJUSTED",
			Instant.parse("2026-11-09T09:00:01Z"), prices);
		SavedLootEvent event = new SavedLootEvent("a798a7a2-7b4b-4d82-8e2b-d16a42db942b",
			profile, "session-1", Instant.parse("2026-11-09T09:00:01Z"),
			"Maggot King", "NPC", 0, 1, 102,
			Arrays.asList(new LootItemPreview(33627, 1, "Dull ancient medal", 0L,
				"UNTRADEABLE_NO_GE_SALE", 30L, new ConversionQuote("NO_GE_CONVERSION", 0L,
					null, null, 1, java.util.Collections.emptyList()))), click, expected);
		String json = codec.encode(event);
		SavedLootEvent restored = codec.decode(json);
		assertEquals(Long.valueOf(489466), restored.getExpectedSnapshot().getExpectedGp());
		assertEquals(Long.valueOf(123456789), restored.getExpectedSnapshot().getInputPrices().get(33636));
		assertEquals(null, codec.decode(json.replace("\"schemaVersion\":7", "\"schemaVersion\":6"))
			.getExpectedSnapshot());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnknownSchema()
	{
		codec.decode("{\"schemaVersion\":8}");
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
