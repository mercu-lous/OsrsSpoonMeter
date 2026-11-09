package com.lootluck.model;

import java.util.Arrays;
import net.runelite.api.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MaggotKingConversionCatalogTest
{
	@Test
	public void pricesAllNineTarnishedPolishRecipes()
	{
		int[] tarnished = {ItemID.TARNISHED_SPEAR, ItemID.TARNISHED_LONGSWORD,
			ItemID.TARNISHED_2H_SWORD, ItemID.TARNISHED_BATTLEAXE, ItemID.TARNISHED_HALBERD,
			ItemID.TARNISHED_RING, ItemID.TARNISHED_BRACELET, ItemID.TARNISHED_NECKLACE,
			ItemID.TARNISHED_AMULET};
		for (int itemId : tarnished)
		{
			ConversionQuote quote = MaggotKingConversionCatalog.quote(itemId, 2, 0, false,
				outputId -> 1000, "RUNELITE_JAGEX");
			assertEquals("POLISH_EXPECTED", quote.getMethod());
			assertEquals(Long.valueOf(2000), quote.getStackGp());
			assertEquals(3, quote.getComponents().size());
		}
	}

	@Test
	public void weightsSpearOutputsAndPreservesMissingPrice()
	{
		ConversionQuote quote = MaggotKingConversionCatalog.quote(ItemID.TARNISHED_SPEAR, 1,
			0, false, outputId -> outputId == ItemID.RUNE_SPEAR ? 10000
				: outputId == ItemID.ADAMANT_SPEAR ? 1000 : 100, "RUNELITE_JAGEX");
		assertEquals(Long.valueOf(7210), quote.getStackGp());
		assertEquals(7, quote.getComponents().get(0).getWeight());
		assertEquals(10, quote.getDenominator());
		assertEquals(Long.valueOf(8337), MaggotKingConversionCatalog.quote(ItemID.TARNISHED_SPEAR,
			1, 0, false, outputId -> outputId == ItemID.RUNE_SPEAR ? 11661
				: outputId == ItemID.ADAMANT_SPEAR ? 757 : 234,
			"RUNELITE_JAGEX").getStackGp());
		assertNull(MaggotKingConversionCatalog.quote(ItemID.TARNISHED_SPEAR, 1, 0,
			false, outputId -> outputId == ItemID.RUNE_SPEAR ? 0 : 1000,
			"RUNELITE_JAGEX").getStackGp());
	}

	@Test
	public void pricesFangAndFractionalBloodSacs()
	{
		ConversionQuote fang = MaggotKingConversionCatalog.quote(ItemID.ELDER_VENATOR_FANG,
			1, 0, false, outputId -> outputId == ItemID.ETCHED_ELDER_VENATOR_FANG ? 123456789 : 0,
			"RUNELITE_JAGEX");
		assertEquals(Long.valueOf(123456789), fang.getStackGp());
		ConversionQuote sacs = MaggotKingConversionCatalog.quote(ItemID.BLOOD_SAC,
			8, 0, false, outputId -> outputId == ItemID.VIAL_OF_BLOOD_22446 ? 8300 : 0,
			"RUNELITE_JAGEX");
		assertEquals(Long.valueOf(1328), sacs.getStackGp());
		assertEquals(50, sacs.getDenominator());
	}

	@Test
	public void distinguishesKnownZeroFromUnsupportedAndMissingPrices()
	{
		assertEquals(Long.valueOf(0), MaggotKingConversionCatalog.quote(ItemID.DULL_ANCIENT_MEDAL,
			1, 0, false, id -> 0, "RUNELITE_JAGEX").getStackGp());
		assertNull(MaggotKingConversionCatalog.quote(999999, 1, 0, false,
			id -> 0, "RUNELITE_JAGEX").getStackGp());
		assertNull(MaggotKingConversionCatalog.quote(999998, 1, 0, true,
			id -> 0, "RUNELITE_JAGEX").getStackGp());
		assertEquals(Long.valueOf(1500), MaggotKingConversionCatalog.quote(450, 3, 500,
			true, id -> 0, "RUNELITE_JAGEX").getStackGp());
	}

	@Test
	public void refusesPartialAfterConversionTotals()
	{
		ConversionQuote medal = MaggotKingConversionCatalog.quote(ItemID.DULL_ANCIENT_MEDAL,
			1, 0, false, id -> 0, "RUNELITE_JAGEX");
		ConversionQuote ore = MaggotKingConversionCatalog.quote(450, 2, 500,
			true, id -> 0, "RUNELITE_JAGEX");
		LootItemPreview first = new LootItemPreview(ItemID.DULL_ANCIENT_MEDAL, 1,
			"Dull ancient medal", 0L, "UNTRADEABLE_NO_GE_SALE", 30L, medal);
		LootItemPreview second = new LootItemPreview(450, 2, "Adamantite ore", 500L,
			"RUNELITE_JAGEX", 240L, ore);
		assertEquals(Long.valueOf(1000), LootValuation.totalConvertedGpIfComplete(Arrays.asList(first, second)));
		LootItemPreview unknown = new LootItemPreview(999999, 1, "Unknown", 0L,
			"UNTRADEABLE_NO_GE_SALE", null, MaggotKingConversionCatalog.quote(999999, 1,
				0, false, id -> 0, "RUNELITE_JAGEX"));
		assertNull(LootValuation.totalConvertedGpIfComplete(Arrays.asList(first, unknown)));
	}
}
