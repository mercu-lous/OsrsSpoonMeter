package com.lootluck.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A temporary observation. This is not a kill record or persistent history. */
public final class LootEventPreview
{
	private final String source;
	private final String type;
	private final Instant observedAtUtc;
	private final List<LootItemPreview> items;

	public LootEventPreview(String source, String type, Instant observedAtUtc, List<LootItemPreview> items)
	{
		this.source = source;
		this.type = type;
		this.observedAtUtc = observedAtUtc;
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
	}

	public String getSource()
	{
		return source;
	}

	public String getType()
	{
		return type;
	}

	public Instant getObservedAtUtc()
	{
		return observedAtUtc;
	}

	public List<LootItemPreview> getItems()
	{
		return items;
	}
}
