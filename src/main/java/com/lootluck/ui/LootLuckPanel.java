package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.LootValuation;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.RewardHistoryEntry;
import com.lootluck.model.RewardScope;
import com.lootluck.model.RewardStatistics;
import com.lootluck.model.RewardSummary;
import com.lootluck.model.SavedLootEvent;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

public final class LootLuckPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
	private final JTextArea overviewText = new JTextArea();
	private final JTextArea historyText = new JTextArea();
	private final JComboBox<RewardScope> statisticsScope = new JComboBox<>(RewardScope.values());
	private String eventSection = "Waiting for a Maggot King loot event.";
	private String rewardSection = "Reward option click: waiting";
	private String statisticsSection = "Loading reward summary...";
	private String storageSection = "Loading profile...";
	private RewardSummary rewardSummary;
	private String statisticsStatus = "Loading reward summary...";

	public LootLuckPanel()
	{
		setLayout(new BorderLayout(0, 8));
		JPanel header = new JPanel(new BorderLayout(0, 4));
		header.add(new JLabel("Loot Luck Tracker"), BorderLayout.NORTH);
		statisticsScope.addActionListener(event -> {
			renderStatistics();
			renderHistory();
		});
		header.add(statisticsScope, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		overviewText.setEditable(false);
		overviewText.setLineWrap(true);
		overviewText.setWrapStyleWord(true);
		overviewText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		renderOverview();
		historyText.setEditable(false);
		historyText.setLineWrap(true);
		historyText.setWrapStyleWord(true);
		historyText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		historyText.setText("Loading saved observations...");
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Overview", new JScrollPane(overviewText));
		tabs.addTab("History", new JScrollPane(historyText));
		add(tabs, BorderLayout.CENTER);
	}

	public void clearObservation()
	{
		SwingUtilities.invokeLater(() -> {
			eventSection = "Waiting for a Maggot King loot event on this profile.";
			rewardSection = "Reward option click: waiting";
			rewardSummary = null;
			statisticsStatus = "Loading reward summary...";
			renderStatistics();
			renderHistory();
		});
	}

	public void showRewardOption(String option, String target, int targetId, int clientTick)
	{
		SwingUtilities.invokeLater(() -> {
			rewardSection = "Reward option clicked: " + option +
				"\nTarget: " + target + " (#" + targetId + ")\nClient tick: " + clientTick +
				"\nClick observed only; reward not yet confirmed.";
			renderOverview();
		});
	}

	public void showLootCorrelation(RewardClickCandidate click, int lootTick)
	{
		SwingUtilities.invokeLater(() -> {
			if (click == null)
			{
				rewardSection = "Reward link: no recent matching click.\nKill not confirmed.";
				renderOverview();
				return;
			}
			String method = "OPEN_STOMACH".equals(click.getMethod()) ? "Open-stomach" : "Take-eggs";
			rewardSection = "Possible reward: " + method +
				"\nClick tick: " + click.getClientTick() + "; loot tick: " + lootTick +
				"\nTarget ID: #" + click.getTargetId() +
				"\nTiming match only; kill not confirmed.";
			renderOverview();
		});
	}

	public void showNoLootConfirmation(RewardClickCandidate click, int messageTick)
	{
		SwingUtilities.invokeLater(() -> {
			eventSection = "Latest reward\n\nMethod: Take-eggs\nOutcome: No loot\n"
				+ "Confirmed by the game's egg-pop message.\n\n"
				+ "No normal-loot expected GP is applied to this choice.";
			rewardSection = "Take-eggs no-loot confirmed\nClick tick: " + click.getClientTick()
				+ "; message tick: " + messageTick + "\nTarget ID: #" + click.getTargetId();
			renderOverview();
		});
	}

	public void showNoLootMessageUnmatched(String reason)
	{
		SwingUtilities.invokeLater(() -> {
			rewardSection = "Take-eggs no-loot message observed.\n"
				+ reason + "\nNot counted or saved.";
			renderOverview();
		});
	}

	public void showStorage(String profile, long count, String status)
	{
		SwingUtilities.invokeLater(() -> {
			storageSection = "Profile: " + profile +
				"\nSaved events: " + (count < 0 ? "—" : count) + "\n" + status;
			renderOverview();
		});
	}

	public void showStatistics(RewardSummary summary)
	{
		SwingUtilities.invokeLater(() -> {
			rewardSummary = summary;
			renderStatistics();
			renderHistory();
		});
	}

	public void showStatisticsUnavailable()
	{
		SwingUtilities.invokeLater(() -> {
			rewardSummary = null;
			statisticsStatus = "Reward summary unavailable; check RuneLite log.";
			renderStatistics();
			renderHistory();
		});
	}

	private void renderStatistics()
	{
		if (rewardSummary == null)
		{
			statisticsSection = statisticsStatus;
			renderOverview();
			return;
		}
		RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
		if (scope == null)
		{
			scope = RewardScope.ALL_TIME;
		}
		RewardStatistics stats = rewardSummary.get(scope);
		StringBuilder text = new StringBuilder();
		text.append("\nTiming-linked Open-stomach rewards: ").append(stats.getObservedNormalRewards());
		text.append("\nValued normal rewards: ").append(stats.getValuedNormalRewards());
		text.append("\nTake-eggs loot-linked: ").append(stats.getObservedEggRewards());
		text.append("\nConfirmed no-loot Take-eggs: ")
			.append(rewardSummary.getConfirmedNoLoot(scope));
		if (stats.getNetLuckGp() != null)
		{
			text.append("\nConverted loot: ").append(gp(stats.getConvertedGp()));
			text.append("\nExpected: ").append(gp(stats.getExpectedGp()));
			text.append("\nProvisional GP vs expected: ").append(signedGp(stats.getNetLuckGp()));
			text.append("\nLatest normal reward: ").append(stats.getLastRewardDeltaGp() == null
				? "Unavailable" : signedGp(stats.getLastRewardDeltaGp()));
		}
		text.append("\nLoot linked by click timing; not total kill count.\n");
		if (scope.isCalendar())
		{
			text.append("Time zone: ").append(rewardSummary.getCalendarZone().getId())
				.append("; weeks start Monday.\n");
		}
		statisticsSection = text.toString();
		renderOverview();
	}

	private void renderOverview()
	{
		overviewText.setText(eventSection.trim() + "\n\n" + rewardSection.trim() + "\n\n"
			+ statisticsSection.trim() + "\n\n" + storageSection.trim());
	}

	private void renderHistory()
	{
		if (rewardSummary == null)
		{
			historyText.setText(statisticsStatus);
			return;
		}
		RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
		if (scope == null)
		{
			scope = RewardScope.ALL_TIME;
		}
		StringBuilder text = new StringBuilder("Saved observations, newest first (up to 30)\n\n");
		java.util.List<RewardHistoryEntry> entries = rewardSummary.getHistory(scope);
		if (entries.isEmpty())
		{
			text.append("No saved observations in this view.");
		}
		for (RewardHistoryEntry entry : entries)
		{
			text.append(TIME_FORMAT.withZone(rewardSummary.getCalendarZone())
				.format(entry.getObservedAtUtc())).append('\n');
			if (entry.isNoLootConfirmation())
			{
				text.append("Take-eggs: no loot confirmed by game message\n");
				text.append("Click tick: ").append(entry.getNoLootConfirmation().getClick().getClientTick())
					.append("; message tick: ").append(entry.getNoLootConfirmation().getClientTick()).append('\n');
			}
			else
			{
				SavedLootEvent event = entry.getLootEvent();
				RewardClickCandidate click = event.getRewardClick();
				text.append(click == null ? "Unlinked loot event" :
					"OPEN_STOMACH".equals(click.getMethod()) ? "Open-stomach timing link" : "Take-eggs timing link")
					.append(" | event ").append(event.getEventId(), 0, 8).append('\n');
				for (LootItemPreview item : event.getItems())
				{
					text.append(item.getQuantity()).append(" × ").append(item.getName())
						.append(" (#").append(item.getItemId()).append(')');
					text.append(item.getUnitPriceGp() == null ? " @ direct price unavailable"
						: " @ " + gp(item.getUnitPriceGp()) + " direct each").append('\n');
					ConversionQuote quote = item.getConversionQuote();
					if (quote != null && quote.getStackGp() != null
						&& !"DIRECT_GE".equals(quote.getMethod())
						&& !"NO_GE_CONVERSION".equals(quote.getMethod()))
					{
						text.append("  After conversion: ").append(gp(quote.getStackGp())).append('\n');
					}
				}
				if (event.getItems().isEmpty())
				{
					text.append("No item stacks in this callback\n");
				}
				text.append("Direct event value: ").append(event.getActualGp() == null
					? "Unavailable" : gp(event.getActualGp())).append('\n');
				text.append("Converted event value: ").append(event.getConvertedGp() == null
					? "Unavailable" : gp(event.getConvertedGp())).append('\n');
				if (event.getExpectedSnapshot() != null)
				{
					Long expectedGp = event.getExpectedSnapshot().getExpectedGp();
					text.append("Expected for linked reward: ")
						.append(expectedGp == null ? "Unavailable" : gp(expectedGp)).append('\n');
				}
			}
			text.append('\n');
		}
		historyText.setText(text.toString());
		historyText.setCaretPosition(0);
	}

	public void showObservation(LootEventPreview observation, boolean showItemIds,
		ExpectedValueSnapshot expected)
	{
		SwingUtilities.invokeLater(() -> {
			StringBuilder text = new StringBuilder();
			text.append("Latest loot event\n\n");
			text.append("Source: ").append(observation.getSource()).append('\n');
			text.append("Type: ").append(observation.getType()).append('\n');
			text.append("Observed: ").append(TIME_FORMAT.withZone(ZoneId.systemDefault()).format(observation.getObservedAtUtc())).append('\n');
			text.append("Item stacks: ").append(observation.getItems().size()).append('\n');
			Long actualGp = LootValuation.totalGpIfComplete(observation.getItems());
			Long convertedGp = LootValuation.totalConvertedGpIfComplete(observation.getItems());
			text.append("Direct GE sale estimate: ").append(actualGp == null ? "Unavailable (missing price)" : gp(actualGp)).append('\n');
			text.append("After-conversion GE estimate: ").append(convertedGp == null ? "Unavailable" : gp(convertedGp)).append("\n\n");
			if (expected != null)
			{
				text.append("Expected Open-stomach GP: ").append(expected.getExpectedGp() == null
					? "Unavailable (missing table price)" : gp(expected.getExpectedGp())).append('\n');
				text.append("Expected source: Wiki drop table + ")
					.append("RUNELITE_WIKI_ADJUSTED".equals(expected.getPriceSource())
						? "RuneLite Wiki prices" : "RuneLite Jagex prices")
					.append("\n\n");
			}
			boolean hasUntradeable = false;
			for (LootItemPreview item : observation.getItems())
			{
				text.append(item.getQuantity()).append(" × ").append(item.getName());
				if (showItemIds)
				{
					text.append(" (#").append(item.getItemId()).append(')');
				}
				if ("UNTRADEABLE_NO_GE_SALE".equals(item.getPriceSource()))
				{
					hasUntradeable = true;
					text.append(" @ 0 GP direct sale (untradeable)");
					if (item.getHighAlchGp() != null)
					{
						text.append("; HA ").append(gp(item.getHighAlchGp()));
					}
				}
				else
				{
					text.append(" @ ").append(item.getUnitPriceGp() == null ? "price unavailable" : gp(item.getUnitPriceGp()) + " each");
				}
				text.append('\n');
				ConversionQuote quote = item.getConversionQuote();
				if (quote != null && !"DIRECT_GE".equals(quote.getMethod())
					&& !"NO_GE_CONVERSION".equals(quote.getMethod())
					&& !"MISSING_DIRECT_PRICE".equals(quote.getMethod()))
				{
					text.append("  ").append(conversionLabel(quote)).append('\n');
				}
			}
			if (hasUntradeable)
			{
				text.append("\nUntradeable items count as 0 direct GE GP. HA and conversion estimates are shown separately.\n");
			}
			text.append("\nConversion estimate uses captured output prices and assumes required skills and inputs. Costs and GE fees excluded. GP differences count observed rewards; total kill count is not inferred.");
			eventSection = text.toString();
			renderOverview();
			overviewText.setCaretPosition(0);
		});
	}

	private static String gp(long amount)
	{
		return String.format(Locale.US, "%,d GP", amount);
	}

	private static String signedGp(long amount)
	{
		return (amount > 0 ? "+" : "") + gp(amount);
	}

	private static String conversionLabel(ConversionQuote quote)
	{
		Long value = quote.getStackGp();
		if (value == null)
		{
			switch (quote.getMethod())
			{
				case "POLISH_EXPECTED":
					return "Polish value unavailable: missing output price";
				case "ETCH_TO_TRADEABLE":
					return "Etched fang price unavailable";
				case "SQUEEZE_50_SACS_TO_VIAL":
					return "Vial of blood price unavailable";
				case "UNSUPPORTED_UNTRADEABLE":
					return "No GP conversion estimate for this item yet";
				default:
					return "Conversion value unavailable";
			}
		}
		switch (quote.getMethod())
		{
			case "POLISH_EXPECTED":
				return "Expected after polish (64 Crafting): " + gp(value);
			case "ETCH_TO_TRADEABLE":
				return "After etching (84 Crafting): " + gp(value);
			case "SQUEEZE_50_SACS_TO_VIAL":
				return "After squeezing (50 sacs/vial): " + gp(value);
			default:
				return "After conversion: " + gp(value);
		}
	}
}
