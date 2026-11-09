package com.lootluck;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.SavedLootEvent;
import com.lootluck.storage.LootEventCodec;
import com.lootluck.storage.LootEventStore;
import com.lootluck.storage.ProfileIds;
import com.lootluck.ui.LootLuckPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Loot Luck Tracker",
	description = "Observe boss loot and compare it with expected GP as development progresses",
	tags = {"loot", "boss", "luck"},
	internalName = "loot_luck_tracker"
)
public class LootLuckPlugin extends Plugin
{
	private static final String SUPPORTED_SOURCE = "Maggot King";

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private LootLuckConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private RuneLiteConfig runeLiteConfig;

	private LootLuckPanel panel;
	private NavigationButton navigationButton;
	private ExecutorService storageExecutor;
	private LootEventStore storage; // Accessed only by storageExecutor.
	private String sessionId;
	private volatile String currentProfileId;

	@Override
	protected void startUp()
	{
		sessionId = UUID.randomUUID().toString();
		storageExecutor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "loot-luck-storage");
			thread.setDaemon(false);
			return thread;
		});
		panel = new LootLuckPanel();
		navigationButton = NavigationButton.builder()
			.tooltip("Loot Luck Tracker")
			.icon(createIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshProfileCount();
		log.debug("Loot Luck Tracker started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navigationButton);
		storageExecutor.shutdown(); // Allow queued event writes to finish without blocking shutdown.
		currentProfileId = null;
		panel = null;
		navigationButton = null;
		log.debug("Loot Luck Tracker stopped");
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (panel == null || !SUPPORTED_SOURCE.equals(event.getName()))
		{
			return;
		}

		List<LootItemPreview> items = new ArrayList<>();
		boolean wikiPriceSetting = runeLiteConfig.useWikiItemPrices();
		for (ItemStack item : event.getItems())
		{
			long price = itemManager.getItemPriceWithSource(item.getId(), wikiPriceSetting);
			String name = itemManager.getItemComposition(item.getId()).getName();
			items.add(new LootItemPreview(item.getId(), item.getQuantity(),
				name == null || name.isEmpty() ? "Item #" + item.getId() : name,
				price > 0 ? price : null,
				price > 0 ? (wikiPriceSetting ? "RUNELITE_WIKI_ADJUSTED" : "RUNELITE_JAGEX") : "UNAVAILABLE"));
		}
		LootEventPreview observation = new LootEventPreview(
			event.getName(),
			event.getType() == null ? "unknown" : event.getType().name(),
			Instant.now(),
			items
		);
		panel.showObservation(observation, config.showItemIds());
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null || profileKey.isEmpty())
		{
			currentProfileId = null;
			panel.showStorage("No active profile", -1, "Event not saved: profile unavailable");
			return;
		}

		String profileId = ProfileIds.fromRuneScapeProfileKey(profileKey);
		currentProfileId = profileId;
		SavedLootEvent savedEvent = new SavedLootEvent(
			UUID.randomUUID().toString(), profileId, sessionId, observation.getObservedAtUtc(),
			observation.getSource(), observation.getType(), event.getCombatLevel(), event.getAmount(),
			client.getTickCount(), items);
		LootLuckPanel targetPanel = panel;
		targetPanel.showStorage(shortProfileId(profileId), -1, "Saving event...");
		submitStorageTask(() -> {
			try
			{
				boolean inserted = getStorage().save(savedEvent);
				long count = getStorage().count(profileId);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), count,
						inserted ? "Event saved" : "Event already saved");
				}
			}
			catch (Exception exception)
			{
				log.warn("Could not save Maggot King loot event", exception);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), -1, "Save failed; check RuneLite log");
				}
			}
		});
		log.debug("Observed Maggot King loot event with {} item stacks", items.size());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (panel == null || event.getMenuOption() == null || event.getMenuTarget() == null)
		{
			return;
		}
		String option = Text.standardize(event.getMenuOption());
		String target = Text.standardize(event.getMenuTarget());
		if (!("open-stomach".equals(option) || "take-eggs".equals(option)) || !target.contains("maggot king"))
		{
			return;
		}
		panel.showRewardOption(Text.removeTags(event.getMenuOption()),
			Text.removeTags(event.getMenuTarget()), event.getId(), client.getTickCount());
		log.debug("Observed Maggot King reward option click: {} (target id {})", option, event.getId());
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		if (panel != null)
		{
			panel.clearObservation();
			refreshProfileCount();
		}
	}

	private void refreshProfileCount()
	{
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null || profileKey.isEmpty())
		{
			currentProfileId = null;
			panel.showStorage("No active profile", -1, "Log in to load saved events");
			return;
		}
		String profileId = ProfileIds.fromRuneScapeProfileKey(profileKey);
		currentProfileId = profileId;
		LootLuckPanel targetPanel = panel;
		targetPanel.showStorage(shortProfileId(profileId), -1, "Loading saved events...");
		submitStorageTask(() -> {
			try
			{
				long count = getStorage().count(profileId);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), count, "Ready");
				}
			}
			catch (Exception exception)
			{
				log.warn("Could not read saved loot event count", exception);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), -1, "Load failed; check RuneLite log");
				}
			}
		});
	}

	private void submitStorageTask(Runnable task)
	{
		try
		{
			storageExecutor.execute(task);
		}
		catch (RejectedExecutionException ignored)
		{
			log.debug("Storage executor closed before task could be queued");
		}
	}

	private LootEventStore getStorage() throws java.io.IOException
	{
		if (storage == null)
		{
			storage = new LootEventStore(getPluginDirectory(), new LootEventCodec(gson));
		}
		return storage;
	}

	private static String shortProfileId(String profileId)
	{
		return profileId.substring(0, 8);
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
