package com.lootluck.storage;

import com.google.gson.Gson;
import com.lootluck.model.NoLootConfirmation;
import com.lootluck.model.RewardClickCandidate;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NoLootConfirmationCodecTest
{
	private static final String PROFILE = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	private final NoLootConfirmationCodec codec = new NoLootConfirmationCodec(new Gson());

	@Test
	public void recognizesOnlyTheSpecificGameMessage()
	{
		assertTrue(NoLootConfirmation.isNoLootText("  The eggs pop as you try to take them.  "));
		assertFalse(NoLootConfirmation.isNoLootText("You pop a maggot egg."));
		assertFalse(NoLootConfirmation.isSupportedChatType("PUBLICCHAT"));
		assertTrue(NoLootConfirmation.isSupportedChatType("GAMEMESSAGE"));
		assertTrue(NoLootConfirmation.isSupportedChatType("SPAM"));
	}

	@Test
	public void savesEvidenceAndClickIdentityAcrossJsonRoundTrip()
	{
		RewardClickCandidate click = new RewardClickCandidate("d5f88875-332c-429c-8adc-59b44b669a41",
			PROFILE, "TAKE_EGGS", 500, 6514, Instant.parse("2026-11-09T12:00:00Z"));
		NoLootConfirmation confirmation = new NoLootConfirmation(click, "session-one",
			Instant.parse("2026-11-09T12:00:01Z"), 502, "GAMEMESSAGE");
		String json = codec.encode(confirmation);
		NoLootConfirmation restored = codec.decode(json);
		assertEquals(PROFILE, restored.getProfileId());
		assertEquals("session-one", restored.getSessionId());
		assertEquals(502, restored.getClientTick());
		assertEquals(click.getClickId(), restored.getClick().getClickId());
		assertEquals(click.getTargetId(), restored.getClick().getTargetId());
		assertEquals("GAMEMESSAGE", restored.getChatType());
		assertTrue(json.contains("\"confirmationType\":\"TAKE_EGGS_NO_LOOT_CHAT\""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnrecognizedChatMessageInSavedRecord()
	{
		RewardClickCandidate click = new RewardClickCandidate("d5f88875-332c-429c-8adc-59b44b669a41",
			PROFILE, "TAKE_EGGS", 500, 6514, Instant.parse("2026-11-09T12:00:00Z"));
		NoLootConfirmation confirmation = new NoLootConfirmation(click, "session-one",
			Instant.parse("2026-11-09T12:00:01Z"), 502, "SPAM");
		codec.decode(codec.encode(confirmation).replace(NoLootConfirmation.GAME_MESSAGE, "Different message"));
	}
}
