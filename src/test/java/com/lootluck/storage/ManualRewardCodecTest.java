package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.ManualReward;
import com.lootluck.model.ManualRewards;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ManualRewardCodecTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	@Test
	public void olderBlankExpectedRewardGainsOneKillAndFixedBossDefault()
	{
		String id = UUID.randomUUID().toString();
		String json = "{\"schemaVersion\":1,\"rewards\":[{\"id\":\"" + id
			+ "\",\"profileId\":\"" + PROFILE
			+ "\",\"bossId\":\"CORRUPTED_GAUNTLET\",\"sessionId\":\"run\","
			+ "\"recordedAtUtc\":\"2026-11-10T20:00:00Z\",\"actualGp\":139041,"
			+ "\"expectedGp\":null,\"note\":\"Missed chest\",\"included\":true}]}";
		ManualRewardCodec codec = new ManualRewardCodec(new Gson());
		ManualReward restored = codec.decode(json).get(id);
		assertEquals(1, restored.getKillCountIncrease());
		assertEquals(Long.valueOf(550380), restored.getExpectedGpPerKill());
		assertTrue(restored.usesBossDefault());
		String upgraded = codec.encode(ManualRewards.empty().with(restored));
		assertTrue(upgraded.contains("\"schemaVersion\":2"));
		assertEquals(Long.valueOf(550380), codec.decode(upgraded).get(id).getExpectedGp());
	}

	@Test
	public void storedNullBaselineStaysNullWhenNoBossDefaultExists()
	{
		String id = UUID.randomUUID().toString();
		ManualReward reward = ManualReward.fromStored(id, PROFILE, "MAGGOT_KING", "run",
			Instant.parse("2026-11-10T20:00:00Z"), 0, null, 3, true, "", true);
		ManualReward restored = new ManualRewardCodec(new Gson()).decode(
			new ManualRewardCodec(new Gson()).encode(ManualRewards.empty().with(reward))).get(id);
		assertEquals(3, restored.getKillCountIncrease());
		assertEquals(null, restored.getExpectedGp());
	}
}
