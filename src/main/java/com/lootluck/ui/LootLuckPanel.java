package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.BossDefinition;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ProfileDashboard;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.LootValuation;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.FixedExpectedBossRules;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.RewardHistoryEntry;
import com.lootluck.model.RewardScope;
import com.lootluck.model.RewardStatistics;
import com.lootluck.model.RewardSummary;
import com.lootluck.model.SavedLootEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import net.runelite.client.ui.PluginPanel;

public final class LootLuckPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
	private static final Color GP_HIGHLIGHT = new Color(82, 62, 18);
	private static final Color ABOVE_EXPECTED = new Color(178, 235, 128);
	private static final Color BELOW_EXPECTED = new Color(255, 154, 145);
	private final JTextPane overviewText = new JTextPane();
	private final JTextArea historyText = new JTextArea();
	private final JComboBox<BossDefinition> bossSelector = new JComboBox<>(
		BossRegistry.bosses().toArray(new BossDefinition[0]));
	private final JComboBox<RewardScope> statisticsScope = new JComboBox<>(RewardScope.values());
	private final Map<BossDefinition, String> latestEvents = new HashMap<>();
	private final Map<BossDefinition, String> latestLinks = new HashMap<>();
	private String eventSection = "Waiting for a Maggot King loot event.";
	private String rewardSection = "Reward option click: waiting";
	private String statisticsSection = "Loading reward summary...";
	private String storageSection = "Loading profile...";
	private ProfileDashboard dashboard;
	private String statisticsStatus = "Loading reward summary...";
	private Long totalDeltaGp;
	private Long latestDeltaGp;

	public LootLuckPanel()
	{
		setLayout(new BorderLayout(0, 8));
		JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
		header.add(new JLabel("Loot Luck Tracker"));
		bossSelector.addActionListener(event -> {
			showSelectedBossSections();
			renderStatistics();
			renderHistory();
		});
		header.add(bossSelector);
		statisticsScope.addActionListener(event -> {
			renderStatistics();
			renderHistory();
		});
		header.add(statisticsScope);
		add(header, BorderLayout.NORTH);
		overviewText.setEditable(false);
		overviewText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		overviewText.setBackground(historyText.getBackground());
		overviewText.setForeground(historyText.getForeground());
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
			latestEvents.clear();
			latestLinks.clear();
			showSelectedBossSections();
			dashboard = null;
			statisticsStatus = "Loading reward summary...";
			renderStatistics();
			renderHistory();
		});
	}

	public void showRewardOption(String option, String target, int targetId, int clientTick)
	{
		SwingUtilities.invokeLater(() -> {
			latestLinks.put(BossRegistry.MAGGOT_KING, "Reward option clicked: " + option +
				"\nTarget: " + target + " (#" + targetId + ")\nClient tick: " + clientTick +
				"\nClick observed only; reward not yet confirmed.");
			bossSelector.setSelectedItem(BossRegistry.MAGGOT_KING);
			showSelectedBossSections();
			renderOverview();
		});
	}

	public void showLootCorrelation(RewardClickCandidate click, int lootTick)
	{
		SwingUtilities.invokeLater(() -> {
			if (click == null)
			{
				latestLinks.put(BossRegistry.MAGGOT_KING,
					"No recent matching menu click. Loot still counts as an observed reward group.\n"
						+ "Reward method and expected GP unavailable; kill not confirmed.");
				showSelectedBossSections();
				renderOverview();
				return;
			}
			String method = "OPEN_STOMACH".equals(click.getMethod()) ? "Open-stomach" : "Take-eggs";
			latestLinks.put(BossRegistry.MAGGOT_KING, "Possible reward: " + method +
				"\nClick tick: " + click.getClientTick() + "; loot tick: " + lootTick +
				"\nTarget ID: #" + click.getTargetId() +
				"\nLoot callback counts as an observed reward group. Timing match supplies method only; kill not confirmed.");
			showSelectedBossSections();
			renderOverview();
		});
	}

	public void showLootGroupHint(BossDefinition boss)
	{
		SwingUtilities.invokeLater(() -> {
			latestLinks.put(boss, "Loot callback recorded. Nearby callbacks are grouped as one observed reward.\n"
				+ "Kill count is not confirmed.");
			showSelectedBossSections();
			renderOverview();
		});
	}

	public void showNoLootConfirmation(RewardClickCandidate click, int messageTick)
	{
		SwingUtilities.invokeLater(() -> {
			latestEvents.put(BossRegistry.MAGGOT_KING, "Latest reward\n\nMethod: Take-eggs\nOutcome: No loot\n"
				+ "Confirmed by the game's egg-pop message.\n\n"
				+ "No normal-loot expected GP is applied to this choice.");
			latestLinks.put(BossRegistry.MAGGOT_KING,
				"Take-eggs no-loot confirmed\nClick tick: " + click.getClientTick()
				+ "; message tick: " + messageTick + "\nTarget ID: #" + click.getTargetId());
			bossSelector.setSelectedItem(BossRegistry.MAGGOT_KING);
			showSelectedBossSections();
			renderOverview();
		});
	}

	public void showNoLootMessageUnmatched(String reason)
	{
		SwingUtilities.invokeLater(() -> {
			latestLinks.put(BossRegistry.MAGGOT_KING, "Take-eggs no-loot message observed.\n"
				+ reason + "\nNot counted or saved.");
			bossSelector.setSelectedItem(BossRegistry.MAGGOT_KING);
			showSelectedBossSections();
			renderOverview();
		});
	}

	public void showStorage(String profile, long count, String status)
	{
		SwingUtilities.invokeLater(() -> {
			storageSection = "Profile: " + profile +
				"\nSaved events (all bosses): " + (count < 0 ? "—" : count) + "\n" + status;
			renderOverview();
		});
	}

	public void showStatistics(ProfileDashboard summary)
	{
		SwingUtilities.invokeLater(() -> {
			dashboard = summary;
			renderStatistics();
			renderHistory();
		});
	}

	public void showStatisticsUnavailable()
	{
		SwingUtilities.invokeLater(() -> {
			dashboard = null;
			statisticsStatus = "Reward summary unavailable; check RuneLite log.";
			renderStatistics();
			renderHistory();
		});
	}

	private void renderStatistics()
	{
		if (dashboard == null)
		{
			totalDeltaGp = null;
			latestDeltaGp = null;
			statisticsSection = statisticsStatus;
			renderOverview();
			return;
		}
		BossDefinition boss = selectedBoss();
		RewardSummary rewardSummary = dashboard.get(boss);
		RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
		if (scope == null)
		{
			scope = RewardScope.ALL_TIME;
		}
		RewardStatistics stats = rewardSummary.get(scope);
		totalDeltaGp = stats.getNetLuckGp();
		latestDeltaGp = stats.getLastRewardDeltaGp();
		StringBuilder text = new StringBuilder();
		text.append("\nObserved loot reward groups: ").append(stats.getObservedLootRewards());
		if (stats.getPricedNormalRewards() > 0)
		{
			text.append("\nPriced non-egg rewards: ").append(stats.getPricedNormalRewards())
				.append(" / ").append(stats.getObservedNormalRewards());
			text.append("\nGP per priced reward: ")
				.append(stats.getAverageGpPerPricedReward() == null
					? "Unavailable" : gp(stats.getAverageGpPerPricedReward()));
		}
		if (boss == BossRegistry.MAGGOT_KING)
		{
			text.append("\nOpen-stomach or unlinked groups: ")
				.append(stats.getObservedNormalRewards());
			text.append("\nValued Open-stomach rewards: ").append(stats.getValuedNormalRewards());
			text.append("\nTake-eggs loot-linked: ").append(stats.getObservedEggRewards());
			text.append("\nConfirmed no-loot Take-eggs: ")
				.append(rewardSummary.getConfirmedNoLoot(scope));
		}
		else if (stats.getValuedNormalRewards() > 0)
		{
			text.append("\nValued reward groups: ").append(stats.getValuedNormalRewards());
		}
		if (stats.getNetLuckGp() != null)
		{
			text.append(boss == BossRegistry.MAGGOT_KING
				? "\nValued converted loot: " : "\nValued direct GE loot: ")
				.append(gp(stats.getConvertedGp()));
			text.append(boss == BossRegistry.MAGGOT_KING
				? "\nExpected for valued: " : "\nFixed expected for valued: ")
				.append(gp(stats.getExpectedGp()));
			text.append("\nProvisional GP vs expected: ").append(signedGp(stats.getNetLuckGp()));
			BigDecimal percent = stats.getPercentVsExpected();
			if (percent != null)
			{
				text.append("\nGP vs expected: ").append(signedPercent(percent));
			}
			text.append("\nLatest valued reward: ").append(stats.getLastRewardDeltaGp() == null
				? "Unavailable" : signedGp(stats.getLastRewardDeltaGp()));
		}
		else if (boss != BossRegistry.MAGGOT_KING)
		{
			text.append("\nGP comparison unavailable for an incomplete reward value.");
		}
		if (boss != BossRegistry.MAGGOT_KING)
		{
			text.append("\nFixed Wiki baseline; price changes and untradeable conversions are not updated.");
		}
		text.append("\nGrouped from loot callbacks within five seconds; not confirmed kill count.\n");
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
		String content = eventSection.trim() + "\n\n" + rewardSection.trim() + "\n\n"
			+ statisticsSection.trim() + "\n\n" + storageSection.trim();
		overviewText.setText(content);
		highlightGpDifference(content, "Provisional GP vs expected: ", totalDeltaGp);
		highlightGpDifference(content, "GP vs expected: ", totalDeltaGp);
		highlightGpDifference(content, "Latest valued reward: ", latestDeltaGp);
	}

	private void highlightGpDifference(String content, String label, Long deltaGp)
	{
		if (deltaGp == null || deltaGp == 0)
		{
			return;
		}
		int start = content.indexOf('\n' + label);
		if (start >= 0)
		{
			start++;
		}
		else if (content.startsWith(label))
		{
			start = 0;
		}
		else
		{
			return;
		}
		int end = content.indexOf('\n', start);
		if (end < 0)
		{
			end = content.length();
		}
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, deltaGp > 0 ? ABOVE_EXPECTED : BELOW_EXPECTED);
		StyleConstants.setBackground(style, GP_HIGHLIGHT);
		overviewText.getStyledDocument().setCharacterAttributes(start, end - start, style, false);
	}

	private void renderHistory()
	{
		if (dashboard == null)
		{
			historyText.setText(statisticsStatus);
			return;
		}
		RewardSummary rewardSummary = dashboard.get(selectedBoss());
		RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
		if (scope == null)
		{
			scope = RewardScope.ALL_TIME;
		}
		StringBuilder text = new StringBuilder(selectedBoss().getName())
			.append(" saved observations, newest first (up to 30)\n\n");
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
				text.append(click == null ? "Loot callback" :
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
					text.append("Expected for reward group: ")
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
			BossDefinition boss = BossRegistry.findBySource(observation.getSource());
			StringBuilder text = new StringBuilder();
			text.append("Latest loot event\n\n");
			text.append("Source: ").append(observation.getSource()).append('\n');
			text.append("Type: ").append(observation.getType()).append('\n');
			text.append("Observed: ").append(TIME_FORMAT.withZone(ZoneId.systemDefault()).format(observation.getObservedAtUtc())).append('\n');
			text.append("Item stacks: ").append(observation.getItems().size()).append('\n');
			Long actualGp = LootValuation.totalGpIfComplete(observation.getItems());
			Long convertedGp = LootValuation.totalConvertedGpIfComplete(observation.getItems());
			text.append("Direct GE sale estimate: ").append(actualGp == null ? "Unavailable (missing price)" : gp(actualGp)).append('\n');
			text.append(boss == BossRegistry.MAGGOT_KING
				? "After-conversion GE estimate: " : "Direct-only GE estimate: ")
				.append(convertedGp == null ? "Unavailable" : gp(convertedGp)).append("\n\n");
			if (expected != null)
			{
				text.append(boss == BossRegistry.MAGGOT_KING
					? "Expected Open-stomach GP: " : "Expected reward GP: ")
					.append(expected.getExpectedGp() == null
					? "Unavailable (missing table price)" : gp(expected.getExpectedGp())).append('\n');
				if (FixedExpectedBossRules.PRICE_SOURCE.equals(expected.getPriceSource()))
				{
					text.append("Expected source: fixed Wiki average; RuneLite prices are used for observed loot.\n\n");
				}
				else
				{
					text.append("Expected source: Wiki drop table + ")
						.append("RUNELITE_WIKI_ADJUSTED".equals(expected.getPriceSource())
							? "RuneLite Wiki prices" : "RuneLite Jagex prices")
						.append("\n\n");
				}
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
			if (boss == BossRegistry.MAGGOT_KING)
			{
				text.append("\nConversion estimate uses captured output prices and assumes required skills and inputs. Costs and GE fees excluded. GP differences count observed rewards; total kill count is not inferred.");
			}
			else
			{
				text.append("\nDirect-only estimate uses captured RuneLite prices. The fixed expected GP estimate may drift as prices change. Reward groups are provisional, not confirmed kill count.");
			}
			if (boss != null)
			{
				latestEvents.put(boss, text.toString());
				bossSelector.setSelectedItem(boss);
				showSelectedBossSections();
			}
			renderOverview();
			overviewText.setCaretPosition(0);
		});
	}

	private BossDefinition selectedBoss()
	{
		BossDefinition selected = (BossDefinition) bossSelector.getSelectedItem();
		return selected == null ? BossRegistry.MAGGOT_KING : selected;
	}

	private void showSelectedBossSections()
	{
		BossDefinition boss = selectedBoss();
		eventSection = latestEvents.getOrDefault(boss,
			"Waiting for a " + boss.getName() + " loot event on this profile.");
		rewardSection = latestLinks.getOrDefault(boss, boss == BossRegistry.MAGGOT_KING
			? "Reward option click: waiting"
			: "Loot callbacks count as observed reward groups.");
	}

	private static String gp(long amount)
	{
		return String.format(Locale.US, "%,d GP", amount);
	}

	private static String signedGp(long amount)
	{
		return (amount > 0 ? "+" : "") + gp(amount);
	}

	private static String signedPercent(BigDecimal amount)
	{
		return (amount.signum() > 0 ? "+" : "") + amount.toPlainString() + "%";
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
