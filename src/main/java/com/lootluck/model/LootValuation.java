package com.lootluck.model;

import java.util.List;

/** Returns no total when any price is unavailable or arithmetic overflows. */
public final class LootValuation
{
	private LootValuation()
	{
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
}
