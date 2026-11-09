package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.LootValuation;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.RewardScope;
import com.lootluck.model.RewardStatistics;
import com.lootluck.model.RewardSummary;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

public final class LootLuckPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
	private final JTextArea eventText = new JTextArea();
	private final JTextArea rewardText = new JTextArea();
	private final JTextArea statisticsText = new JTextArea();
	private final JTextArea storageText = new JTextArea();
	private final JComboBox<RewardScope> statisticsScope = new JComboBox<>(RewardScope.values());
	private RewardSummary rewardSummary;
	private String statisticsStatus = "Loading reward summary...";

	public LootLuckPanel()
	{
		setLayout(new BorderLayout(0, 8));
		JPanel header = new JPanel(new BorderLayout(0, 4));
		header.add(new JLabel("Loot Luck Tracker"), BorderLayout.NORTH);
		statisticsScope.addActionListener(event -> renderStatistics());
		header.add(statisticsScope, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);
		eventText.setEditable(false);
		eventText.setLineWrap(true);
		eventText.setWrapStyleWord(true);
		eventText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		eventText.setText("Waiting for a Maggot King loot event.");
		add(new JScrollPane(eventText), BorderLayout.CENTER);
		JPanel statusPanel = new JPanel(new BorderLayout(0, 8));
		rewardText.setEditable(false);
		rewardText.setLineWrap(true);
		rewardText.setWrapStyleWord(true);
		rewardText.setText("Reward option click: waiting");
		statusPanel.add(rewardText, BorderLayout.NORTH);
		statisticsText.setEditable(false);
		statisticsText.setLineWrap(true);
		statisticsText.setWrapStyleWord(true);
		statisticsText.setText("Loading reward summary...");
		statusPanel.add(statisticsText, BorderLayout.CENTER);
		storageText.setEditable(false);
		storageText.setLineWrap(true);
		storageText.setWrapStyleWord(true);
		storageText.setText("Loading profile...");
		statusPanel.add(storageText, BorderLayout.SOUTH);
		add(statusPanel, BorderLayout.SOUTH);
	}

	public void clearObservation()
	{
		SwingUtilities.invokeLater(() -> {
			eventText.setText("Waiting for a Maggot King loot event on this profile.");
			rewardText.setText("Reward option click: waiting");
			rewardSummary = null;
			statisticsStatus = "Loading reward summary...";
			renderStatistics();
		});
	}

	public void showRewardOption(String option, String target, int targetId, int clientTick)
	{
		SwingUtilities.invokeLater(() -> rewardText.setText("Reward option clicked: " + option +
			"\nTarget: " + target + " (#" + targetId + ")\nClient tick: " + clientTick +
			"\nClick observed only; reward not yet confirmed."));
	}

	public void showLootCorrelation(RewardClickCandidate click, int lootTick)
	{
		SwingUtilities.invokeLater(() -> {
			if (click == null)
			{
				rewardText.setText("Reward link: no recent matching click.\nKill not confirmed.");
				return;
			}
			String method = "OPEN_STOMACH".equals(click.getMethod()) ? "Open-stomach" : "Take-eggs";
			rewardText.setText("Possible reward: " + method +
				"\nClick tick: " + click.getClientTick() + "; loot tick: " + lootTick +
				"\nTarget ID: #" + click.getTargetId() +
				"\nTiming match only; kill not confirmed.");
		});
	}

	public void showStorage(String profile, long count, String status)
	{
		SwingUtilities.invokeLater(() -> storageText.setText("Profile: " + profile +
			"\nSaved events: " + (count < 0 ? "—" : count) + "\n" + status));
	}

	public void showStatistics(RewardSummary summary)
	{
		SwingUtilities.invokeLater(() -> {
			rewardSummary = summary;
			renderStatistics();
		});
	}

	public void showStatisticsUnavailable()
	{
		SwingUtilities.invokeLater(() -> {
			rewardSummary = null;
			statisticsStatus = "Reward summary unavailable; check RuneLite log.";
			renderStatistics();
		});
	}

	private void renderStatistics()
	{
		if (rewardSummary == null)
		{
			statisticsText.setText(statisticsStatus);
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
		text.append("\nObserved Take-eggs loot: ").append(stats.getObservedEggRewards());
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
		statisticsText.setText(text.toString());
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
			eventText.setText(text.toString());
			eventText.setCaretPosition(0);
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
