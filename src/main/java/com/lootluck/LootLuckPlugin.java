package com.lootluck;

import com.google.inject.Provides;
import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import com.lootluck.ui.LootLuckPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Loot Luck Tracker",
	description = "Observe boss loot and compare it with expected GP as development progresses",
	tags = {"loot", "boss", "luck"},
	internalName = "loot_luck_tracker"
)
public class LootLuckPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private LootLuckConfig config;

	private LootLuckPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		panel = new LootLuckPanel();
		navigationButton = NavigationButton.builder()
			.tooltip("Loot Luck Tracker")
			.icon(createIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		log.debug("Loot Luck Tracker started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		panel = null;
		navigationButton = null;
		log.debug("Loot Luck Tracker stopped");
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (panel == null)
		{
			return;
		}

		List<LootItemPreview> items = new ArrayList<>();
		for (ItemStack item : event.getItems())
		{
			items.add(new LootItemPreview(item.getId(), item.getQuantity()));
		}
		LootEventPreview observation = new LootEventPreview(
			event.getName(),
			event.getType() == null ? "unknown" : event.getType().name(),
			Instant.now(),
			items
		);
		panel.showObservation(observation, config.showItemIds());
		log.debug("Observed loot event from {} with {} item stacks", event.getName(), items.size());
	}

	@Provides
	LootLuckConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LootLuckConfig.class);
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(new Color(55, 182, 112));
			graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
			graphics.setColor(Color.BLACK);
			graphics.drawString("L", 5, 12);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
