package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.BossRegistry;
import com.lootluck.model.ConversionQuote;
import com.lootluck.model.LootItemPreview;
import com.lootluck.model.ProfileDashboard;
import com.lootluck.model.RewardEdit;
import com.lootluck.model.RewardScope;
import com.lootluck.model.SavedLootEvent;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RewardEditStoreTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private static final String EVENT_ID = "a798a7a2-7b4b-4d82-8e2b-d16a42db942b";
	private static final Instant OBSERVED = Instant.parse("2026-11-10T18:00:00Z");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void savesReplacesAndRestoresEditsWithoutChangingLootFile() throws Exception
	{
		Path base = folder.getRoot().toPath();
		// Filepath is normally supplied by RuneLite's Plugin.getPluginDirectory().
		Constructor<Filepath> constructor = Filepath.class.getDeclaredConstructor(Path.class, Path.class);
		constructor.setAccessible(true);
		Filepath pluginDirectory = constructor.newInstance(base, base);
		Gson gson = new Gson();
		LootEventStore store = new LootEventStore(pluginDirectory, new LootEventCodec(gson),
			new NoLootConfirmationCodec(gson), new RewardEditCodec(gson));
		ConversionQuote quote = new ConversionQuote("DIRECT_GE", 10000L, null,
			"RUNELITE_JAGEX", 1, Collections.emptyList());
		SavedLootEvent event = new SavedLootEvent(EVENT_ID, PROFILE, "run", OBSERVED,
			"Vorkath", "NPC", 0, 1, 102,
			Collections.singletonList(new LootItemPreview(450, 1, "Test", 10000L,
				"RUNELITE_JAGEX", null, quote)), null,
			BossRegistry.VORKATH.getRules().expected(null, id -> 1L,
				"RUNELITE_JAGEX", OBSERVED));
		store.save(event);
		String original = java.nio.file.Files.readString(base.resolve("profiles")
			.resolve(PROFILE).resolve("events").resolve(EVENT_ID + ".json"));

		store.saveRewardEdit(PROFILE, new RewardEdit(EVENT_ID, false, -5000,
			"Left item", OBSERVED.plusSeconds(10)));
		ProfileDashboard corrected = store.statistics(PROFILE, "run", ZoneOffset.UTC);
		assertEquals(Long.valueOf(5000), corrected.get(BossRegistry.VORKATH)
			.get(RewardScope.ALL_TIME).getObservedConvertedGp());
		assertEquals("Left item", corrected.get(BossRegistry.VORKATH)
			.getEditForEvent(EVENT_ID).getNote());

		store.saveRewardEdit(PROFILE, new RewardEdit(EVENT_ID, true, -5000,
			"Exclude run", OBSERVED.plusSeconds(20)));
		ProfileDashboard excluded = store.statistics(PROFILE, "run", ZoneOffset.UTC);
		assertEquals(0, excluded.get(BossRegistry.VORKATH)
			.get(RewardScope.ALL_TIME).getObservedLootRewards());
		assertEquals(1, excluded.get(BossRegistry.VORKATH)
			.get(RewardScope.ALL_TIME).getExcludedRewards());

		store.saveRewardEdit(PROFILE, new RewardEdit(EVENT_ID, false, 0, "",
			OBSERVED.plusSeconds(30)));
		ProfileDashboard restored = store.statistics(PROFILE, "run", ZoneOffset.UTC);
		assertEquals(Long.valueOf(10000), restored.get(BossRegistry.VORKATH)
			.get(RewardScope.ALL_TIME).getObservedConvertedGp());
		assertEquals(null, restored.get(BossRegistry.VORKATH).getEditForEvent(EVENT_ID));
		assertEquals(original, java.nio.file.Files.readString(base.resolve("profiles")
			.resolve(PROFILE).resolve("events").resolve(EVENT_ID + ".json")));

		try
		{
			store.saveRewardEdit(PROFILE, new RewardEdit(EVENT_ID, false, -20000,
				"Too much", OBSERVED.plusSeconds(40)));
			fail("Expected invalid negative adjusted GP to be rejected");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(expected.getMessage().contains("negative"));
		}
		assertEquals(Long.valueOf(10000), store.statistics(PROFILE, "run", ZoneOffset.UTC)
			.get(BossRegistry.VORKATH).get(RewardScope.ALL_TIME).getObservedConvertedGp());
	}
}
