package com.lootluck.model;

import java.util.Arrays;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LootValuationTest
{
	@Test
	public void missingPriceStaysNullableWhileCoinsKeepFaceValue()
	{
		assertNull(LootValuation.directUnitGp(ItemID.UNCUT_SAPPHIRE, 0, false, true));
		assertEquals(Long.valueOf(1), LootValuation.directUnitGp(ItemID.COINS, 0, false, false));
		assertEquals(Long.valueOf(0), LootValuation.directUnitGp(ItemID.PRIF_CRYSTAL_SHARD,
			0, true, false));
		assertEquals(Long.valueOf(500), LootValuation.directUnitGp(ItemID.UNCUT_SAPPHIRE,
			500, false, true));

		assertEquals(Long.valueOf(164000), LootValuation.totalGpIfComplete(Arrays.asList(
			new LootItemPreview(ItemID.COINS, 108000, "Coins", 1L, "CURRENCY_FACE_VALUE"),
			new LootItemPreview(ItemID.UNCUT_SAPPHIRE, 60, "Uncut sapphire", 500L, "RUNELITE_JAGEX"),
			new LootItemPreview(ItemID.UNCUT_RUBY, 26, "Uncut ruby", 1000L, "RUNELITE_JAGEX"),
			new LootItemPreview(ItemID.PRIF_CRYSTAL_SHARD, 11, "Crystal shard", 0L,
				"UNTRADEABLE_NO_GE_SALE"))));
		assertNull(LootValuation.totalGpIfComplete(Arrays.asList(
			new LootItemPreview(ItemID.COINS, 108000, "Coins", 1L, "CURRENCY_FACE_VALUE"),
			new LootItemPreview(ItemID.UNCUT_SAPPHIRE, 60, "Uncut sapphire", null, "UNAVAILABLE"))));
	}
}
