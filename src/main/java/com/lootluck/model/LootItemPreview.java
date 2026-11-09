package com.lootluck.model;

public final class LootItemPreview
{
	private final int itemId;
	private final int quantity;
	private final String name;
	private final Long unitPriceGp;
	private final String priceSource;
	private final Long highAlchGp;

	public LootItemPreview(int itemId, int quantity)
	{
		this(itemId, quantity, "Item #" + itemId, null, "UNAVAILABLE");
	}

	public LootItemPreview(int itemId, int quantity, String name, Long unitPriceGp, String priceSource)
	{
		this(itemId, quantity, name, unitPriceGp, priceSource, null);
	}

	public LootItemPreview(int itemId, int quantity, String name, Long unitPriceGp,
		String priceSource, Long highAlchGp)
	{
		this.itemId = itemId;
		this.quantity = quantity;
		this.name = name;
		this.unitPriceGp = unitPriceGp;
		this.priceSource = priceSource;
		this.highAlchGp = highAlchGp;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public String getName()
	{
		return name;
	}

	public Long getUnitPriceGp()
	{
		return unitPriceGp;
	}

	public String getPriceSource()
	{
		return priceSource;
	}

	public Long getHighAlchGp()
	{
		return highAlchGp;
	}
}
