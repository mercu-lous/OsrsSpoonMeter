package com.lootluck.model;

import java.time.Instant;

/** Capture-only default until a boss has a verified expected-value table. */
public final class DirectGeBossRules implements BossRules
{
	@Override
	public String conversionVersion()
	{
		return DirectGeConversion.VERSION;
	}

	@Override
	public ConversionQuote quote(int itemId, int quantity, long directUnitPrice,
		boolean tradeable, ItemPriceLookup prices, String priceSource)
	{
		return DirectGeConversion.quote(quantity, directUnitPrice, tradeable, priceSource);
	}

	@Override
	public ExpectedValueSnapshot expected(RewardClickCandidate click,
		ItemPriceLookup prices, String priceSource, Instant observedAtUtc)
	{
		return null;
	}
}
