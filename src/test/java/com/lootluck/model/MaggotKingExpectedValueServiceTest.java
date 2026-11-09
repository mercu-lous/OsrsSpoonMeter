package com.lootluck.model;

import java.time.Instant;
import net.runelite.api.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MaggotKingExpectedValueServiceTest
{
	@Test
	public void calculatesNormalRewardAverageFromTheFullTable()
	{
		ExpectedValueSnapshot result = MaggotKingExpectedValueService.capture(id -> 1000,
			"RUNELITE_WIKI_ADJUSTED", Instant.parse("2026-11-09T09:00:00Z"));
		assertEquals(Long.valueOf(20060), result.getExpectedGp());
		assertEquals(MaggotKingConversionCatalog.VERSION, result.getConversionCatalogVersion());
		assertEquals(Long.valueOf(1000), result.getInputPrices().get(ItemID.VIAL_OF_BLOOD_22446));
		assertTrue(result.getInputPrices().containsKey(ItemID.ETCHED_ELDER_VENATOR_FANG));
	}

	@Test
	public void missingRarePriceKeepsExpectedValueUnavailable()
	{
		ExpectedValueSnapshot result = MaggotKingExpectedValueService.capture(
			id -> id == ItemID.CRIMSON_KISTEN ? 0 : 1000,
			"RUNELITE_JAGEX", Instant.parse("2026-11-09T09:00:00Z"));
		assertNull(result.getExpectedGp());
		assertEquals(Long.valueOf(0), result.getInputPrices().get(ItemID.CRIMSON_KISTEN));
	}
}
