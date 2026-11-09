# Loot Luck Tracker

An early RuneLite plugin for comparing boss loot with expected gross GP value. The product requirements and longer roadmap are in [loot-luck-tracker-codex-context.md](loot-luck-tracker-codex-context.md).

## Current prototype

- Adds a **Loot Luck Tracker** sidebar button and panel.
- Observes RuneLite Loot Tracker's `LootReceived` event. The user confirmed a `Maggot King` / `NPC` event with item IDs `33627` and `33628` in-game on November 8, 2026.
- Saves each new Maggot King event's item IDs and quantities, UTC timestamp, event ID, client tick, RuneLite event metadata fields, and session ID. The sidebar shows a per-profile saved-event count.
- Snapshots each item's name, RuneLite price, and available high-alchemy reference value when the event arrives. The panel shows a **direct GE sale estimate**. Explicitly untradeable items with no RuneLite price count as 0 direct GE GP, while their alchemy value appears separately when positive. Conversion and use values remain excluded. A missing price for a tradeable item remains unavailable.
- Records when the valuation was captured; RuneLite does not expose the underlying price-feed timestamp through this lookup, so that field remains unavailable.
- Shows a read-only preview when a Maggot King corpse's **Open-stomach** or **Take-eggs** menu option is clicked. A loot event within five client ticks on the same profile is linked to that click as an unconfirmed candidate. Multiple loot events may share a click ID. A click is not treated as a completed reward or kill.
- Stores separate files under RuneLite's `plugin-data/loot_luck_tracker/profiles/<hashed-profile-id>/events/` directory. The original RuneScape profile key and account name are not saved. A session currently starts when the plugin starts and ends when it stops.

The screenshot from the first prototype was a temporary preview; that earlier event was not saved. Event files are written on a dedicated worker using a temporary file and atomic move. Earlier records remain frozen; they are not silently repriced. New records use schema version 5, while versions 1–4 remain readable. The panel does not yet fetch boss expected values, calculate luck, or infer a kill count. Three **Open-stomach** examples had a two-tick gap from click to loot (540→542, 339→341, and 728→730). The last event was saved with the unconfirmed click link. Repeated-event behavior needs in-game observation. Identical callbacks do not yet have a RuneLite-provided stable ID, so callback-level deduplication remains unresolved; a stored event ID prevents writing the same record twice.

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

Verified locally: the official template was retrieved, its Gradle wrapper downloaded, and `clean test` compiled successfully with JDK 21. The development client started and logged that `LootLuckPlugin` was running with its `LootReceived` subscription registered. RuneLite also logged a Java 21 reflection warning during startup. The user confirmed the saved count survived a restart and a new Maggot King event displayed `37 × 509 GP = 18,833 GP` on November 8, 2026. On November 9, untradeable supply drops displayed `0 GP`, and a Tarnished spear was saved as event six with a tentative Open-stomach link. The spear's zero is only its direct GE sale value; the [Wiki says it can be polished](https://oldschool.runescape.wiki/w/Tarnished_spear) and lists a 12,000 GP high-alchemy value. The new alchemy reference display has not yet been tested in-game.

1. Restart the development client with this version. Confirm a subsequent untradeable drop shows a direct GE sale value and a separate high-alchemy reference where available. Existing saved events should remain unchanged.
2. Observe whether normal and Take-eggs rewards emit one or multiple loot events; Take-eggs can yield no loot, so we cannot assume every kill is represented by a loot event.
3. Choose and implement a conversion-aware GP valuation before comparing against the [Wiki's Maggot King GE average](https://oldschool.runescape.wiki/w/Maggot_King), which assumes Tarnished items are polished, blood sacs are converted, and Elder venator fangs are etched. Then add a clearly labeled expected-GP source and calculate GP delta only for confirmed reward attribution.
4. Add session and time-window statistics, then boss rankings, and expand boss coverage.

No live Wiki expected value is used yet. Values in the context file are illustrative or unverified.
