package com.lootluck.storage;

import com.lootluck.model.NoLootConfirmation;
import com.lootluck.model.SavedLootEvent;
import com.lootluck.model.RewardSummary;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.client.util.Filepath;

/** Atomically published observations; all calls run on the storage executor. */
public final class LootEventStore
{
	private static final int MAX_OBSERVATION_CHARS = 1_000_000;
	private final Filepath pluginDirectory;
	private final LootEventCodec codec;
	private final NoLootConfirmationCodec confirmationCodec;

	public LootEventStore(Filepath pluginDirectory, LootEventCodec codec,
		NoLootConfirmationCodec confirmationCodec)
	{
		this.pluginDirectory = pluginDirectory;
		this.codec = codec;
		this.confirmationCodec = confirmationCodec;
	}

	public boolean save(SavedLootEvent event) throws IOException
	{
		return saveJson(eventDirectory(event.getProfileId()), event.getEventId() + ".json",
			codec.encode(event));
	}

	public boolean saveConfirmation(NoLootConfirmation confirmation) throws IOException
	{
		return saveJson(confirmationDirectory(confirmation.getProfileId()),
			confirmation.getClick().getClickId() + ".json", confirmationCodec.encode(confirmation));
	}

	private static boolean saveJson(Filepath directory, String name, String json) throws IOException
	{
		directory.createDirectories();
		Filepath target = directory.joinSegment(name);
		if (target.exists())
		{
			return false;
		}
		Filepath temporary = directory.createTempFile("event-", ".tmp");
		try
		{
			temporary.write(json);
			try (FileChannel channel = temporary.openFileChannel(StandardOpenOption.WRITE))
			{
				channel.force(true);
			}
			temporary.moveTo(target, StandardCopyOption.ATOMIC_MOVE);
			return true;
		}
		finally
		{
			temporary.deleteIfExists();
		}
	}

	public long count(String profileId) throws IOException
	{
		Filepath directory = eventDirectory(profileId);
		if (!directory.exists())
		{
			return 0;
		}
		try (Stream<Filepath> files = directory.walk(1))
		{
			return files.filter(Filepath::isFile)
				.filter(file -> file.getFileName().endsWith(".json"))
				.count();
		}
	}

	public RewardSummary statistics(String profileId, String sessionId, ZoneId calendarZone) throws IOException
	{
		List<SavedLootEvent> events = new ArrayList<>();
		for (Filepath file : jsonFiles(eventDirectory(profileId)))
		{
			try
			{
				events.add(codec.decode(readJson(file)));
			}
			catch (RuntimeException malformed)
			{
				throw new IOException("Could not read saved loot event", malformed);
			}
		}
		List<NoLootConfirmation> confirmations = new ArrayList<>();
		for (Filepath file : jsonFiles(confirmationDirectory(profileId)))
		{
			try
			{
				NoLootConfirmation confirmation = confirmationCodec.decode(readJson(file));
				if (!profileId.equals(confirmation.getProfileId()))
				{
					throw new IllegalArgumentException("Confirmation profile does not match directory");
				}
				confirmations.add(confirmation);
			}
			catch (RuntimeException malformed)
			{
				throw new IOException("Could not read saved no-loot confirmation", malformed);
			}
		}
		return RewardSummary.fromObservations(events, confirmations, sessionId, Instant.now(), calendarZone);
	}

	private static List<Filepath> jsonFiles(Filepath directory) throws IOException
	{
		if (!directory.exists())
		{
			return java.util.Collections.emptyList();
		}
		try (Stream<Filepath> files = directory.walk(1))
		{
			return files.filter(Filepath::isFile)
				.filter(file -> file.getFileName().endsWith(".json"))
				.collect(Collectors.toList());
		}
	}

	private static String readJson(Filepath file) throws IOException
	{
		try (BufferedReader reader = file.openBufferedReader())
		{
			StringBuilder json = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer)) != -1)
			{
				json.append(buffer, 0, read);
				if (json.length() > MAX_OBSERVATION_CHARS)
				{
					throw new IOException("Saved observation exceeds size limit");
				}
			}
			return json.toString();
		}
	}

	private Filepath eventDirectory(String profileId)
	{
		return profileDirectory(profileId).joinSegment("events");
	}

	private Filepath confirmationDirectory(String profileId)
	{
		return profileDirectory(profileId).joinSegment("no-loot-confirmations");
	}

	private Filepath profileDirectory(String profileId)
	{
		if (!profileId.matches("[0-9a-f]{64}"))
		{
			throw new IllegalArgumentException("Invalid profile ID");
		}
		return pluginDirectory.joinSegment("profiles").joinSegment(profileId);
	}
}
