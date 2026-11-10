package com.lootluck.model;

import java.time.Instant;
import java.util.Collections;

/** A sourced, fixed direct-GE estimate per observed reward for early boss testing. */
public final class FixedExpectedBossRules implements BossRules
{
	public static final String SOURCE_VERSION = "WIKI_FIXED_DIRECT_GE_2026_11_10_V1";
	public static final String PRICE_SOURCE = "WIKI_FIXED_AVERAGE";
	private final long expectedGp;
	private final String sourceUrl;

	public FixedExpectedBossRules(long expectedGp, String sourceUrl)
	{
		if (expectedGp < 0)
		{
			throw new IllegalArgumentException("Expected GP cannot be negative");
		}
		this.expectedGp = expectedGp;
		this.sourceUrl = java.util.Objects.requireNonNull(sourceUrl);
	}

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
		return new ExpectedValueSnapshot(expectedGp, SOURCE_VERSION, sourceUrl,
			conversionVersion(), PRICE_SOURCE, observedAtUtc, Collections.emptyMap());
	}
}
