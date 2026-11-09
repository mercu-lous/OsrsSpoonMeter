package com.lootluck.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A local pseudonymous key; the RuneScape profile key never reaches disk. */
public final class ProfileIds
{
	private ProfileIds()
	{
	}

	public static String fromRuneScapeProfileKey(String key)
	{
		if (key == null || key.isEmpty())
		{
			throw new IllegalArgumentException("RuneScape profile key is unavailable");
		}
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte value : digest)
			{
				hex.append(Character.forDigit((value >>> 4) & 0xf, 16));
				hex.append(Character.forDigit(value & 0xf, 16));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException exception)
		{
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
