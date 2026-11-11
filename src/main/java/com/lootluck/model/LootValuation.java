package com.lootluck.model;

import java.util.List;
import net.runelite.api.gameval.ItemID;

/** Valuation helpers; older saved null prices remain unavailable. */
public final class LootValuation
{
	public static final String MISSING_PRICE_ASSUMED_ZERO = "MISSING_PRICE_ASSUMED_ZERO";

	private LootValuation()
	{
	}

	/** New captured items without a direct price contribute zero; coins keep face value. */
	public static Long directUnitGp(int itemId, long reportedPrice,
		boolean untradeable, boolean geTradeable)
	{
		if (itemId == ItemID.COINS)
		{
			return 1L;
		}
		if (untradeable)
		{
			return 0L;
		}
		if (reportedPrice > 0 && geTradeable)
		{
			return reportedPrice;
		}
		return 0L;
	}

	public static boolean isMissingDirectPrice(int itemId, long reportedPrice,
		boolean untradeable, boolean geTradeable)
	{
		return itemId != ItemID.COINS && !untradeable
			&& !(reportedPrice > 0 && geTradeable);
	}

	public static Long totalGpIfComplete(List<LootItemPreview> items)
	{
		long total = 0;
		try
		{
			for (LootItemPreview item : items)
			{
				Long price = item.getUnitPriceGp();
				if (price == null || price < 0 || item.getQuantity() < 0)
				{
					return null;
				}
				total = Math.addExact(total, Math.multiplyExact(price, item.getQuantity()));
			}
			return total;
		}
		catch (ArithmeticException overflow)
		{
			return null;
		}
	}

	public static Long totalConvertedGpIfComplete(List<LootItemPreview> items)
	{
		long total = 0;
		try
		{
			for (LootItemPreview item : items)
			{
				ConversionQuote quote = item.getConversionQuote();
				if (quote == null || quote.getStackGp() == null)
				{
					return null;
				}
				total = Math.addExact(total, quote.getStackGp());
			}
			return total;
		}
		catch (ArithmeticException overflow)
		{
			return null;
		}
	}
}
