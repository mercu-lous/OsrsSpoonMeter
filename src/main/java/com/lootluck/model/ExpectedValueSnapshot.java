package com.lootluck.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Frozen expected GP and the item prices used for the boss drop-table calculation. */
public final class ExpectedValueSnapshot
{
	private final Long expectedGp;
	private final String sourceVersion;
	private final String sourceUrl;
	private final String conversionCatalogVersion;
	private final String priceSource;
	private final Instant capturedAtUtc;
	private final Map<Integer, Long> inputPrices;

	public ExpectedValueSnapshot(Long expectedGp, String sourceVersion, String sourceUrl,
		String conversionCatalogVersion, String priceSource, Instant capturedAtUtc,
		Map<Integer, Long> inputPrices)
	{
		if (expectedGp != null && expectedGp < 0)
		{
			throw new IllegalArgumentException("Expected GP cannot be negative");
		}
		this.expectedGp = expectedGp;
		this.sourceVersion = Objects.requireNonNull(sourceVersion);
		this.sourceUrl = Objects.requireNonNull(sourceUrl);
		this.conversionCatalogVersion = Objects.requireNonNull(conversionCatalogVersion);
		this.priceSource = Objects.requireNonNull(priceSource);
		this.capturedAtUtc = Objects.requireNonNull(capturedAtUtc);
		this.inputPrices = Collections.unmodifiableMap(new LinkedHashMap<>(inputPrices));
	}

	public Long getExpectedGp() { return expectedGp; }
	public String getSourceVersion() { return sourceVersion; }
	public String getSourceUrl() { return sourceUrl; }
	public String getConversionCatalogVersion() { return conversionCatalogVersion; }
	public String getPriceSource() { return priceSource; }
	public Instant getCapturedAtUtc() { return capturedAtUtc; }
	public Map<Integer, Long> getInputPrices() { return inputPrices; }
}
