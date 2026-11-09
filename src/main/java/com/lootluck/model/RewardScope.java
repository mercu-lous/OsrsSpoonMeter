package com.lootluck.model;

/** Scopes available in the observed-reward summary. */
public enum RewardScope
{
	ALL_TIME("All time"),
	CURRENT_RUN("Current plugin run"),
	TODAY("Today"),
	THIS_WEEK("This week"),
	THIS_MONTH("This month"),
	THIS_YEAR("This year");

	private final String label;

	RewardScope(String label)
	{
		this.label = label;
	}

	public boolean isCalendar()
	{
		return this != ALL_TIME && this != CURRENT_RUN;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
