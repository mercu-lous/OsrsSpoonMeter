package com.lootluck.model;

import java.time.Instant;

/** Boss-specific valuation; sources and timing live in BossDefinition. */
public interface BossRules
{
	String conversionVersion();

	ConversionQuote quote(int itemId, int quantity, long directUnitPrice,
		boolean tradeable, ItemPriceLookup prices, String priceSource);

	ExpectedValueSnapshot expected(RewardClickCandidate click,
		ItemPriceLookup prices, String priceSource, Instant observedAtUtc);
}
