package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ImportedBaseline;
import com.lootluck.model.RewardScope;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class ImportedBaselineStoreTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void latestSnapshotSurvivesRestartWithoutAddingEarlierSnapshot() throws Exception
	{
		LootEventStore firstStore = store();
		firstStore.saveImportedBaseline(baseline("2026-11-11T01:00:00Z", 10, 1_000_000));
		firstStore.saveImportedBaseline(baseline("2026-11-11T02:00:00Z", 12, 1_200_000));
		LootEventStore reopened = store();
		ImportedBaseline latest = reopened.readLatestBaselines(PROFILE).get("VORKATH");
		assertEquals(12, latest.getKills());
		assertEquals(1_200_000, reopened.statistics(PROFILE, "run", ZoneOffset.UTC)
			.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getConvertedGp());
	}

	private ImportedBaseline baseline(String timestamp, int kills, long actualGp)
	{
		return new ImportedBaseline(PROFILE, "VORKATH", Instant.parse(timestamp), kills,
			actualGp, actualGp, 144404,
			Collections.singletonMap(995, actualGp), Collections.singletonMap(995, 1L));
	}

	private LootEventStore store() throws Exception
	{
		Path base = folder.getRoot().toPath();
		Constructor<Filepath> constructor = Filepath.class.getDeclaredConstructor(Path.class, Path.class);
		constructor.setAccessible(true);
		Filepath directory = constructor.newInstance(base, base);
		Gson gson = new Gson();
		return new LootEventStore(directory, new LootEventCodec(gson),
			new NoLootConfirmationCodec(gson), new RewardEditCodec(gson),
			new ManualRewardCodec(gson), new ImportedBaselineCodec(gson));
	}
}
