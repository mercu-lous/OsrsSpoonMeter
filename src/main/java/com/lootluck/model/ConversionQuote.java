package com.lootluck.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Frozen, item-stack-level conversion estimate and the prices used to derive it. */
public final class ConversionQuote
{
	private final String method;
	private final Long stackGp;
	private final String recipeSourceUrl;
	private final String priceSource;
	private final int denominator;
	private final List<ConversionComponent> components;

	public ConversionQuote(String method, Long stackGp, String recipeSourceUrl,
		String priceSource, int denominator, List<ConversionComponent> components)
	{
		if (denominator <= 0 || (stackGp != null && stackGp < 0))
		{
			throw new IllegalArgumentException("Invalid conversion quote");
		}
		this.method = Objects.requireNonNull(method);
		this.stackGp = stackGp;
		this.recipeSourceUrl = recipeSourceUrl;
		this.priceSource = priceSource;
		this.denominator = denominator;
		this.components = Collections.unmodifiableList(new ArrayList<>(components));
	}

	public String getMethod() { return method; }
	public Long getStackGp() { return stackGp; }
	public String getRecipeSourceUrl() { return recipeSourceUrl; }
	public String getPriceSource() { return priceSource; }
	public int getDenominator() { return denominator; }
	public List<ConversionComponent> getComponents() { return components; }
}
