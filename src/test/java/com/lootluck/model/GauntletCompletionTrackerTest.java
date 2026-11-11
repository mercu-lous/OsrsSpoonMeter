package com.lootluck.model;

import java.time.Instant;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GauntletCompletionTrackerTest
{
	private static final String PROFILE = "profile-one";
	private static final Instant START = Instant.parse("2026-11-10T18:53:39Z");

	@Test
	public void recognizesConfirmedPartialRewardWithoutCountingItAsCompletion()
	{
		GauntletCompletionTracker tracker = new GauntletCompletionTracker();
		tracker.observe(PROFILE, 3, 7);
		tracker.observe(PROFILE, 3, 8);
		assertEquals(GauntletOutcome.PARTIAL, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, mapleShortbows(), START));
		assertEquals(GauntletOutcome.UNKNOWN, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START.plusSeconds(6)));
		assertEquals(GauntletOutcome.PARTIAL, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET,
			Collections.singletonList(new LootItemPreview(555, 300)), START.plusSeconds(12)));
	}

	@Test
	public void ignoresRegularCounterIncreaseThatPrecedesCorruptedCounterIncrease()
	{
		GauntletCompletionTracker tracker = new GauntletCompletionTracker();
		tracker.observe(PROFILE, 3, 7);
		tracker.observe(PROFILE, 4, 7);
		tracker.observe(PROFILE, 4, 8);
		assertEquals(GauntletOutcome.UNKNOWN, tracker.classify(PROFILE,
			BossRegistry.GAUNTLET, otherLoot(), START));
		assertEquals(GauntletOutcome.COMPLETED, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START.plusSeconds(1)));
	}

	@Test
	public void matchesCounterIncreaseToOneCompletedRewardGroup()
	{
		GauntletCompletionTracker tracker = new GauntletCompletionTracker();
		tracker.observe(PROFILE, 3, 7);
		tracker.observe(PROFILE, 4, 8); // Some clients may advance both counters for corrupted.
		assertEquals(GauntletOutcome.COMPLETED, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START));
		assertEquals(GauntletOutcome.COMPLETED, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START.plusSeconds(4)));
		assertEquals(GauntletOutcome.UNKNOWN, tracker.classify(PROFILE,
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START.plusSeconds(6)));
		assertEquals(GauntletOutcome.UNKNOWN, tracker.classify(PROFILE,
			BossRegistry.GAUNTLET, otherLoot(), START.plusSeconds(7)));
		tracker.observe(PROFILE, 5, 8);
		assertEquals(GauntletOutcome.COMPLETED, tracker.classify(PROFILE,
			BossRegistry.GAUNTLET, otherLoot(), START.plusSeconds(8)));
	}

	@Test
	public void doesNotUseAnotherProfilesCompletionCounter()
	{
		GauntletCompletionTracker tracker = new GauntletCompletionTracker();
		tracker.observe(PROFILE, 3, 7);
		tracker.observe(PROFILE, 3, 8);
		tracker.observe("profile-two", 10, 20);
		assertEquals(GauntletOutcome.UNKNOWN, tracker.classify("profile-two",
			BossRegistry.CORRUPTED_GAUNTLET, otherLoot(), START));
	}

	private static java.util.List<LootItemPreview> mapleShortbows()
	{
		return Collections.singletonList(new LootItemPreview(854, 10));
	}

	private static java.util.List<LootItemPreview> otherLoot()
	{
		return Collections.singletonList(new LootItemPreview(1621, 5));
	}
}
