package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.LootValuation;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
	private final JTextArea storageText = new JTextArea();

	public LootLuckPanel()
	{
		setLayout(new BorderLayout(0, 8));
		add(new JLabel("Loot Luck Tracker"), BorderLayout.NORTH);
		eventText.setEditable(false);
		eventText.setLineWrap(true);
		eventText.setWrapStyleWord(true);
		eventText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		eventText.setText("Waiting for a Maggot King loot event.\n\nGP values and kill attribution are coming next.");
		add(new JScrollPane(eventText), BorderLayout.CENTER);
		JPanel statusPanel = new JPanel(new BorderLayout(0, 8));
		rewardText.setEditable(false);
		rewardText.setLineWrap(true);
		rewardText.setWrapStyleWord(true);
		rewardText.setText("Reward option click: waiting");
		statusPanel.add(rewardText, BorderLayout.NORTH);
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
		});
	}

	public void showRewardOption(String option, String target, int targetId, int clientTick)
	{
		SwingUtilities.invokeLater(() -> rewardText.setText("Reward option clicked: " + option +
			"\nTarget: " + target + " (#" + targetId + ")\nClient tick: " + clientTick +
			"\nClick observed only; reward not yet confirmed."));
	}

	public void showStorage(String profile, long count, String status)
	{
		SwingUtilities.invokeLater(() -> storageText.setText("Profile: " + profile +
			"\nSaved events: " + (count < 0 ? "—" : count) + "\n" + status));
	}

	public void showObservation(LootEventPreview observation, boolean showItemIds)
	{
		SwingUtilities.invokeLater(() -> {
			StringBuilder text = new StringBuilder();
			text.append("Latest loot event\n\n");
			text.append("Source: ").append(observation.getSource()).append('\n');
			text.append("Type: ").append(observation.getType()).append('\n');
			text.append("Observed: ").append(TIME_FORMAT.withZone(ZoneId.systemDefault()).format(observation.getObservedAtUtc())).append('\n');
			text.append("Item stacks: ").append(observation.getItems().size()).append('\n');
			Long actualGp = LootValuation.totalGpIfComplete(observation.getItems());
			text.append("GE loot value estimate: ").append(actualGp == null ? "Unavailable (missing price)" : gp(actualGp)).append("\n\n");
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
					text.append(" @ 0 GP (untradeable)");
				}
				else
				{
					text.append(" @ ").append(item.getUnitPriceGp() == null ? "price unavailable" : gp(item.getUnitPriceGp()) + " each");
				}
				text.append('\n');
			}
			if (hasUntradeable)
			{
				text.append("\nUntradeable items count as 0 GE GP; use and alch value excluded.\n");
			}
			text.append("\nGross GE item estimate only. Expected GP, luck, and kill count are not calculated yet.");
			eventText.setText(text.toString());
			eventText.setCaretPosition(0);
		});
	}

	private static String gp(long amount)
	{
		return String.format(Locale.US, "%,d GP", amount);
	}
}
