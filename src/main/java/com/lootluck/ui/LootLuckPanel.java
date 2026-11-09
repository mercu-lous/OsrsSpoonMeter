package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.LootItemPreview;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

public final class LootLuckPanel extends PluginPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
	private final JTextArea eventText = new JTextArea();

	public LootLuckPanel()
	{
		setLayout(new BorderLayout(0, 8));
		add(new JLabel("Loot Luck Tracker"), BorderLayout.NORTH);
		eventText.setEditable(false);
		eventText.setLineWrap(true);
		eventText.setWrapStyleWord(true);
		eventText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		eventText.setText("Waiting for a RuneLite Loot Tracker event.\n\nThis preview is not saved yet. GP values and kill attribution are coming next.");
		add(new JScrollPane(eventText), BorderLayout.CENTER);
	}

	public void showObservation(LootEventPreview observation, boolean showItemIds)
	{
		SwingUtilities.invokeLater(() -> {
			StringBuilder text = new StringBuilder();
			text.append("Latest loot event\n\n");
			text.append("Source: ").append(observation.getSource()).append('\n');
			text.append("Type: ").append(observation.getType()).append('\n');
			text.append("Observed: ").append(TIME_FORMAT.withZone(ZoneId.systemDefault()).format(observation.getObservedAtUtc())).append('\n');
			text.append("Item stacks: ").append(observation.getItems().size()).append("\n\n");
			for (LootItemPreview item : observation.getItems())
			{
				text.append(item.getQuantity()).append(" × item");
				if (showItemIds)
				{
					text.append(" #").append(item.getItemId());
				}
				text.append('\n');
			}
			text.append("\nPreview only: no GP calculation, kill count, or persistence yet.");
			eventText.setText(text.toString());
			eventText.setCaretPosition(0);
		});
	}
}
