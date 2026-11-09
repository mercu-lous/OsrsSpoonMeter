package com.lootluck.model;

/** A priced output and its weight in a conversion recipe. */
public final class ConversionComponent
{
	private final int outputItemId;
	private final int weight;
	private final Long unitPriceGp;

	public ConversionComponent(int outputItemId, int weight, Long unitPriceGp)
	{
		if (outputItemId <= 0 || weight <= 0 || (unitPriceGp != null && unitPriceGp < 0))
		{
			throw new IllegalArgumentException("Invalid conversion component");
		}
		this.outputItemId = outputItemId;
		this.weight = weight;
		this.unitPriceGp = unitPriceGp;
	}

	public int getOutputItemId() { return outputItemId; }
	public int getWeight() { return weight; }
	public Long getUnitPriceGp() { return unitPriceGp; }
}
