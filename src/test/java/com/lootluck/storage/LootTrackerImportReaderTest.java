package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ImportedBaseline;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LootTrackerImportReaderTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Test
	public void importsOnlySelectedBossAndCombinesItemQuantities()
	{
		String first = "{\"type\":\"NPC\",\"name\":\"Vorkath\",\"kills\":2,"
			+ "\"first\":\"2026-11-01T00:00:00Z\",\"last\":\"2026-11-10T00:00:00Z\","
			+ "\"drops\":[22124,4,995,1000]}";
		String second = "{\"type\":\"NPC\",\"name\":\"Vorkath\",\"kills\":1,\"drops\":[22124,2]}";
		String unrelated = "{\"type\":\"NPC\",\"name\":\"Zulrah\",\"kills\":50,\"drops\":[995,9000]}";
		LootTrackerImportReader.Snapshot snapshot = LootTrackerImportReader.read(new Gson(),
			Arrays.asList(first, unrelated, second), BossRegistry.VORKATH);
		assertEquals(3, snapshot.getKills());
		assertEquals(Long.valueOf(6), snapshot.getQuantities().get(22124));
		assertEquals(Long.valueOf(1000), snapshot.getQuantities().get(995));
	}

	@Test
	public void freezesItemCountsAndPricesInBaselineFile()
	{
		ImportedBaseline baseline = new ImportedBaseline(PROFILE, "VORKATH",
			Instant.parse("2026-11-11T01:00:00Z"), 3, 2, 500000, 499000, 144404,
			Collections.singletonMap(22124, 6L), Collections.singletonMap(22124, 83166L));
		ImportedBaselineCodec codec = new ImportedBaselineCodec(new Gson());
		ImportedBaseline decoded = codec.decode(codec.encode(baseline));
		assertEquals(3, decoded.getLootTrackerCount());
		assertEquals(2, decoded.getKills());
		assertEquals(500000, decoded.getActualGp());
		assertEquals(288808, decoded.getExpectedGp());
		assertEquals(Map.of(22124, 6L), decoded.getItemQuantities());
		assertEquals(Map.of(22124, 83166L), decoded.getItemUnitGp());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsOddItemArray()
	{
		LootTrackerImportReader.read(new Gson(), Collections.singletonList(
			"{\"type\":\"NPC\",\"name\":\"Vorkath\",\"kills\":1,\"drops\":[22124]}"),
			BossRegistry.VORKATH);
	}
}
