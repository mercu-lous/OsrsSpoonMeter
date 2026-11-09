package com.lootluck.storage;

import com.lootluck.model.SavedLootEvent;
import com.lootluck.model.RewardStatistics;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.client.util.Filepath;

/** One atomically published file per event; all calls run on the storage executor. */
public final class LootEventStore
{
	private static final int MAX_EVENT_CHARS = 1_000_000;
	private final Filepath pluginDirectory;
	private final LootEventCodec codec;

	public LootEventStore(Filepath pluginDirectory, LootEventCodec codec)
	{
		this.pluginDirectory = pluginDirectory;
		this.codec = codec;
	}

	public boolean save(SavedLootEvent event) throws IOException
	{
		Filepath directory = eventDirectory(event.getProfileId());
		directory.createDirectories();
		Filepath target = directory.joinSegment(event.getEventId() + ".json");
		if (target.exists())
		{
			return false;
		}
		Filepath temporary = directory.createTempFile("event-", ".tmp");
		try
		{
			temporary.write(codec.encode(event));
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

	public RewardStatistics statistics(String profileId) throws IOException
	{
		Filepath directory = eventDirectory(profileId);
		if (!directory.exists())
		{
			return RewardStatistics.summarize(java.util.Collections.emptyList());
		}
		List<Filepath> eventFiles;
		try (Stream<Filepath> files = directory.walk(1))
		{
			eventFiles = files.filter(Filepath::isFile)
				.filter(file -> file.getFileName().endsWith(".json"))
				.collect(Collectors.toList());
		}
		List<SavedLootEvent> events = new ArrayList<>();
		for (Filepath file : eventFiles)
		{
			try (BufferedReader reader = file.openBufferedReader())
			{
				StringBuilder json = new StringBuilder();
				char[] buffer = new char[8192];
				int read;
				while ((read = reader.read(buffer)) != -1)
				{
					json.append(buffer, 0, read);
					if (json.length() > MAX_EVENT_CHARS)
					{
						throw new IOException("Saved loot event exceeds size limit");
					}
				}
				events.add(codec.decode(json.toString()));
			}
			catch (RuntimeException malformed)
			{
				throw new IOException("Could not read saved loot event", malformed);
			}
		}
		return RewardStatistics.summarize(events);
	}

	private Filepath eventDirectory(String profileId)
	{
		if (!profileId.matches("[0-9a-f]{64}"))
		{
			throw new IllegalArgumentException("Invalid profile ID");
		}
		return pluginDirectory.joinSegment("profiles").joinSegment(profileId).joinSegment("events");
	}
}
