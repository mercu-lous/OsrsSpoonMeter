package com.lootluck.model;

public final class LootItemPreview
{
	private final int itemId;
	private final int quantity;

	public LootItemPreview(int itemId, int quantity)
	{
		this.itemId = itemId;
		this.quantity = quantity;
	}

	public int getItemId()
	{
		return itemId;
	}

	public int getQuantity()
	{
		return quantity;
	}
}
