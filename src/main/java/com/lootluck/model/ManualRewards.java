package com.lootluck.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Per-profile manual rewards, including entries the user later excluded. */
public final class ManualRewards
{
	private static final ManualRewards EMPTY = new ManualRewards(Collections.emptyMap());
	private final Map<String, ManualReward> byId;

	private ManualRewards(Map<String, ManualReward> byId)
	{
		this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
	}

	public static ManualRewards empty() { return EMPTY; }
	public ManualReward get(String id) { return byId.get(id); }
	public Collection<ManualReward> all() { return byId.values(); }

	public ManualRewards with(ManualReward reward)
	{
		Map<String, ManualReward> updated = new LinkedHashMap<>(byId);
		updated.put(reward.getId(), reward);
		return new ManualRewards(updated);
	}
}
