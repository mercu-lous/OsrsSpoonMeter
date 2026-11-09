# Loot Luck Tracker

An early RuneLite plugin for comparing boss loot with expected gross GP value. The product requirements and longer roadmap are in [loot-luck-tracker-codex-context.md](loot-luck-tracker-codex-context.md).

## Current prototype

- Adds a **Loot Luck Tracker** sidebar button and panel.
- Observes RuneLite Loot Tracker's `LootReceived` event. The user confirmed a `Maggot King` / `NPC` event with item IDs `33627` and `33628` in-game on November 8, 2026.
- Saves each new Maggot King event's item IDs and quantities, UTC timestamp, event ID, client tick, RuneLite event metadata fields, and session ID. The sidebar shows a per-profile saved-event count.
- Snapshots each item's name, RuneLite price, and available high-alchemy reference value when the event arrives. The panel shows both **direct GE sale** and **after-conversion GE** estimates. Explicitly untradeable items count as 0 direct GE GP. Conversion output prices, recipe weights, source links, and results are frozen in new event files. A missing output price or unsupported untradeable makes the after-conversion total unavailable rather than silently treating it as zero.
- Records when the valuation was captured; RuneLite does not expose the underlying price-feed timestamp through this lookup, so that field remains unavailable.
- Shows a read-only preview when a Maggot King corpse's **Open-stomach** or **Take-eggs** menu option is clicked. A loot event within five client ticks on the same profile is linked to that click as an unconfirmed candidate. Multiple loot events may share a click ID. A click is not treated as a completed reward or kill.
- Stores separate files under RuneLite's `plugin-data/loot_luck_tracker/profiles/<hashed-profile-id>/events/` directory. The original RuneScape profile key and account name are not saved. A session currently starts when the plugin starts and ends when it stops.

The screenshot from the first prototype was a temporary preview; that earlier event was not saved. Event files are written on a dedicated worker using a temporary file and atomic move. Earlier records remain frozen; they are not silently repriced or given a new conversion estimate. New records use schema version 6, while versions 1–5 remain readable. The panel does not yet fetch boss expected values, calculate luck, or infer a kill count. Five **Open-stomach** examples linked click to loot after two or three ticks (540→542, 339→341, 728→730, 586→589, 315→317). Repeated-event behavior needs in-game observation. Identical callbacks do not yet have a RuneLite-provided stable ID, so callback-level deduplication remains unresolved; a stored event ID prevents writing the same record twice.

### Conversion catalog v1

- The nine [tarnished items](https://oldschool.runescape.wiki/w/Maggot_King#Tarnished_items) use the Wiki's listed polish outcomes and weights. Each outcome's price is read from RuneLite at loot capture time; the weighted stack estimate is truncated to whole GP. Polishing requires 64 Crafting.
- [Elder venator fang](https://oldschool.runescape.wiki/w/Elder_venator_fang) is valued as one etched fang per fang, assuming 84 Crafting. [Blood sacs](https://oldschool.runescape.wiki/w/Blood_sac) are valued as one tradeable vial of blood per 50 sacs, assuming an empty vial is available. The empty vial's cost is excluded.
- Dull ancient medals and stymphike tartare have no GE conversion in this catalog and contribute zero. High-alchemy references remain separate. Other untradeables are reported as unsupported until their GP treatment is verified.
- These are gross output values. GE fees, materials, and other costs are not deducted. Polished outcomes are estimates, not guaranteed results for an individual item.

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

Verified locally: the official template was retrieved, its Gradle wrapper downloaded, and `clean test` compiled successfully with JDK 21. The development client started and logged that `LootLuckPlugin` was running with its `LootReceived` subscription registered. RuneLite also logged a Java 21 reflection warning during startup. The user confirmed the saved count survived a restart and a new Maggot King event displayed `37 × 509 GP = 18,833 GP` on November 8, 2026. On November 9, untradeable supply drops displayed `0 GP`; a Tarnished spear was saved as event six with a tentative Open-stomach link, and event seven showed a separate 30 GP high-alchemy reference for a dull ancient medal. The conversion catalog passes fixture tests and was checked in-game with event eight: one Tarnished halberd showed 0 GP direct GE sale, 36,000 GP high alchemy, and 35,326 GP after conversion. The saved output prices and 5:4:1 weights reproduce that estimate.

1. Observe whether normal and Take-eggs rewards emit one or multiple loot events; Take-eggs can yield no loot, so we cannot assume every kill is represented by a loot event.
2. Add a clearly labeled expected-GP source matched to this conversion policy before calculating GP delta. The [Wiki's Maggot King GE average](https://oldschool.runescape.wiki/w/Maggot_King) assumes these conversions, but its displayed value changes with prices and is not yet snapshotted by the plugin. Apply deltas only to confirmed reward attribution.
3. Add session and time-window statistics, then boss rankings, and expand boss coverage.

No live Wiki expected value is used yet. Values in the context file are illustrative or unverified.
