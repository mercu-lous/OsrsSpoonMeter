package com.lootluck.model;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Stable identity and source aliases for one supported loot source. */
public final class BossDefinition
{
	private final String id;
	private final String name;
	private final String wikiUrl;
	private final Duration groupingWindow;
	private final Set<String> sourceNames;
	private final BossRules rules;

	public BossDefinition(String id, String name, String wikiUrl, Duration groupingWindow,
		BossRules rules, String... sourceNames)
	{
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.wikiUrl = Objects.requireNonNull(wikiUrl);
		this.groupingWindow = Objects.requireNonNull(groupingWindow);
		if (groupingWindow.isNegative() || groupingWindow.isZero())
		{
			throw new IllegalArgumentException("Grouping window must be positive");
		}
		this.rules = Objects.requireNonNull(rules);
		Set<String> names = new LinkedHashSet<>();
		for (String sourceName : sourceNames)
		{
			names.add(Objects.requireNonNull(sourceName));
		}
		if (names.isEmpty())
		{
			throw new IllegalArgumentException("At least one source name is required");
		}
		this.sourceNames = Collections.unmodifiableSet(names);
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public String getWikiUrl() { return wikiUrl; }
	public Duration getGroupingWindow() { return groupingWindow; }
	public Set<String> getSourceNames() { return sourceNames; }
	public BossRules getRules() { return rules; }

	@Override
	public String toString() { return name; }
}
