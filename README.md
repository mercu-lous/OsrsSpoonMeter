# Loot Luck Tracker

An early RuneLite plugin for comparing boss loot with expected gross GP value. The product requirements and longer roadmap are in [loot-luck-tracker-codex-context.md](loot-luck-tracker-codex-context.md).

## Current prototype

- Adds a **Loot Luck Tracker** sidebar button and panel.
- Observes RuneLite Loot Tracker's `LootReceived` event. The user confirmed a `Maggot King` / `NPC` event with item IDs `33627` and `33628` in-game on November 8, 2026.
- Saves each new Maggot King event's item IDs and quantities, UTC timestamp, event ID, client tick, RuneLite event metadata fields, and session ID. The sidebar shows a per-profile saved-event count.
- Stores separate files under RuneLite's `plugin-data/loot_luck_tracker/profiles/<hashed-profile-id>/events/` directory. The original RuneScape profile key and account name are not saved. A session currently starts when the plugin starts and ends when it stops.

The screenshot from the first prototype was a temporary preview; that earlier event was not saved. The new storage code only captures events received after this update is running. Event files are written on a dedicated worker using a temporary file and atomic move. The panel does not yet price items, fetch boss expected values, calculate luck, or infer a kill count. Reward-method attribution and repeated-event behavior still need in-game observation. Identical callbacks do not yet have a RuneLite-provided stable ID, so callback-level deduplication remains unresolved; a stored event ID prevents writing the same record twice.

## Run locally

The project follows the [official RuneLite example plugin](https://github.com/runelite/example-plugin) layout. Its Gradle wrapper is 8.10 and Java compilation targets release 11. Use a JDK 11 or newer; this machine has JDK 17 and 21, while its default `java` command currently points to Java 8.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean test
.\gradlew.bat run
```

In IntelliJ, open this folder as a Gradle project, set the Gradle JVM to JDK 17 or 21, and run `LootLuckPluginTest` in `src/test/java/com/lootluck`. The `run` task opens RuneLite in developer mode. Enable RuneLite's built-in Loot Tracker if it is disabled, since it publishes the event this prototype observes. For a Jagex Account, follow RuneLite's [development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Verification and next increments

Verified locally: the official template was retrieved, its Gradle wrapper downloaded, and `clean test` compiled successfully with JDK 21. The development client started and logged that `LootLuckPlugin` was running with its `LootReceived` subscription registered. RuneLite also logged a Java 21 reflection warning during startup. The user confirmed the sidebar and a Maggot King event in-game on November 8, 2026. The new persistence code passes its JSON format and profile-key tests, but still needs an in-game restart test.

1. Restart the development client with this version, collect a Maggot King reward, and confirm the panel says **Event saved** and the count increases. Restart once more and confirm the count persists.
2. Observe whether normal and Take-eggs rewards emit one or multiple events. Confirm whether repeated callbacks occur before treating an event as a kill.
3. Snapshot item prices and a clearly labeled expected-GP input; calculate GP delta only for confirmed attribution.
4. Add session and time-window statistics, then boss rankings, and expand boss coverage.

No live Wiki expected value is used yet. Values in the context file are illustrative or unverified.
