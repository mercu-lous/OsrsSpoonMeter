package com.lootluck.model;

import java.math.BigDecimal;
import java.util.Objects;

/** A boss with at least one comparable reward in the selected scope. */
public final class BossRankingEntry
{
	private final BossDefinition boss;
	private final RewardStatistics statistics;
	private final BigDecimal percentVsExpected;

	BossRankingEntry(BossDefinition boss, RewardStatistics statistics)
	{
		this.boss = Objects.requireNonNull(boss);
		this.statistics = Objects.requireNonNull(statistics);
		this.percentVsExpected = Objects.requireNonNull(statistics.getPercentVsExpected());
	}

	public BossDefinition getBoss() { return boss; }
	public RewardStatistics getStatistics() { return statistics; }
	public BigDecimal getPercentVsExpected() { return percentVsExpected; }
}
