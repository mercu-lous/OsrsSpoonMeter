package com.lootluck.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A read-only snapshot of RuneLite Loot Tracker's cumulative per-source totals. */
public final class ImportedBaseline
{
	private final String profileId;
	private final String bossId;
	private final Instant importedAtUtc;
	private final int lootTrackerCount;
	private final int kills;
	private final long actualGp;
	private final long calculatedItemGp;
	private final long expectedGpPerKill;
	private final Map<Integer, Long> itemQuantities;
	private final Map<Integer, Long> itemUnitGp;

	public ImportedBaseline(String profileId, String bossId, Instant importedAtUtc, int kills,
		long actualGp, long calculatedItemGp, long expectedGpPerKill,
		Map<Integer, Long> itemQuantities, Map<Integer, Long> itemUnitGp)
	{
		this(profileId, bossId, importedAtUtc, kills, kills, actualGp, calculatedItemGp,
			expectedGpPerKill, itemQuantities, itemUnitGp);
	}

	public ImportedBaseline(String profileId, String bossId, Instant importedAtUtc,
		int lootTrackerCount, int kills, long actualGp, long calculatedItemGp,
		long expectedGpPerKill, Map<Integer, Long> itemQuantities, Map<Integer, Long> itemUnitGp)
	{
		if (profileId == null || !profileId.matches("[0-9a-f]{64}"))
		{
			throw new IllegalArgumentException("Invalid profile ID");
		}
		if (BossRegistry.bosses().stream().noneMatch(boss -> boss.getId().equals(bossId)))
		{
			throw new IllegalArgumentException("Unknown boss ID");
		}
		if (lootTrackerCount < 1 || kills < 0 || kills > lootTrackerCount
			|| actualGp < 0 || calculatedItemGp < 0 || expectedGpPerKill < 1)
		{
			throw new IllegalArgumentException("Invalid imported totals");
		}
		this.profileId = profileId;
		this.bossId = bossId;
		this.importedAtUtc = Objects.requireNonNull(importedAtUtc);
		this.lootTrackerCount = lootTrackerCount;
		this.kills = kills;
		this.actualGp = actualGp;
		this.calculatedItemGp = calculatedItemGp;
		this.expectedGpPerKill = expectedGpPerKill;
		this.itemQuantities = checkedItems(itemQuantities, false);
		this.itemUnitGp = checkedItems(itemUnitGp, true);
		if (!this.itemQuantities.keySet().equals(this.itemUnitGp.keySet()))
		{
			throw new IllegalArgumentException("Imported item prices do not match quantities");
		}
		Math.multiplyExact(expectedGpPerKill, kills);
	}

	private static Map<Integer, Long> checkedItems(Map<Integer, Long> values, boolean allowZero)
	{
		Map<Integer, Long> copy = new LinkedHashMap<>();
		for (Map.Entry<Integer, Long> entry : Objects.requireNonNull(values).entrySet())
		{
			if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null
				|| entry.getValue() < (allowZero ? 0 : 1))
			{
				throw new IllegalArgumentException("Invalid imported item");
			}
			copy.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(copy);
	}

	public ImportedBaseline withValues(int eligibleKills, long actual, long expectedPerKill)
	{
		return new ImportedBaseline(profileId, bossId, importedAtUtc,
			lootTrackerCount, eligibleKills, actual,
			calculatedItemGp, expectedPerKill, itemQuantities, itemUnitGp);
	}

	public long getExpectedGp()
	{
		return Math.multiplyExact(expectedGpPerKill, kills);
	}

	public String getProfileId() { return profileId; }
	public String getBossId() { return bossId; }
	public Instant getImportedAtUtc() { return importedAtUtc; }
	public int getLootTrackerCount() { return lootTrackerCount; }
	public int getKills() { return kills; }
	public long getActualGp() { return actualGp; }
	public long getCalculatedItemGp() { return calculatedItemGp; }
	public long getExpectedGpPerKill() { return expectedGpPerKill; }
	public Map<Integer, Long> getItemQuantities() { return itemQuantities; }
	public Map<Integer, Long> getItemUnitGp() { return itemUnitGp; }
}
