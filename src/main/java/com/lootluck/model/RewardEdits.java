package com.lootluck.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Per-profile manual reward changes kept separate from captured loot files. */
public final class RewardEdits
{
	private static final RewardEdits EMPTY = new RewardEdits(Collections.emptyMap());
	private final Map<String, RewardEdit> byGroupId;

	private RewardEdits(Map<String, RewardEdit> byGroupId)
	{
		this.byGroupId = Collections.unmodifiableMap(new LinkedHashMap<>(byGroupId));
	}

	public static RewardEdits empty() { return EMPTY; }
	public RewardEdit get(String groupId) { return byGroupId.get(groupId); }
	public Collection<RewardEdit> all() { return byGroupId.values(); }

	public RewardEdits with(RewardEdit edit)
	{
		Map<String, RewardEdit> updated = new LinkedHashMap<>(byGroupId);
		if (edit.isEmpty())
		{
			updated.remove(edit.getGroupId());
		}
		else
		{
			updated.put(edit.getGroupId(), edit);
		}
		return new RewardEdits(updated);
	}
}
