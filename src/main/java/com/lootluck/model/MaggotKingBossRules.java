package com.lootluck.model;

import java.time.Instant;

/** Existing Maggot King conversion and Open-stomach expected-value rules. */
public final class MaggotKingBossRules implements BossRules
{
	@Override
	public String conversionVersion()
	{
		return MaggotKingConversionCatalog.VERSION;
	}

	@Override
	public ConversionQuote quote(int itemId, int quantity, long directUnitPrice,
		boolean tradeable, ItemPriceLookup prices, String priceSource)
	{
		return MaggotKingConversionCatalog.quote(itemId, quantity, directUnitPrice,
			tradeable, prices, priceSource);
	}

	@Override
	public ExpectedValueSnapshot expected(RewardClickCandidate click,
		ItemPriceLookup prices, String priceSource, Instant observedAtUtc)
	{
		return click != null && "OPEN_STOMACH".equals(click.getMethod())
			? MaggotKingExpectedValueService.capture(prices, priceSource, observedAtUtc) : null;
	}
}
