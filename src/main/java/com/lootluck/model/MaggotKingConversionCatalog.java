package com.lootluck.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemID;

/** Versioned Maggot King conversion recipes; prices are supplied at loot capture time. */
public final class MaggotKingConversionCatalog
{
	public static final String VERSION = "MAGGOT_KING_CONVERSIONS_V2_ZERO_MISSING";
	private static final Map<Integer, Recipe> RECIPES = recipes();

	private MaggotKingConversionCatalog()
	{
	}

	public static ConversionQuote quote(int itemId, int quantity, long directPrice,
		boolean tradeable, ItemPriceLookup prices, String priceSource)
	{
		if (quantity < 0)
		{
			return empty("INVALID_QUANTITY", priceSource);
		}
		Recipe recipe = RECIPES.get(itemId);
		if (recipe != null)
		{
			List<ConversionComponent> components = new ArrayList<>();
			BigInteger weightedPrice = BigInteger.ZERO;
			boolean complete = true;
			for (int i = 0; i < recipe.outputIds.length; i++)
			{
				long price = prices.price(recipe.outputIds[i]);
				Long captured = price > 0 ? price : null;
				components.add(new ConversionComponent(recipe.outputIds[i], recipe.weights[i], captured));
				if (captured == null)
				{
					complete = false;
				}
				else
				{
					weightedPrice = weightedPrice.add(BigInteger.valueOf(captured)
						.multiply(BigInteger.valueOf(recipe.weights[i])));
				}
			}
			Long stackGp = complete ? stackValueInWholeGp(weightedPrice, quantity, recipe.denominator) : 0L;
			return new ConversionQuote(complete ? recipe.method : "MISSING_CONVERSION_PRICE_ASSUMED_ZERO",
				stackGp, recipe.sourceUrl,
				priceSource, recipe.denominator, components);
		}
		if (itemId == ItemID.DULL_ANCIENT_MEDAL || itemId == ItemID.STYMPHIKE_TARTARE)
		{
			return new ConversionQuote("NO_GE_CONVERSION", 0L, null, null, 1, Collections.emptyList());
		}
		if (directPrice > 0)
		{
			try
			{
				return new ConversionQuote("DIRECT_GE", Math.multiplyExact(directPrice, quantity),
					null, priceSource, 1, Collections.emptyList());
			}
			catch (ArithmeticException overflow)
			{
				return empty("OVERFLOW", priceSource);
			}
		}
		return new ConversionQuote(tradeable ? "MISSING_DIRECT_PRICE_ASSUMED_ZERO"
			: "UNSUPPORTED_UNTRADEABLE_ASSUMED_ZERO", 0L, null,
			priceSource, 1, Collections.emptyList());
	}

	private static Long stackValueInWholeGp(BigInteger weightedPrice, int quantity, int denominator)
	{
		BigInteger wholeGp = weightedPrice.multiply(BigInteger.valueOf(quantity))
			.divide(BigInteger.valueOf(denominator));
		return wholeGp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? null : wholeGp.longValue();
	}

	private static ConversionQuote empty(String method, String priceSource)
	{
		return new ConversionQuote(method, null, null, priceSource, 1, Collections.emptyList());
	}

	private static Map<Integer, Recipe> recipes()
	{
		Map<Integer, Recipe> map = new HashMap<>();
		map.put(ItemID.TARNISHED_SPEAR, polish("Tarnished_spear", new int[]{ItemID.RUNE_SPEAR, ItemID.ADAMANT_SPEAR, ItemID.MITHRIL_SPEAR}, new int[]{7, 2, 1}));
		map.put(ItemID.TARNISHED_LONGSWORD, polish("Tarnished_longsword", new int[]{ItemID.RUNE_LONGSWORD, ItemID.ADAMANT_LONGSWORD, ItemID.DRAGON_LONGSWORD}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_2H_SWORD, polish("Tarnished_2h_sword", new int[]{ItemID.RUNE_2H_SWORD, ItemID.ADAMANT_2H_SWORD, ItemID.MITHRIL_2H_SWORD}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_BATTLEAXE, polish("Tarnished_battleaxe", new int[]{ItemID.RUNE_BATTLEAXE, ItemID.ADAMANT_BATTLEAXE, ItemID.DRAGON_BATTLEAXE}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_HALBERD, polish("Tarnished_halberd", new int[]{ItemID.RUNE_HALBERD, ItemID.ADAMANT_HALBERD, ItemID.DRAGON_HALBERD}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_RING, polish("Tarnished_ring", new int[]{ItemID.RUBY_RING, ItemID.TOPAZ_RING, ItemID.SAPPHIRE_RING}, new int[]{7, 2, 1}));
		map.put(ItemID.TARNISHED_BRACELET, polish("Tarnished_bracelet", new int[]{ItemID.RUBY_BRACELET, ItemID.DIAMOND_BRACELET, ItemID.TOPAZ_BRACELET}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_NECKLACE, polish("Tarnished_necklace", new int[]{ItemID.RUBY_NECKLACE, ItemID.DIAMOND_NECKLACE, ItemID.JADE_NECKLACE}, new int[]{5, 4, 1}));
		map.put(ItemID.TARNISHED_AMULET, polish("Tarnished_amulet", new int[]{ItemID.RUBY_AMULET_U, ItemID.SAPPHIRE_AMULET_U, ItemID.DIAMOND_AMULET_U}, new int[]{6, 3, 1}));
		map.put(ItemID.ELDER_VENATOR_FANG, new Recipe("ETCH_TO_TRADEABLE",
			"https://oldschool.runescape.wiki/w/Elder_venator_fang", 1,
			new int[]{ItemID.ETCHED_ELDER_VENATOR_FANG}, new int[]{1}));
		map.put(ItemID.BLOOD_SAC, new Recipe("SQUEEZE_50_SACS_TO_VIAL",
			"https://oldschool.runescape.wiki/w/Blood_sac", 50,
			new int[]{ItemID.VIAL_OF_BLOOD_22446}, new int[]{1}));
		return Collections.unmodifiableMap(map);
	}

	private static Recipe polish(String page, int[] outputIds, int[] weights)
	{
		return new Recipe("POLISH_EXPECTED", "https://oldschool.runescape.wiki/w/" + page,
			10, outputIds, weights);
	}

	private static final class Recipe
	{
		private final String method;
		private final String sourceUrl;
		private final int denominator;
		private final int[] outputIds;
		private final int[] weights;

		private Recipe(String method, String sourceUrl, int denominator, int[] outputIds, int[] weights)
		{
			int totalWeight = Arrays.stream(weights).sum();
			if (outputIds.length == 0 || outputIds.length != weights.length || denominator <= 0
				|| totalWeight <= 0 || totalWeight > denominator)
			{
				throw new IllegalArgumentException("Invalid conversion recipe");
			}
			this.method = method;
			this.sourceUrl = sourceUrl;
			this.denominator = denominator;
			this.outputIds = outputIds.clone();
			this.weights = weights.clone();
		}
	}
}
