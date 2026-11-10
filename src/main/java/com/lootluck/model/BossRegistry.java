package com.lootluck.model;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Curated source-to-boss table. Add aliases after verifying RuneLite's loot source name. */
public final class BossRegistry
{
	private static final BossRules MAGGOT_RULES = new MaggotKingBossRules();
	private static final BossRules DIRECT_GE_RULES = new DirectGeBossRules();
	public static final BossDefinition MAGGOT_KING = new BossDefinition("MAGGOT_KING", "Maggot King",
		"https://oldschool.runescape.wiki/w/Maggot_King", Duration.ofSeconds(5), MAGGOT_RULES, "Maggot King");
	public static final BossDefinition VORKATH = new BossDefinition("VORKATH", "Vorkath",
		"https://oldschool.runescape.wiki/w/Vorkath", Duration.ofSeconds(5), DIRECT_GE_RULES, "Vorkath");
	public static final BossDefinition ZULRAH = new BossDefinition("ZULRAH", "Zulrah",
		"https://oldschool.runescape.wiki/w/Zulrah", Duration.ofSeconds(5), DIRECT_GE_RULES, "Zulrah");
	private static final List<BossDefinition> BOSSES = Collections.unmodifiableList(
		Arrays.asList(MAGGOT_KING, VORKATH, ZULRAH));
	private static final Map<String, BossDefinition> BY_SOURCE = sourceMap();

	private BossRegistry()
	{
	}

	public static List<BossDefinition> bosses() { return BOSSES; }

	public static BossDefinition findBySource(String source)
	{
		return source == null ? null : BY_SOURCE.get(normalize(source));
	}

	private static Map<String, BossDefinition> sourceMap()
	{
		Map<String, BossDefinition> result = new LinkedHashMap<>();
		for (BossDefinition boss : BOSSES)
		{
			for (String source : boss.getSourceNames())
			{
				BossDefinition previous = result.putIfAbsent(normalize(source), boss);
				if (previous != null)
				{
					throw new IllegalStateException("Duplicate boss loot source: " + source);
				}
			}
		}
		return Collections.unmodifiableMap(result);
	}

	private static String normalize(String source)
	{
		return source.trim().toLowerCase(Locale.ROOT);
	}
}
