package com.lootluck.model;

import java.util.Collections;

/** Default valuation for bosses without verified item conversion recipes. */
public final class DirectGeConversion
{
	public static final String VERSION = "DIRECT_GE_ONLY_V1";

	private DirectGeConversion()
	{
	}

	public static ConversionQuote quote(int quantity, long directUnitPrice,
		boolean tradeable, String priceSource)
	{
		if (quantity < 0)
		{
			return new ConversionQuote("INVALID_QUANTITY", null, null, priceSource, 1,
				Collections.emptyList());
		}
		if (directUnitPrice > 0)
		{
			try
			{
				return new ConversionQuote("DIRECT_GE", Math.multiplyExact(directUnitPrice, quantity),
					null, priceSource, 1, Collections.emptyList());
			}
			catch (ArithmeticException overflow)
			{
				return new ConversionQuote("OVERFLOW", null, null, priceSource, 1,
						Collections.emptyList());
			}
		}
		return new ConversionQuote(tradeable ? "MISSING_DIRECT_PRICE" : "NO_GE_CONVERSION",
			tradeable ? null : 0L, null, priceSource, 1, Collections.emptyList());
	}
}
