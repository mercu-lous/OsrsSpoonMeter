package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ManualReward;
import com.lootluck.model.ManualRewards;
import com.lootluck.model.RewardScope;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManualRewardStoreTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final Instant RECORDED = Instant.parse("2026-11-10T18:00:00Z");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void savesAndRestoresManualRewardWithoutCreatingLootEvent() throws Exception
	{
		Path base = folder.getRoot().toPath();
		Constructor<Filepath> constructor = Filepath.class.getDeclaredConstructor(Path.class, Path.class);
		constructor.setAccessible(true);
		Filepath pluginDirectory = constructor.newInstance(base, base);
		Gson gson = new Gson();
		LootEventStore store = new LootEventStore(pluginDirectory, new LootEventCodec(gson),
			new NoLootConfirmationCodec(gson), new RewardEditCodec(gson),
			new ManualRewardCodec(gson), new ImportedBaselineCodec(gson));
		ManualReward reward = new ManualReward(UUID.randomUUID().toString(), PROFILE,
			BossRegistry.VORKATH.getId(), "run", RECORDED, 200000, 144404L,
			"Missed callback", true);
		store.saveManualReward(reward);
		assertEquals(0, store.count(PROFILE));
		assertTrue(java.nio.file.Files.exists(base.resolve("profiles").resolve(PROFILE)
			.resolve("manual-rewards.json")));
		assertEquals(Long.valueOf(55596), store.statistics(PROFILE, "run", ZoneOffset.UTC)
			.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getNetLuckGp());

		store.saveManualReward(reward.withIncluded(false));
		ManualRewards excluded = store.readManualRewards(PROFILE);
		assertFalse(excluded.get(reward.getId()).isIncluded());
		assertEquals(0, store.statistics(PROFILE, "run", ZoneOffset.UTC)
			.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getManualRewardCount());
		store.saveManualReward(reward.withIncluded(true));
		assertEquals(1, store.statistics(PROFILE, "run", ZoneOffset.UTC)
			.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getManualRewardCount());

		try
		{
			store.saveManualReward(new ManualReward(reward.getId(), PROFILE,
				BossRegistry.VORKATH.getId(), "run", RECORDED, 300000, 144404L,
				"Missed callback", true));
			fail("Saved manual amount must be immutable");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(expected.getMessage().contains("included or excluded"));
		}
		assertEquals(200000, store.readManualRewards(PROFILE).get(reward.getId()).getActualGp());
	}
}
