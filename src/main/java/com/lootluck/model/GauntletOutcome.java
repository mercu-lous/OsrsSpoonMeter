package com.lootluck.model;

import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/** Completion evidence for a Gauntlet chest callback. */
public enum GauntletOutcome
{
	NOT_APPLICABLE,
	COMPLETED,
	PARTIAL,
	UNKNOWN;

	/** Item IDs and quantities found only on the Wiki's partial-completion table. */
	private static final Map<Integer, IntPredicate> PARTIAL_ONLY_REWARDS = Map.of(
		854, quantity -> quantity >= 8 && quantity <= 11, // Noted maple shortbows.
		555, quantity -> quantity >= 200 && quantity <= 300 // Water runes.
	);

	public static boolean isGauntlet(BossDefinition boss)
	{
		return boss == BossRegistry.GAUNTLET || boss == BossRegistry.CORRUPTED_GAUNTLET;
	}

	/** Recognizes the observed partial-completion reward without changing old event files. */
	public static GauntletOutcome fromLegacy(String source, List<LootItemPreview> items)
	{
		if (!isGauntlet(BossRegistry.findBySource(source)))
		{
			return NOT_APPLICABLE;
		}
		return isKnownPartialReward(items) ? PARTIAL : UNKNOWN;
	}

	/** A single stack from a partial-only quantity range identifies a failed chest. */
	public static boolean isKnownPartialReward(List<LootItemPreview> items)
	{
		if (items.size() != 1)
		{
			return false;
		}
		LootItemPreview item = items.get(0);
		IntPredicate quantityRange = PARTIAL_ONLY_REWARDS.get(item.getItemId());
		return quantityRange != null && quantityRange.test(item.getQuantity());
	}
}
