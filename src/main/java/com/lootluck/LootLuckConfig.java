package com.lootluck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("loot-luck-tracker")
public interface LootLuckConfig extends Config
{
	@ConfigItem(
		keyName = "showItemIds",
		name = "Show item IDs in preview",
		description = "Include item IDs in the temporary loot event preview"
	)
	default boolean showItemIds()
	{
		return true;
	}
}
