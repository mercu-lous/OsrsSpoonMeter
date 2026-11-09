package com.lootluck;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Local development entry point, following RuneLite's example plugin template. */
public class LootLuckPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LootLuckPlugin.class);
		RuneLite.main(args);
	}
}
