package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.RewardEdit;
import com.lootluck.model.RewardEdits;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RewardEditCodecTest
{
	@Test
	public void savesAndRestoresIndependentEdits()
	{
		String first = "a798a7a2-7b4b-4d82-8e2b-d16a42db942b";
		String second = "d5f88875-332c-429c-8adc-59b44b669a41";
		Instant time = Instant.parse("2026-11-10T19:00:00Z");
		RewardEdits edits = RewardEdits.empty()
			.with(new RewardEdit(first, true, 0, "Failed run", time))
			.with(new RewardEdit(second, false, -5000, "Left item", time));
		RewardEditCodec codec = new RewardEditCodec(new Gson());
		String json = codec.encode(edits);
		RewardEdits restored = codec.decode(json);
		assertTrue(json.contains("\"schemaVersion\":1"));
		assertTrue(restored.get(first).isExcluded());
		assertEquals(-5000, restored.get(second).getGpCorrection());
		assertEquals("Left item", restored.get(second).getNote());
		assertEquals(time, restored.get(second).getEditedAtUtc());
	}
}
