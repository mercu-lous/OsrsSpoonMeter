package com.lootluck;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.lootluck.model.LootEventPreview;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.BossDefinition;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.GauntletCompletionTracker;
import com.lootluck.model.GauntletOutcome;
import com.lootluck.model.NoLootConfirmation;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.ProfileDashboard;
import com.lootluck.model.SavedLootEvent;
import com.lootluck.storage.LootEventCodec;
import com.lootluck.storage.LootEventStore;
import com.lootluck.storage.NoLootConfirmationCodec;
import com.lootluck.storage.ProfileIds;
import com.lootluck.ui.LootLuckPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.events.ConfigChanged;
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
	private RewardClickCandidate recentRewardClick; // RuneLite client thread only.
	private int lastNoLootMessageTick = -1; // RuneLite client thread only.
	private final GauntletCompletionTracker gauntletCompletions = new GauntletCompletionTracker();
	private ZoneId calendarZone; // RuneLite client thread only.
	private LocalDate calendarDate; // RuneLite client thread only.

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
		recentRewardClick = null;
		lastNoLootMessageTick = -1;
		gauntletCompletions.reset();
		panel = null;
		navigationButton = null;
		log.debug("Loot Luck Tracker stopped");
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		BossDefinition boss = BossRegistry.findBySource(event.getName());
		if (panel == null || boss == null)
		{
			return;
		}

		List<LootItemPreview> items = new ArrayList<>();
		boolean wikiPriceSetting = runeLiteConfig.useWikiItemPrices();
		for (ItemStack item : event.getItems())
		{
			long price = itemManager.getItemPriceWithSource(item.getId(), wikiPriceSetting);
			ItemComposition composition = itemManager.getItemComposition(item.getId());
			ItemComposition canonicalComposition = itemManager.getItemComposition(itemManager.canonicalize(item.getId()));
			String name = composition == null ? null : composition.getName();
			boolean untradeable = canonicalComposition != null && !canonicalComposition.isTradeable();
			boolean directSale = price > 0 && (canonicalComposition == null || canonicalComposition.isGeTradeable());
			int highAlchGp = composition == null ? 0 : composition.getHaPrice();
			String priceSource = wikiPriceSetting ? "RUNELITE_WIKI_ADJUSTED" : "RUNELITE_JAGEX";
			items.add(new LootItemPreview(item.getId(), item.getQuantity(),
				name == null || name.isEmpty() ? "Item #" + item.getId() : name,
				directSale ? price : untradeable ? 0L : null,
				directSale ? priceSource : untradeable ? "UNTRADEABLE_NO_GE_SALE" : "UNAVAILABLE",
				highAlchGp > 0 ? (long) highAlchGp : null,
				boss.getRules().quote(item.getId(), item.getQuantity(), directSale ? price : 0,
					canonicalComposition != null && canonicalComposition.isTradeable(),
					outputId -> itemManager.getItemPriceWithSource(outputId, wikiPriceSetting), priceSource)));
		}
		LootEventPreview observation = new LootEventPreview(
			event.getName(),
			event.getType() == null ? "unknown" : event.getType().name(),
			Instant.now(),
			items
		);
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null || profileKey.isEmpty())
		{
			currentProfileId = null;
			panel.showObservation(observation, config.showItemIds(), null,
				GauntletOutcome.fromLegacy(observation.getSource(), items));
			panel.showStatistics(ProfileDashboard.empty(sessionId, Instant.now(), resolveCalendarZone()));
			panel.showStorage("No active profile", -1, "Event not saved: profile unavailable");
			return;
		}

		String profileId = ProfileIds.fromRuneScapeProfileKey(profileKey);
		currentProfileId = profileId;
		int lootTick = client.getTickCount();
		RewardClickCandidate click = boss == BossRegistry.MAGGOT_KING
			&& recentRewardClick != null && recentRewardClick.matches(profileId, lootTick)
			? recentRewardClick : null;
		GauntletOutcome gauntletOutcome = GauntletOutcome.NOT_APPLICABLE;
		if (GauntletOutcome.isGauntlet(boss))
		{
			observeGauntletCompletions(profileId);
			gauntletOutcome = gauntletCompletions.classify(profileId, boss, items,
				observation.getObservedAtUtc());
		}
		ExpectedValueSnapshot expected = !GauntletOutcome.isGauntlet(boss)
			|| gauntletOutcome == GauntletOutcome.COMPLETED
			? boss.getRules().expected(click,
				id -> itemManager.getItemPriceWithSource(id, wikiPriceSetting),
				wikiPriceSetting ? "RUNELITE_WIKI_ADJUSTED" : "RUNELITE_JAGEX",
				observation.getObservedAtUtc()) : null;
		panel.showObservation(observation, config.showItemIds(), expected, gauntletOutcome);
		if (boss == BossRegistry.MAGGOT_KING)
		{
			panel.showLootCorrelation(click, lootTick);
		}
		else
		{
			panel.showLootGroupHint(boss);
		}
		SavedLootEvent savedEvent = new SavedLootEvent(
			UUID.randomUUID().toString(), profileId, sessionId, observation.getObservedAtUtc(),
			observation.getSource(), observation.getType(), event.getCombatLevel(), event.getAmount(),
			lootTick, items, click, expected, gauntletOutcome);
		ZoneId summaryZone = resolveCalendarZone();
		LootLuckPanel targetPanel = panel;
		targetPanel.showStorage(shortProfileId(profileId), -1, "Saving event...");
		submitStorageTask(() -> {
			try
			{
				boolean inserted = getStorage().save(savedEvent);
				long count = getStorage().count(profileId);
				ProfileDashboard statistics = readStatistics(profileId, summaryZone);
				if (profileId.equals(currentProfileId))
				{
					if (statistics == null)
					{
						targetPanel.showStatisticsUnavailable();
					}
					else
					{
						targetPanel.showStatistics(statistics);
					}
					targetPanel.showStorage(shortProfileId(profileId), count,
						inserted ? "Event saved" : "Event already saved");
				}
			}
			catch (Exception exception)
			{
				log.warn("Could not save {} loot event", boss.getName(), exception);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), -1, "Save failed; check RuneLite log");
				}
			}
		});
		log.debug("Observed {} loot event with {} item stacks", boss.getName(), items.size());
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
		String profileKey = configManager.getRSProfileKey();
		recentRewardClick = profileKey == null || profileKey.isEmpty() ? null : new RewardClickCandidate(
			UUID.randomUUID().toString(), ProfileIds.fromRuneScapeProfileKey(profileKey),
			"open-stomach".equals(option) ? "OPEN_STOMACH" : "TAKE_EGGS",
			client.getTickCount(), event.getId(), Instant.now());
		panel.showRewardOption(Text.removeTags(event.getMenuOption()),
			Text.removeTags(event.getMenuTarget()), event.getId(), client.getTickCount());
		log.debug("Observed Maggot King reward option click: {} (target id {})", option, event.getId());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (panel == null || event.getMessage() == null
			|| !NoLootConfirmation.isNoLootText(Text.removeTags(event.getMessage())))
		{
			return;
		}
		int messageTick = client.getTickCount();
		if (messageTick == lastNoLootMessageTick)
		{
			return;
		}
		lastNoLootMessageTick = messageTick;
		String chatType = event.getType() == null ? "UNKNOWN" : event.getType().name();
		if (!NoLootConfirmation.isSupportedChatType(chatType))
		{
			panel.showNoLootMessageUnmatched("Unexpected chat type: " + chatType);
			return;
		}
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null || profileKey.isEmpty())
		{
			panel.showNoLootMessageUnmatched("No active profile.");
			return;
		}
		String profileId = ProfileIds.fromRuneScapeProfileKey(profileKey);
		RewardClickCandidate click = recentRewardClick;
		if (click == null || !"TAKE_EGGS".equals(click.getMethod())
			|| !click.matches(profileId, messageTick))
		{
			panel.showNoLootMessageUnmatched("No matching Take-eggs click within five ticks.");
			return;
		}
		NoLootConfirmation confirmation = new NoLootConfirmation(click, sessionId,
			Instant.now(), messageTick, chatType);
		panel.showNoLootConfirmation(click, messageTick);
		ZoneId summaryZone = resolveCalendarZone();
		LootLuckPanel targetPanel = panel;
		targetPanel.showStorage(shortProfileId(profileId), -1, "Saving no-loot confirmation...");
		submitStorageTask(() -> {
			try
			{
				boolean inserted = getStorage().saveConfirmation(confirmation);
				long count = getStorage().count(profileId);
				ProfileDashboard statistics = readStatistics(profileId, summaryZone);
				if (profileId.equals(currentProfileId))
				{
					if (statistics == null)
					{
						targetPanel.showStatisticsUnavailable();
					}
					else
					{
						targetPanel.showStatistics(statistics);
					}
					targetPanel.showStorage(shortProfileId(profileId), count,
						inserted ? "No-loot confirmation saved" : "No-loot confirmation already saved");
				}
			}
			catch (Exception exception)
			{
				log.warn("Could not save Maggot King no-loot confirmation", exception);
				if (profileId.equals(currentProfileId))
				{
					targetPanel.showStorage(shortProfileId(profileId), -1,
						"No-loot save failed; check RuneLite log");
				}
			}
		});
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		recentRewardClick = null;
		lastNoLootMessageTick = -1;
		gauntletCompletions.reset();
		if (panel != null)
		{
			panel.clearObservation();
			refreshProfileCount();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (panel != null && "loot-luck-tracker".equals(event.getGroup())
			&& "calendarTimeZone".equals(event.getKey()))
		{
			refreshProfileCount();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (panel != null && currentProfileId != null && client.getGameState() == GameState.LOGGED_IN)
		{
			observeGauntletCompletions(currentProfileId);
		}
		if (panel != null && currentProfileId != null && calendarZone != null
			&& !LocalDate.now(calendarZone).equals(calendarDate))
		{
			refreshProfileCount();
		}
	}

	private void observeGauntletCompletions(String profileId)
	{
		gauntletCompletions.observe(profileId,
			client.getVarpValue(VarPlayerID.TOTAL_COMPLETED_GAUNTLET),
			client.getVarpValue(VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM));
	}

	private void refreshProfileCount()
	{
		ZoneId summaryZone = resolveCalendarZone();
		calendarZone = summaryZone;
		calendarDate = LocalDate.now(summaryZone);
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null || profileKey.isEmpty())
		{
			currentProfileId = null;
			panel.showStatistics(ProfileDashboard.empty(sessionId, Instant.now(), summaryZone));
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
				ProfileDashboard statistics = readStatistics(profileId, summaryZone);
				if (profileId.equals(currentProfileId))
				{
					if (statistics == null)
					{
						targetPanel.showStatisticsUnavailable();
					}
					else
					{
						targetPanel.showStatistics(statistics);
					}
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
			storage = new LootEventStore(getPluginDirectory(), new LootEventCodec(gson),
				new NoLootConfirmationCodec(gson));
		}
		return storage;
	}

	private ProfileDashboard readStatistics(String profileId, ZoneId summaryZone)
	{
		try
		{
			return getStorage().statistics(profileId, sessionId, summaryZone);
		}
		catch (Exception exception)
		{
			log.warn("Could not read boss reward summary", exception);
			return null;
		}
	}

	private ZoneId resolveCalendarZone()
	{
		String configured = config.calendarTimeZone();
		if (configured == null || configured.trim().isEmpty())
		{
			return ZoneId.systemDefault();
		}
		try
		{
			return ZoneId.of(configured.trim());
		}
		catch (DateTimeException invalid)
		{
			return ZoneId.systemDefault();
		}
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
