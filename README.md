# Loot Luck Tracker

An early RuneLite plugin for comparing boss loot with expected gross GP value. The product requirements and longer roadmap are in [loot-luck-tracker-codex-context.md](loot-luck-tracker-codex-context.md).

## Current prototype

- Adds a **Loot Luck Tracker** sidebar button and panel.
- Observes RuneLite Loot Tracker's `LootReceived` event and shows the most recent event's source, type, local display time, item IDs, and quantities.
- Keeps the observer read-only. The preview is lost when RuneLite closes. It does **not** claim that an event equals a kill.

The panel does not yet price items, fetch boss expected values, calculate luck, or save history. The exact Maggot King event name and reward methods still need an in-game observation before adding attribution rules.

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

Verified locally: the official template was retrieved, its Gradle wrapper downloaded, and `clean test` compiled successfully with JDK 21. The development client started and logged that `LootLuckPlugin` was running with its `LootReceived` subscription registered. RuneLite also logged a Java 21 reflection warning during startup. The sidebar appearance and actual loot-event delivery still require a user check in RuneLite; a successful build or JVM start does not prove those behaviors.

1. Launch RuneLite and confirm the sidebar opens. Collect a Maggot King reward and note the event source/type and whether each reward method emits one or multiple events.
2. Add an itemized, account-separated event store with unique IDs and atomic writes. Keep events distinct from attributed kills or rewards.
3. Snapshot item prices and a clearly labeled expected-GP input; calculate GP delta only for confirmed attribution.
4. Add session and time-window statistics, then boss rankings, and expand boss coverage.

No live Wiki expected value is used yet. Values in the context file are illustrative or unverified.
