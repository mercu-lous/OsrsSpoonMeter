package com.lootluck.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemID;

/** Curated normal-reward table, valued with the same conversion catalog as observed loot. */
public final class MaggotKingExpectedValueService
{
	public static final String SOURCE_VERSION = "WIKI_MAGGOT_KING_OPEN_STOMACH_2026_11_09_V1";
	public static final String SOURCE_URL = "https://oldschool.runescape.wiki/w/Maggot_King";
	private static final MathContext PRECISION = MathContext.DECIMAL128;
	private static final int MAIN_WEIGHT = 159;
	private static final List<Outcome> MAIN = mainTable();

	private MaggotKingExpectedValueService()
	{
	}

	public static ExpectedValueSnapshot capture(MaggotKingConversionCatalog.PriceLookup priceLookup,
		String priceSource, Instant capturedAtUtc)
	{
		Map<Integer, Long> prices = new LinkedHashMap<>();
		MaggotKingConversionCatalog.PriceLookup recorded = id -> prices.computeIfAbsent(id,
			key -> Math.max(0L, priceLookup.price(key)));
		BigDecimal fang = unitValue(ItemID.ELDER_VENATOR_FANG, recorded, priceSource);
		BigDecimal kisten = unitValue(ItemID.CRIMSON_KISTEN, recorded, priceSource);
		BigDecimal mainAverage = BigDecimal.ZERO;
		boolean complete = fang != null && kisten != null;
		for (Outcome outcome : MAIN)
		{
			BigDecimal outcomeValue = BigDecimal.ZERO;
			for (Drop drop : outcome.drops)
			{
				BigDecimal unit = unitValue(drop.itemId, recorded, priceSource);
				if (unit == null)
				{
					complete = false;
				}
				else
				{
					BigDecimal averageQuantity = BigDecimal.valueOf((long) drop.minQuantity + drop.maxQuantity)
						.divide(BigDecimal.valueOf(2), PRECISION);
					outcomeValue = outcomeValue.add(unit.multiply(averageQuantity, PRECISION), PRECISION);
				}
			}
			mainAverage = mainAverage.add(outcomeValue.multiply(BigDecimal.valueOf(outcome.weight), PRECISION)
				.divide(BigDecimal.valueOf(MAIN_WEIGHT), PRECISION), PRECISION);
		}
		Long expectedGp = null;
		if (complete)
		{
			BigDecimal fangRate = BigDecimal.ONE.divide(BigDecimal.valueOf(340), PRECISION);
			BigDecimal kistenRate = BigDecimal.ONE.divide(BigDecimal.valueOf(520), PRECISION);
			BigDecimal commonRate = BigDecimal.ONE.subtract(fangRate, PRECISION).subtract(kistenRate, PRECISION);
			BigDecimal value = fang.multiply(fangRate, PRECISION)
				.add(kisten.multiply(kistenRate, PRECISION), PRECISION)
				.add(mainAverage.multiply(commonRate, PRECISION), PRECISION);
			try
			{
				expectedGp = value.setScale(0, RoundingMode.DOWN).longValueExact();
			}
			catch (ArithmeticException overflow)
			{
				// Keep the snapshot incomplete rather than truncating a large value.
			}
		}
		return new ExpectedValueSnapshot(expectedGp, SOURCE_VERSION, SOURCE_URL,
			MaggotKingConversionCatalog.VERSION, priceSource, capturedAtUtc, prices);
	}

	private static BigDecimal unitValue(int itemId, MaggotKingConversionCatalog.PriceLookup prices,
		String priceSource)
	{
		long directPrice = prices.price(itemId);
		ConversionQuote quote = MaggotKingConversionCatalog.quote(itemId, 1, directPrice,
			true, prices, priceSource);
		if ("NO_GE_CONVERSION".equals(quote.getMethod()))
		{
			return BigDecimal.ZERO;
		}
		if ("DIRECT_GE".equals(quote.getMethod()))
		{
			return BigDecimal.valueOf(quote.getStackGp());
		}
		if (quote.getComponents().isEmpty())
		{
			return null;
		}
		BigDecimal total = BigDecimal.ZERO;
		for (ConversionComponent component : quote.getComponents())
		{
			if (component.getUnitPriceGp() == null)
			{
				return null;
			}
			total = total.add(BigDecimal.valueOf(component.getUnitPriceGp())
				.multiply(BigDecimal.valueOf(component.getWeight()), PRECISION), PRECISION);
		}
		return total.divide(BigDecimal.valueOf(quote.getDenominator()), PRECISION);
	}

	private static List<Outcome> mainTable()
	{
		List<Outcome> outcomes = Arrays.asList(
			new Outcome(41, new Drop(ItemID.STYMPHIKE_TARTARE, 2, 4), new Drop(ItemID.DULL_ANCIENT_MEDAL, 1, 1)),
			new Outcome(15, new Drop(ItemID.UNCUT_RUBY, 10, 13)),
			new Outcome(10, new Drop(ItemID.STYMPHIKE_FEATHER, 51, 74)),
			new Outcome(10, new Drop(ItemID.MORT_MYRE_FUNGUS, 50, 76)),
			new Outcome(10, new Drop(ItemID.LETVEK, 23, 34)),
			new Outcome(5, new Drop(ItemID.RUNITE_ORE, 12, 15)),
			new Outcome(5, new Drop(ItemID.BLOOD_SAC, 8, 11)),
			new Outcome(5, new Drop(ItemID.GOLD_ORE, 24, 32)),
			new Outcome(5, new Drop(ItemID.MITHRIL_ORE, 36, 41)),
			new Outcome(10, new Drop(ItemID.ADAMANTITE_ORE, 30, 40)),
			new Outcome(4, new Drop(ItemID.ORIKALKUM_GRAVEL, 4, 5)),
			new Outcome(2, new Drop(ItemID.VENATOR_FANG, 112, 179)),
			new Outcome(2, new Drop(ItemID.VENATOR_TOOTH, 178, 219)),
			new Outcome(12, new Drop(ItemID.TARNISHED_SPEAR, 1, 1)),
			new Outcome(6, new Drop(ItemID.TARNISHED_RING, 1, 1)),
			new Outcome(3, new Drop(ItemID.TARNISHED_BRACELET, 1, 1)),
			new Outcome(3, new Drop(ItemID.TARNISHED_NECKLACE, 1, 1)),
			new Outcome(3, new Drop(ItemID.TARNISHED_AMULET, 1, 1)),
			new Outcome(2, new Drop(ItemID.TARNISHED_BATTLEAXE, 1, 1)),
			new Outcome(2, new Drop(ItemID.TARNISHED_LONGSWORD, 1, 1)),
			new Outcome(2, new Drop(ItemID.TARNISHED_HALBERD, 1, 1)),
			new Outcome(2, new Drop(ItemID.TARNISHED_2H_SWORD, 1, 1)));
		int weight = outcomes.stream().mapToInt(outcome -> outcome.weight).sum();
		if (weight != MAIN_WEIGHT)
		{
			throw new IllegalStateException("Maggot King main table weights do not total 159");
		}
		return outcomes;
	}

	private static final class Outcome
	{
		private final int weight;
		private final Drop[] drops;

		private Outcome(int weight, Drop... drops)
		{
			this.weight = weight;
			this.drops = drops;
		}
	}

	private static final class Drop
	{
		private final int itemId;
		private final int minQuantity;
		private final int maxQuantity;

		private Drop(int itemId, int minQuantity, int maxQuantity)
		{
			this.itemId = itemId;
			this.minQuantity = minQuantity;
			this.maxQuantity = maxQuantity;
		}
	}
}
