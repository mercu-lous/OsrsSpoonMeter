package com.lootluck.model;

/** Supplies current RuneLite prices to a boss valuation rule. */
@FunctionalInterface
public interface ItemPriceLookup
{
	long price(int itemId);
}
