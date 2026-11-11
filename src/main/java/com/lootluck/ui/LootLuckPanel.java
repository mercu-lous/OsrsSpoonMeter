package com.lootluck.ui;

import com.lootluck.model.LootEventPreview;
import com.lootluck.model.BossDefinition;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.BossRankingEntry;
import com.lootluck.model.ProfileDashboard;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.LootRewardGroup;
import com.lootluck.model.LootValuation;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.ExpectedValueSnapshot;
import com.lootluck.model.FixedExpectedBossRules;
import com.lootluck.model.GauntletOutcome;
import com.lootluck.model.RewardClickCandidate;
import com.lootluck.model.RewardHistoryEntry;
import com.lootluck.model.RewardEdit;
import com.lootluck.model.ManualReward;
import com.lootluck.model.RewardScope;
import com.lootluck.model.RewardStatistics;
import com.lootluck.model.RewardSummary;
import com.lootluck.model.SavedLootEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTextField;
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
	private final JTextArea rankingsText = new JTextArea();
	private final JTextArea editPreview = new JTextArea();
	private final JComboBox<RewardChoice> rewardChoice = new JComboBox<>();
	private final JCheckBox excludeReward = new JCheckBox("Exclude this reward");
	private final JTextField gpCorrection = new JTextField("0");
	private final JTextField editReason = new JTextField();
	private final JLabel editStatus = new JLabel("Select a saved reward.");
	private Consumer<RewardEdit> editHandler;
	private final JTextField manualActualGp = new JTextField();
	private final JTextField manualExpectedGp = new JTextField();
	private final JTextField manualReason = new JTextField();
	private final JButton manualAdd = new JButton("Add manual reward");
	private final JComboBox<ManualChoice> manualChoice = new JComboBox<>();
	private final JTextArea manualPreview = new JTextArea(6, 16);
	private final JTextArea manualStatus = new JTextArea("Enter a missed reward.", 3, 16);
	private final JButton manualToggle = new JButton("Exclude selected");
	private ManualRewardAddHandler manualAddHandler;
	private BiConsumer<String, Boolean> manualToggleHandler;
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
			renderEditChoices();
			renderManualChoices();
		});
		header.add(bossSelector);
		statisticsScope.addActionListener(event -> {
			renderStatistics();
			renderHistory();
			renderRankings();
			renderEditChoices();
			renderManualChoices();
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
		rankingsText.setEditable(false);
		rankingsText.setLineWrap(true);
		rankingsText.setWrapStyleWord(true);
		rankingsText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		rankingsText.setText("Loading boss rankings...");
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Overview", new JScrollPane(overviewText));
		tabs.addTab("History", new JScrollPane(historyText));
		tabs.addTab("Rankings", new JScrollPane(rankingsText));
		tabs.addTab("Edit", buildEditTab());
		tabs.addTab("Manual", buildManualTab());
		tabs.setToolTipTextAt(4, "Add a missed reward and manage manual entries");
		add(tabs, BorderLayout.CENTER);
	}

	@FunctionalInterface
	public interface ManualRewardAddHandler
	{
		void add(BossDefinition boss, long actualGp, Long expectedGp, String note);
	}

	public void setManualRewardHandlers(ManualRewardAddHandler addHandler,
		BiConsumer<String, Boolean> toggleHandler)
	{
		this.manualAddHandler = addHandler;
		this.manualToggleHandler = toggleHandler;
	}

	private JPanel buildManualTab()
	{
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.add(new JLabel("Missed reward for selected boss"));
		form.add(new JLabel("Actual GP received"));
		form.add(manualActualGp);
		form.add(new JLabel("Expected GP (optional)"));
		form.add(manualExpectedGp);
		form.add(new JLabel("Reason (optional)"));
		form.add(manualReason);
		manualAdd.addActionListener(event -> addManualReward());
		form.add(manualAdd);
		manualStatus.setEditable(false);
		manualStatus.setLineWrap(true);
		manualStatus.setWrapStyleWord(true);
		manualStatus.setOpaque(false);
		form.add(manualStatus);
		form.add(new JLabel("Saved manual rewards in this view"));
		manualChoice.addActionListener(event -> loadSelectedManual());
		form.add(manualChoice);
		manualToggle.addActionListener(event -> toggleSelectedManual());
		form.add(manualToggle);
		manualPreview.setEditable(false);
		manualPreview.setLineWrap(true);
		manualPreview.setWrapStyleWord(true);
		manualPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		manualPreview.setText("Manual GP is kept separate from captured loot. Add expected GP only when this reward should affect the GP comparison.");
		form.add(manualPreview);
		for (Component component : form.getComponents())
		{
			JComponent control = (JComponent) component;
			control.setAlignmentX(Component.LEFT_ALIGNMENT);
			control.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				control.getPreferredSize().height + 4));
		}
		JPanel tab = new JPanel(new BorderLayout());
		tab.add(new JScrollPane(form), BorderLayout.CENTER);
		return tab;
	}

	private void addManualReward()
	{
		if (manualAddHandler == null)
		{
			manualStatus.setText("Manual entry unavailable.");
			return;
		}
		try
		{
			String actualText = manualActualGp.getText().trim().replace(",", "");
			String expectedText = manualExpectedGp.getText().trim().replace(",", "");
			if (actualText.isEmpty())
			{
				throw new IllegalArgumentException("Actual GP is required.");
			}
			long actual = Long.parseLong(actualText);
			Long expected = expectedText.isEmpty() ? null : Long.parseLong(expectedText);
			if (actual < 0 || (expected != null && expected <= 0)
				|| manualReason.getText().trim().length() > 160)
			{
				throw new IllegalArgumentException("Use nonnegative actual GP, positive expected GP, and a reason up to 160 characters.");
			}
			manualStatus.setText("Saving manual reward...");
			manualAdd.setEnabled(false);
			manualAddHandler.add(selectedBoss(), actual, expected, manualReason.getText());
		}
		catch (IllegalArgumentException invalid)
		{
			manualAdd.setEnabled(true);
			manualStatus.setText(invalid.getMessage());
		}
	}

	private void toggleSelectedManual()
	{
		ManualChoice selected = (ManualChoice) manualChoice.getSelectedItem();
		if (selected == null || manualToggleHandler == null)
		{
			manualStatus.setText("No manual reward selected.");
			return;
		}
		manualStatus.setText("Saving manual reward...");
		manualToggleHandler.accept(selected.reward.getId(), !selected.reward.isIncluded());
	}

	public void showManualRewardStatus(String status)
	{
		SwingUtilities.invokeLater(() -> {
			manualStatus.setText(status);
			manualAdd.setEnabled(true);
			if ("Manual reward saved.".equals(status))
			{
				manualActualGp.setText("");
				manualExpectedGp.setText("");
				manualReason.setText("");
			}
		});
	}

	private void renderManualChoices()
	{
		ManualChoice previous = (ManualChoice) manualChoice.getSelectedItem();
		String previousId = previous == null ? null : previous.reward.getId();
		manualChoice.removeAllItems();
		if (dashboard != null)
		{
			RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
			if (scope == null)
			{
				scope = RewardScope.ALL_TIME;
			}
			RewardSummary summary = dashboard.get(selectedBoss());
			for (ManualReward reward : summary.getManualRewards(scope))
			{
				manualChoice.addItem(new ManualChoice(reward, summary.getCalendarZone()));
			}
		}
		if (previousId != null)
		{
			for (int index = 0; index < manualChoice.getItemCount(); index++)
			{
				if (previousId.equals(manualChoice.getItemAt(index).reward.getId()))
				{
					manualChoice.setSelectedIndex(index);
					break;
				}
			}
		}
		loadSelectedManual();
	}

	private void loadSelectedManual()
	{
		ManualChoice selected = (ManualChoice) manualChoice.getSelectedItem();
		manualToggle.setEnabled(selected != null);
		if (selected == null)
		{
			manualPreview.setText("No manual rewards in this view.");
			return;
		}
		ManualReward reward = selected.reward;
		manualToggle.setText(reward.isIncluded() ? "Exclude selected" : "Restore selected");
		manualPreview.setText("Recorded: " + TIME_FORMAT.withZone(selected.zone).format(reward.getRecordedAtUtc())
			+ "\nActual: " + gp(reward.getActualGp())
			+ "\nExpected: " + (reward.getExpectedGp() == null ? "Not compared" : gp(reward.getExpectedGp()))
			+ "\nStatus: " + (reward.isIncluded() ? "Included" : "Excluded")
			+ (reward.getNote().isEmpty() ? "" : "\nReason: " + reward.getNote()));
	}

	private static final class ManualChoice
	{
		private final ManualReward reward;
		private final ZoneId zone;

		private ManualChoice(ManualReward reward, ZoneId zone)
		{
			this.reward = reward;
			this.zone = zone;
		}

		@Override
		public String toString()
		{
			return DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(zone)
				.format(reward.getRecordedAtUtc()) + " " + gp(reward.getActualGp())
				+ (reward.isIncluded() ? "" : " (excluded)");
		}
	}

	private JPanel buildEditTab()
	{
		JPanel tab = new JPanel(new BorderLayout(0, 8));
		JPanel controls = new JPanel(new GridLayout(0, 1, 0, 4));
		controls.add(new JLabel("Saved reward"));
		rewardChoice.addActionListener(event -> loadSelectedEdit());
		controls.add(rewardChoice);
		controls.add(excludeReward);
		controls.add(new JLabel("GP correction (+ / -)"));
		controls.add(gpCorrection);
		controls.add(new JLabel("Reason (optional)"));
		controls.add(editReason);
		JButton save = new JButton("Save reward changes");
		save.addActionListener(event -> saveSelectedEdit());
		controls.add(save);
		controls.add(editStatus);
		tab.add(controls, BorderLayout.NORTH);
		editPreview.setEditable(false);
		editPreview.setLineWrap(true);
		editPreview.setWrapStyleWord(true);
		editPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		editPreview.setText("Choose a saved reward. A positive correction adds GP; a negative one removes GP. Expected GP stays the same.");
		tab.add(new JScrollPane(editPreview), BorderLayout.CENTER);
		return tab;
	}

	public void setRewardEditHandler(Consumer<RewardEdit> handler)
	{
		this.editHandler = handler;
	}

	private void saveSelectedEdit()
	{
		RewardChoice selected = (RewardChoice) rewardChoice.getSelectedItem();
		if (selected == null || editHandler == null)
		{
			editStatus.setText("No saved reward selected.");
			return;
		}
		try
		{
			String entered = gpCorrection.getText().trim().replace(",", "");
			long correction = entered.isEmpty() ? 0 : Long.parseLong(entered);
			RewardEdit edit = new RewardEdit(selected.group.getGroupId(),
				excludeReward.isSelected(), correction, editReason.getText(), Instant.now());
			editStatus.setText("Saving changes...");
			editHandler.accept(edit);
		}
		catch (IllegalArgumentException invalid)
		{
			editStatus.setText("Check the GP amount or reason.");
		}
	}

	public void showRewardEditStatus(String status)
	{
		SwingUtilities.invokeLater(() -> editStatus.setText(status));
	}

	private void renderEditChoices()
	{
		RewardChoice previous = (RewardChoice) rewardChoice.getSelectedItem();
		String previousId = previous == null ? null : previous.group.getGroupId();
		rewardChoice.removeAllItems();
		if (dashboard != null)
		{
			RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
			if (scope == null)
			{
				scope = RewardScope.ALL_TIME;
			}
			RewardSummary summary = dashboard.get(selectedBoss());
			for (LootRewardGroup group : summary.getEditableGroups(scope))
			{
				rewardChoice.addItem(new RewardChoice(group, summary.getCalendarZone()));
			}
		}
		if (previousId != null)
		{
			for (int index = 0; index < rewardChoice.getItemCount(); index++)
			{
				if (previousId.equals(rewardChoice.getItemAt(index).group.getGroupId()))
				{
					rewardChoice.setSelectedIndex(index);
					break;
				}
			}
		}
		rewardChoice.setEnabled(rewardChoice.getItemCount() > 0);
		loadSelectedEdit();
	}

	private void loadSelectedEdit()
	{
		RewardChoice selected = (RewardChoice) rewardChoice.getSelectedItem();
		if (selected == null || dashboard == null)
		{
			editPreview.setText("No saved rewards in this view.");
			excludeReward.setSelected(false);
			gpCorrection.setText("0");
			editReason.setText("");
			return;
		}
		RewardEdit edit = dashboard.get(selectedBoss()).getEdit(selected.group.getGroupId());
		excludeReward.setSelected(edit != null && edit.isExcluded());
		gpCorrection.setText(edit == null ? "0" : Long.toString(edit.getGpCorrection()));
		editReason.setText(edit == null ? "" : edit.getNote());
		StringBuilder text = new StringBuilder();
		text.append(TIME_FORMAT.withZone(selected.zone).format(selected.group.getObservedAtUtc()))
			.append("\nGroup: ").append(selected.group.getGroupId(), 0, 8).append('\n');
		for (SavedLootEvent event : selected.group.getEvents())
		{
			for (LootItemPreview item : event.getItems())
			{
				text.append(item.getQuantity()).append(" × ").append(item.getName()).append('\n');
			}
		}
		Long captured = selected.group.getConvertedGp();
		text.append("Captured GP: ").append(captured == null ? "Unavailable" : gp(captured));
		if (edit != null && edit.getGpCorrection() != 0 && captured != null)
		{
			text.append("\nAdjusted GP: ").append(gp(captured + edit.getGpCorrection()));
		}
		text.append("\n\nA positive correction adds GP; a negative one removes GP. ")
			.append("Expected GP stays the same. Excluded rewards leave totals and rankings; uncheck to restore.");
		editPreview.setText(text.toString());
		editPreview.setCaretPosition(0);
	}

	private static final class RewardChoice
	{
		private final LootRewardGroup group;
		private final ZoneId zone;

		private RewardChoice(LootRewardGroup group, ZoneId zone)
		{
			this.group = group;
			this.zone = zone;
		}

		@Override
		public String toString()
		{
			String item = group.getEvents().get(0).getItems().isEmpty() ? "No items"
				: group.getEvents().get(0).getItems().get(0).getName();
			return DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(zone)
				.format(group.getObservedAtUtc()) + " " + item;
		}
	}

	public void clearObservation()
	{
		SwingUtilities.invokeLater(() -> {
			latestEvents.clear();
			latestLinks.clear();
			showSelectedBossSections();
			dashboard = null;
			statisticsStatus = "Loading reward summary...";
			manualAdd.setEnabled(true);
			renderStatistics();
			renderHistory();
			renderRankings();
			renderEditChoices();
			renderManualChoices();
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
			renderRankings();
			renderEditChoices();
			renderManualChoices();
		});
	}

	public void showStatisticsUnavailable()
	{
		SwingUtilities.invokeLater(() -> {
			dashboard = null;
			statisticsStatus = "Reward summary unavailable; check RuneLite log.";
			renderStatistics();
			renderHistory();
			renderRankings();
			renderEditChoices();
			renderManualChoices();
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
		if (stats.getManualRewardCount() > 0)
		{
			text.append("\nManual rewards included: ").append(stats.getManualRewardCount());
			text.append("\nManual GP recorded: ").append(stats.getManualActualGp() == null
				? "Unavailable" : gp(stats.getManualActualGp()));
			text.append("\nManual rewards compared: ").append(stats.getManualComparableRewardCount());
		}
		if (stats.getExcludedRewards() > 0)
		{
			text.append("\nExcluded reward groups: ").append(stats.getExcludedRewards());
		}
		if (stats.getCorrectedRewards() > 0)
		{
			text.append("\nManual GP correction applied: ")
				.append(stats.getAppliedCorrectionGp() == null
					? "Unavailable" : signedGp(stats.getAppliedCorrectionGp()));
		}
		if (GauntletOutcome.isGauntlet(boss))
		{
			text.append("\nPartial-completion chests: ").append(stats.getPartialGauntletRewards());
			text.append("\nOutcome unverified: ").append(stats.getUnclassifiedGauntletRewards());
		}
		if (stats.getPricedNormalRewards() > 0)
		{
			text.append(GauntletOutcome.isGauntlet(boss)
				? "\nPriced completed rewards: " : "\nPriced non-egg rewards: ")
				.append(stats.getPricedNormalRewards());
			if (!GauntletOutcome.isGauntlet(boss))
			{
				text.append(" / ").append(stats.getObservedNormalRewards());
			}
			text.append("\nGP per priced reward: ")
				.append(stats.getAverageGpPerPricedReward() == null
					? "Unavailable" : gp(stats.getAverageGpPerPricedReward()));
		}
		if (boss == BossRegistry.MAGGOT_KING)
		{
			text.append("\nOpen-stomach or unlinked groups: ")
				.append(stats.getObservedNormalRewards());
			text.append("\nComparable rewards (including manual): ").append(stats.getValuedNormalRewards());
			text.append("\nTake-eggs loot-linked: ").append(stats.getObservedEggRewards());
			text.append("\nConfirmed no-loot Take-eggs: ")
				.append(rewardSummary.getConfirmedNoLoot(scope));
		}
		else if (stats.getValuedNormalRewards() > 0)
		{
			text.append("\nComparable rewards: ").append(stats.getValuedNormalRewards());
		}
		if (stats.getNetLuckGp() != null)
		{
			text.append("\nComparable actual GP: ")
				.append(gp(stats.getConvertedGp()));
			text.append("\nExpected for comparable rewards: ")
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
			text.append(GauntletOutcome.isGauntlet(boss)
				? "\nNo priced, completion-confirmed rewards available for GP comparison."
				: "\nGP comparison unavailable for an incomplete reward value.");
		}
		if (boss != BossRegistry.MAGGOT_KING)
		{
			text.append("\nCaptured rewards use the fixed Wiki baseline; manual rewards use entered expected GP. Prices and untradeable conversions are not updated.");
		}
		text.append("\nUnpriced captured items count as 0 GP. Captured callbacks are grouped within five seconds; not confirmed kill count.\n");
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
			else if (entry.isManualReward())
			{
				ManualReward reward = entry.getManualReward();
				text.append("Manual reward")
					.append(reward.isIncluded() ? " (included)\n" : " (excluded)\n");
				text.append("Actual: ").append(gp(reward.getActualGp())).append('\n');
				text.append("Expected: ").append(reward.getExpectedGp() == null
					? "Not compared" : gp(reward.getExpectedGp())).append('\n');
				if (!reward.getNote().isEmpty())
				{
					text.append("Reason: ").append(reward.getNote()).append('\n');
				}
			}
			else
			{
				SavedLootEvent event = entry.getLootEvent();
				RewardEdit edit = rewardSummary.getEditForEvent(event.getEventId());
				if (edit != null)
				{
					if (edit.isExcluded())
					{
						text.append("Excluded from totals and rankings\n");
					}
					if (edit.getGpCorrection() != 0)
					{
						text.append("Group GP correction: ")
							.append(signedGp(edit.getGpCorrection())).append('\n');
					}
					if (!edit.getNote().isEmpty())
					{
						text.append("Reason: ").append(edit.getNote()).append('\n');
					}
				}
				if (GauntletOutcome.isGauntlet(selectedBoss()))
				{
					text.append(gauntletOutcomeLabel(event.getGauntletOutcome())).append('\n');
				}
				RewardClickCandidate click = event.getRewardClick();
				text.append(click == null ? "Loot callback" :
					"OPEN_STOMACH".equals(click.getMethod()) ? "Open-stomach timing link" : "Take-eggs timing link")
					.append(" | event ").append(event.getEventId(), 0, 8).append('\n');
				for (LootItemPreview item : event.getItems())
				{
					text.append(item.getQuantity()).append(" × ").append(item.getName())
						.append(" (#").append(item.getItemId()).append(')');
					if (LootValuation.MISSING_PRICE_ASSUMED_ZERO.equals(item.getPriceSource()))
					{
						text.append(" @ 0 GP direct each (missing price assumed zero)\n");
					}
					else
					{
						text.append(item.getUnitPriceGp() == null ? " @ direct price unavailable"
							: " @ " + gp(item.getUnitPriceGp()) + " direct each").append('\n');
					}
					ConversionQuote quote = item.getConversionQuote();
					if (quote != null && quote.getStackGp() != null
						&& !"DIRECT_GE".equals(quote.getMethod())
						&& !"NO_GE_CONVERSION".equals(quote.getMethod())
						&& !"MISSING_DIRECT_PRICE_ASSUMED_ZERO".equals(quote.getMethod()))
					{
						text.append("  ").append(conversionLabel(quote)).append('\n');
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
				if (event.getExpectedSnapshot() != null
					&& event.getGauntletOutcome() != GauntletOutcome.PARTIAL
					&& event.getGauntletOutcome() != GauntletOutcome.UNKNOWN)
				{
					Long expectedGp = event.getExpectedSnapshot().getExpectedGp();
					text.append("Expected for reward group: ")
						.append(expectedGp == null ? "Unavailable" : gp(expectedGp)).append('\n');
				}
				else if (event.getExpectedSnapshot() != null)
				{
					text.append("Older expected snapshot ignored: completion not confirmed.\n");
				}
			}
			text.append('\n');
		}
		historyText.setText(text.toString());
		historyText.setCaretPosition(0);
	}

	private void renderRankings()
	{
		if (dashboard == null)
		{
			rankingsText.setText(statisticsStatus);
			return;
		}
		RewardScope scope = (RewardScope) statisticsScope.getSelectedItem();
		if (scope == null)
		{
			scope = RewardScope.ALL_TIME;
		}
		StringBuilder text = new StringBuilder("Boss GP ranking — ")
			.append(scope).append("\n\n");
		java.util.List<BossRankingEntry> rankings = dashboard.getRankings(scope);
		if (rankings.isEmpty())
		{
			text.append("No comparable rewards in this view.\n");
		}
		for (int i = 0; i < rankings.size(); i++)
		{
			BossRankingEntry ranking = rankings.get(i);
			RewardStatistics stats = ranking.getStatistics();
			text.append(i + 1).append(". ").append(ranking.getBoss().getName())
				.append("  ").append(signedPercent(ranking.getPercentVsExpected())).append('\n');
			text.append("   ").append(stats.getValuedNormalRewards())
				.append(" comparable rewards\n");
			text.append("   ").append(signedGp(stats.getNetLuckGp()))
				.append(" vs expected\n");
			text.append("   ").append(ranking.getBoss() == BossRegistry.MAGGOT_KING
				? "Versioned drop table" : "Fixed Wiki baseline");
			if (stats.getManualComparableRewardCount() > 0)
			{
				text.append(" + entered manual baseline");
			}
			text.append("\n\n");
		}
		text.append("Ranked by GP percentage for rewards with saved expected values. "
			+ "Small samples can swing widely. This is not a rare-drop percentile or confirmed kill ranking.");
		rankingsText.setText(text.toString());
		rankingsText.setCaretPosition(0);
	}

	public void showObservation(LootEventPreview observation, boolean showItemIds,
		ExpectedValueSnapshot expected, GauntletOutcome gauntletOutcome)
	{
		SwingUtilities.invokeLater(() -> {
			BossDefinition boss = BossRegistry.findBySource(observation.getSource());
			StringBuilder text = new StringBuilder();
			text.append("Latest loot event\n\n");
			text.append("Source: ").append(observation.getSource()).append('\n');
			text.append("Type: ").append(observation.getType()).append('\n');
			text.append("Observed: ").append(TIME_FORMAT.withZone(ZoneId.systemDefault()).format(observation.getObservedAtUtc())).append('\n');
			text.append("Item stacks: ").append(observation.getItems().size()).append('\n');
			if (GauntletOutcome.isGauntlet(boss))
			{
				text.append(gauntletOutcomeLabel(gauntletOutcome)).append('\n');
			}
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
			boolean hasAssumedZero = false;
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
				else if (LootValuation.MISSING_PRICE_ASSUMED_ZERO.equals(item.getPriceSource()))
				{
					hasAssumedZero = true;
					text.append(" @ 0 GP (missing price assumed zero)");
				}
				else
				{
					text.append(" @ ").append(item.getUnitPriceGp() == null ? "price unavailable" : gp(item.getUnitPriceGp()) + " each");
				}
				text.append('\n');
				ConversionQuote quote = item.getConversionQuote();
				if (quote != null && !"DIRECT_GE".equals(quote.getMethod())
					&& !"NO_GE_CONVERSION".equals(quote.getMethod())
					&& !"MISSING_DIRECT_PRICE".equals(quote.getMethod())
					&& !"MISSING_DIRECT_PRICE_ASSUMED_ZERO".equals(quote.getMethod()))
				{
					text.append("  ").append(conversionLabel(quote)).append('\n');
				}
			}
			if (hasUntradeable)
			{
				text.append("\nUntradeable items count as 0 direct GE GP. HA and conversion estimates are shown separately.\n");
			}
			if (hasAssumedZero)
			{
				text.append("\nMissing direct prices were counted as 0 GP in this estimate.\n");
			}
			if (boss == BossRegistry.MAGGOT_KING)
			{
				text.append("\nConversion estimate uses captured output prices and assumes required skills and inputs. Costs and GE fees excluded. GP differences count observed rewards; total kill count is not inferred.");
			}
			else
			{
				text.append("\nDirect-only estimate uses captured RuneLite prices. ");
				if (GauntletOutcome.isGauntlet(boss) && gauntletOutcome != GauntletOutcome.COMPLETED)
				{
					text.append("Completion-average GP comparison is excluded for this chest. ");
				}
				else
				{
					text.append("The fixed expected GP estimate may drift as prices change. ");
				}
				text.append("Reward groups are provisional, not confirmed kill count.");
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

	private static String gauntletOutcomeLabel(GauntletOutcome outcome)
	{
		switch (outcome)
		{
			case COMPLETED:
				return "Gauntlet completion: confirmed by game counter";
			case PARTIAL:
				return "Gauntlet completion: partial reward (failed run)";
			default:
				return "Gauntlet completion: unverified";
		}
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
			case "MISSING_CONVERSION_PRICE_ASSUMED_ZERO":
				return "Conversion value: 0 GP (missing output price assumed zero)";
			case "UNSUPPORTED_UNTRADEABLE_ASSUMED_ZERO":
				return "After conversion: 0 GP (unsupported; assumed zero)";
			default:
				return "After conversion: " + gp(value);
		}
	}
}
