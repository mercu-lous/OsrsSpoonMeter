package com.lootluck.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A reversible manual change to one observed reward group. */
public final class RewardEdit
{
	private final String groupId;
	private final boolean excluded;
	private final long gpCorrection;
	private final String note;
	private final Instant editedAtUtc;

	public RewardEdit(String groupId, boolean excluded, long gpCorrection, String note,
		Instant editedAtUtc)
	{
		UUID.fromString(groupId);
		this.groupId = groupId;
		this.excluded = excluded;
		this.gpCorrection = gpCorrection;
		this.note = Objects.requireNonNull(note).trim();
		if (this.note.length() > 160)
		{
			throw new IllegalArgumentException("Reason must be 160 characters or fewer");
		}
		this.editedAtUtc = Objects.requireNonNull(editedAtUtc);
	}

	public String getGroupId() { return groupId; }
	public boolean isExcluded() { return excluded; }
	public long getGpCorrection() { return gpCorrection; }
	public String getNote() { return note; }
	public Instant getEditedAtUtc() { return editedAtUtc; }
	public boolean isEmpty() { return !excluded && gpCorrection == 0 && note.isEmpty(); }
}
