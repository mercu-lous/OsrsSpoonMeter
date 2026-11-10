# Loot Luck Tracker

An early RuneLite plugin for comparing boss loot with expected gross GP value. The product requirements and longer roadmap are in [loot-luck-tracker-codex-context.md](loot-luck-tracker-codex-context.md).

## Current prototype

- Adds a **Loot Luck Tracker** sidebar button and panel.
- Observes RuneLite Loot Tracker's `LootReceived` event. The user confirmed a `Maggot King` / `NPC` event with item IDs `33627` and `33628` in-game on November 8, 2026.
- Saves each new Maggot King event's item IDs and quantities, UTC timestamp, event ID, client tick, RuneLite event metadata fields, and session ID. The sidebar shows a per-profile saved-event count.
- Snapshots each item's name, RuneLite price, and available high-alchemy reference value when the event arrives. The panel shows both **direct GE sale** and **after-conversion GE** estimates. Explicitly untradeable items count as 0 direct GE GP. Conversion output prices, recipe weights, source links, and results are frozen in new event files. A missing output price or unsupported untradeable makes the after-conversion total unavailable rather than silently treating it as zero.
- Records when the valuation was captured; RuneLite does not expose the underlying price-feed timestamp through this lookup, so that field remains unavailable.
- Shows a read-only preview when a Maggot King corpse's **Open-stomach** or **Take-eggs** menu option is clicked. A loot event within five client ticks on the same profile is linked to that click as an unconfirmed candidate. Multiple loot events may share a click ID. A click is not treated as a completed reward or kill.
- Detects the [documented Take-eggs no-loot game message](https://oldschool.runescape.wiki/w/Maggot_King#Drops_(take-eggs)) only when it follows a Take-eggs click on the same profile within five client ticks. This creates a separate, persistent **confirmed no-loot** record with its click, session, timestamp, tick, and chat type. An unmatched message or click is not counted. Take-eggs loot callbacks remain timing-linked candidates; a no-loot message takes precedence if a separate clue or other tertiary callback shares that click.
- For timing-linked **Open-stomach** loot, calculates a captured expected GP value from a curated Wiki drop table and the same RuneLite price setting as the observed conversion estimate. The panel shows an expected value and a **provisional GP vs expected** summary for valued rewards. Loot callbacks sharing a click ID count as one observed reward. Older events without expected snapshots remain counted as observed rewards but do not enter the GP comparison.
- The active profile's summary can show **All time**, **Current plugin run**, **Today**, **This week**, **This month**, or **This year**. A run starts when this plugin starts and ends when it stops; restarting it creates a new run. Calendar windows use the configured IANA time zone, or the computer's time zone when the setting is blank or invalid. Weeks start on Monday. Reward groups use their final loot callback's timestamp, so callbacks that cross midnight stay together. The selector changes only the displayed aggregate and never rewrites saved events.
- The **History** tab shows the newest 30 saved observations in the selected scope, including item IDs, quantities, captured direct and after-conversion GP, expected GP for linked Open-stomach rewards, and any separate Take-eggs no-loot confirmation. History preserves raw callbacks as separate entries; the overview groups them by click for reward counts. Calendar filtering keeps callbacks from one click together even when they cross midnight.
- Stores loot events under RuneLite's `plugin-data/loot_luck_tracker/profiles/<hashed-profile-id>/events/` directory and no-loot confirmations under the same profile's `no-loot-confirmations/` directory. The original RuneScape profile key and account name are not saved. A session currently starts when the plugin starts and ends when it stops.

The screenshot from the first prototype was a temporary preview; that earlier event was not saved. Event and confirmation files are written on a dedicated worker using a temporary file and atomic move. Earlier records remain frozen; they are not silently repriced or given a new expected value. Loot events use schema version 7, while versions 1–6 remain readable; no-loot confirmations use their own schema version 1. Five **Open-stomach** examples linked click to loot after two or three ticks (540→542, 339→341, 728→730, 586→589, 315→317). Stored raw events remain separate; the summary groups by click ID and ignores identical itemized callbacks in the same client tick. RuneLite supplies no stable callback ID, so this duplicate check is provisional. The panel does not infer total kill count. Take-eggs can produce no loot, so a click alone does not increment reward count.

### Conversion catalog v1

- The nine [tarnished items](https://oldschool.runescape.wiki/w/Maggot_King#Tarnished_items) use the Wiki's listed polish outcomes and weights. Each outcome's price is read from RuneLite at loot capture time; the weighted stack estimate is truncated to whole GP. Polishing requires 64 Crafting.
- [Elder venator fang](https://oldschool.runescape.wiki/w/Elder_venator_fang) is valued as one etched fang per fang, assuming 84 Crafting. [Blood sacs](https://oldschool.runescape.wiki/w/Blood_sac) are valued as one tradeable vial of blood per 50 sacs, assuming an empty vial is available. The empty vial's cost is excluded.
- Dull ancient medals and stymphike tartare have no GE conversion in this catalog and contribute zero. High-alchemy references remain separate. Other untradeables are reported as unsupported until their GP treatment is verified.
- These are gross output values. GE fees, materials, and other costs are not deducted. Polished outcomes are estimates, not guaranteed results for an individual item.

### Expected Open-stomach GP v1

- The local [Maggot King drop table](https://oldschool.runescape.wiki/w/Maggot_King) has a 159-weight main roll and unique pre-roll rates of 1/340 for the fang and 1/520 for the kisten. The main roll is weighted by the probability that neither unique is rolled. This precedence is inferred from the [Wiki money-making guide's expected item quantities](https://oldschool.runescape.wiki/w/Money_making_guide/Killing_the_Maggot_King), so the catalog is versioned for correction if the game or source changes.
- Quantity ranges use their midpoint. Each item is valued by the same conversion catalog as actual loot, using RuneLite item prices captured at the event. The saved expected snapshot includes the table and conversion versions, source link, price setting, timestamp, and all price inputs. Missing input prices leave expected GP unavailable.
- Pet, clue, and other tertiary utility rewards are excluded from the GP baseline. The current comparison is **per observed, timing-linked Open-stomach reward**, not total boss KC. Take-eggs rewards have no normal-loot baseline here; confirmed no-loot choices and timing-linked Take-eggs loot are reported separately and excluded from GP luck.

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

Verified locally: the official template was retrieved, its Gradle wrapper downloaded, and `clean test` compiled successfully with JDK 21. The development client started and logged that `LootLuckPlugin` was running with its `LootReceived` subscription registered. RuneLite also logged a Java 21 reflection warning during startup. The user confirmed the saved count survived a restart and a new Maggot King event displayed `37 × 509 GP = 18,833 GP` on November 8, 2026. On November 9, untradeable supply drops displayed `0 GP`; a Tarnished spear was saved as event six with a tentative Open-stomach link, and event seven showed a separate 30 GP high-alchemy reference for a dull ancient medal. The conversion catalog passes fixture tests and was checked in-game with event eight: one Tarnished halberd showed 0 GP direct GE sale, 36,000 GP high alchemy, and 35,326 GP after conversion. The saved output prices and 5:4:1 weights reproduce that estimate. Event nine checked the expected-value feature in-game: one Tarnished necklace showed 1,245 GP after conversion, 437,794 GP expected, and −436,549 GP provisional difference. Its saved event has schema 7, the captured input prices, and Open-stomach click tick 852 linked to loot tick 854. The saved files contain nine events, four distinct Open-stomach click IDs, and one valued reward, matching the panel.

The All time / Current plugin run selector was checked in-game after a development-client restart. All time showed four timing-linked Open-stomach rewards, one valued reward, and −436,549 GP provisional difference. Current plugin run showed zero rewards. Both views retained the profile's nine saved events.

The Today, This week, This month, and This year options appeared in the development client, and the user confirmed that all calendar views worked as expected. Date-boundary behavior remains covered by deterministic tests, including a reward whose callbacks cross local midnight.

The Take-eggs no-loot signal passes fixture tests but has not been checked in-game. A message outside the five-tick window is displayed as uncounted for diagnosis. The user is continuing with normal Open-stomach rewards, so no Take-eggs trial is required for the next increment.

The History tab was checked in-game under All time and showed saved Tarnished necklace, Tarnished halberd, and other earlier itemized events. The same screenshots revealed a large empty gap in Overview between the latest-event text and the reward summary. Overview now renders those sections in one scrollable text area; this layout fix passes the local build but has not yet been checked in-game.

1. Restart the development client and check that the Overview reward summary appears directly below the latest-event text. No new kill is required.
2. Add percentage luck and GP per observed reward, then boss rankings as more bosses are supported.

The plugin does not scrape the Wiki on every kill or use the Wiki's displayed live average directly. It recalculates from the versioned table and current RuneLite prices. Values in the context file are illustrative or unverified.
