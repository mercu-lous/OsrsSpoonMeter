# Loot Luck Tracker — Codex Project Context

> **Purpose:** Paste this document into a Codex workspace or keep it as `PROJECT_CONTEXT.md` in the repository. It captures the product requirements, technical design intentions, implementation order, and caveats discussed with ChatGPT. It is a planning brief, **not** an assertion that code or dependencies have already been implemented.

## Current direction and priority order — November 11, 2026

**Product direction:** Make Loot Luck Tracker an analytics tool first. Live capture continues to supply data, but the default experience should emphasize boss totals, expected GP, actual GP, and the resulting GP difference. Store itemized captures for later analysis; show item details only when the user expands them or enables a setting. This priority order supersedes the original milestone order in section 5. The original requirements remain a longer-term reference.

**Current starting point:** A local development plugin saves itemized loot, supports Maggot King, Vorkath, Zulrah, Corrupted Gauntlet, and regular Gauntlet, and shows time scopes, GP comparisons, Rankings, reversible edits, manual rewards, and a new read-only Loot Tracker aggregate import. The import path has local parser, persistence, and arithmetic tests but still needs a development-client check. Captured reward groups are provisional observations, while imported eligible kills and manual entries add explicit counts. There is no unified confirmed count for later captured rewards, item-filter analysis, or published release yet.

### Priority 1 — Import existing Loot Tracker totals and manual customization

1. Prefer a read-only **Import from RuneLite Loot Tracker** flow for the current RuneScape profile. RuneLite's public `ConfigManager` methods expose profile-scoped Loot Tracker configuration. Its current per-source records contain a tracked kill count, first/last timestamps, and cumulative item-ID/quantity pairs. A local inspection found records for Maggot King, Vorkath, Zulrah, and Corrupted Hunllef. Parse and validate only the current profile's records; do not modify Loot Tracker's data or call private account endpoints. The Loot Tracker record format is internal and may change, so treat this as a guarded import adapter rather than a permanent shared API.
2. Preview each matching boss's tracked count, item totals, selected price convention, calculated GP, expected GP, and resulting luck before importing. Resolve item prices at import time and snapshot them. If RuneLite's displayed total differs because of price type, ignored items, or untradeable treatment, show the difference and allow the user to enter the displayed total as an override. Preserve item aggregates for later item-level analysis, while marking any unallocated GP difference.
3. Save a versioned **baseline anchor** containing boss, profile, cumulative kills, cumulative actual GP, expected GP per kill, effective time, source, and optional imported item aggregates with price snapshots. An anchor represents totals through that time, not invented individual kills. The local Loot Tracker format aggregates quantities per source; it does not preserve the historical order or value of each kill. Continue storing new itemized capture events separately.
4. Prevent overlap: imported Loot Tracker totals may already include rewards this plugin captured. Preview which captured and manual records fall before the anchor and would be excluded from the new aggregate. Add only subsequent eligible rewards and manual adjustments to the anchor. Preserve old records for History; never silently delete or double count them.
5. Keep a prominent **Set starting totals manually** fallback for unavailable or incomplete Loot Tracker data. The user enters the cumulative kill count and gross GP visible in Loot Tracker or another source, with a preview before saving. Let the user replace/correct a baseline and review what changed. Keep earlier values recoverable, make exclusions reversible, and distinguish **Loot Tracker recorded kills**, **manual kills added**, and **provisional kills from new loot**. Decide the boss-specific rule for when a loot group can increase provisional kills; a loot callback alone is not universally a confirmed kill.
6. Keep the default History view compact: date, boss, actual GP, expected GP, and difference. Offer expandable item stacks and captured pricing details for analysis or diagnosis.

**Done when:** The user can import available Loot Tracker boss and item totals with a preview, or enter missing totals manually; see an accurate new starting point after restart; and see later rewards added exactly once. The UI clearly labels imported, manual, and provisional counts.

**Implementation evidence and limit:** The current [Loot Tracker source](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/LootTrackerPlugin.java) reads `loottracker` profile keys through [public `ConfigManager` methods](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/config/ConfigManager.java). Its [local record format](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/ConfigLoot.java) stores `kills`, `first`, `last`, and aggregate `drops` item-ID/quantity pairs. Verify access in the development client and review Plugin Hub compatibility before relying on it for release. Screenshots remain a manual fallback, not the primary import path.

**Current implementation status:** The Import tab reads the current profile's selected boss totals, shows a price and GP preview, allows eligible-kill and actual-GP adjustments, and saves a dated baseline with item aggregates. All-time arithmetic uses the latest baseline and excludes earlier captured/manual entries from that aggregate while keeping them in History. The raw Loot Tracker count is retained alongside eligible kills. Parser, codec, cutoff, and restart tests pass. Next, confirm a real import and restart in the development client, then implement the prominent manual starting-totals fallback, baseline replacement controls, and compact History. Built-in aggregate item quantities cannot separate failed Gauntlet chests or reconstruct individual historical kills.

### Priority 2 — Readable, useful sidebar

1. Investigate RuneLite's supported sidebar sizing behavior and aim for roughly **50% more width**. If the host controls the width, use a layout that remains readable at the available size rather than relying on an unsupported client change.
2. Put the selected boss and time range above a compact summary: total kills and their confidence, actual GP, expected GP, GP difference, and GP percentage. Make the starting-totals action easy to find.
3. Use restrained color and visual hierarchy: green for positive GP difference, red for negative, and neutral colors for context. Keep signs and text labels so color is not the only signal. Show the expected-value source and whether a comparison is provisional.
4. Keep Overview, Rankings, History, and editing easy to reach. Collapse detailed item lists by default. Check scrolling, text wrapping, and form sizes in a narrow and expanded sidebar.

**Done when:** A player can read their overall actual-versus-expected result and change boss or time range without hunting through itemized event text.

### Priority 3 — Broader boss catalog and user-defined bosses

1. Prioritize **Yama, Nex, Doom, Nightmare, Duke, Vard, Muspah, Leviathan, and Whisperer**. Verify each official boss name, RuneLite `LootReceived` source name or names, variants, and a dated expected **gross GP per eligible kill** before adding a comparison. Keep values, citations, and versions in the local catalog; old saved expectations remain frozen. A capture-only state is acceptable while an expectation is unverified.
2. Reuse the generic capture, grouping, valuation, baseline, and analytics path. A simple boss normally needs a definition, source aliases, and expected GP per kill. Some bosses may also need a rule for modes, player share, multiple rewards, no-loot outcomes, or completion evidence; record only the exceptions that matter for accurate totals.
3. Add a **Custom boss** form for display name, expected GP per kill, and one or more loot-source aliases. Where practical, offer recently observed unknown loot-source names to select rather than guessing the source from a display name. Let the user edit or override a built-in boss's expected GP for future observations, with a clear custom label and versioned snapshot.
4. Check a normal reward for each new boss during ordinary play when available; do not require rare drops or delay the catalog on boss-specific edge cases. Use fixtures for source mapping, persistence, and baseline arithmetic.

**Done when:** The requested bosses can appear in totals and GP Rankings when their sources and expectations are known, and a user can configure another boss without editing Java code.

### Priority 4 — Analysis and data manipulation

1. Define the session clock, then show **GP per elapsed session hour** with the time basis labeled. Do not imply combat-only GP/hour from plugin uptime. Add a manual or automatic session boundary only after its behavior is clear.
2. Add charts of cumulative actual GP, expected GP, and GP difference over time. Let the user choose boss and time range; show baseline anchors as separate starting points rather than fabricated historical kills. Imported aggregates have first/last timestamps but no per-kill timeline.
3. Add item-ID-based value filters: include or exclude selected items from an analysis view, and calculate how much a chosen boss yielded from a selected item, such as Vorkath's superior dragon bones. Preserve raw captures and provide a reset to the unfiltered result. Imported item aggregates can support historical item totals; GP-only manual anchors cannot be retroactively item-filtered.
4. Expand Rankings with luckiest/unluckiest ordering by GP difference and GP percentage, a minimum sample size, and per-kill metrics only where the kill denominator is explicit. Distinguish **GP above expectation** from statistical rare-drop luck, which would require drop probabilities and its own model.
5. Add export/backup of anchors, manual entries, and captured records before advanced imports or migrations.

**Done when:** The user can explore when GP was earned, which items contributed, and which bosses are above or below expectation without changing saved historical data.

### Priority 5 — Share and publish

1. Prepare a friend-test build with setup instructions, an example data backup, and a clear description of what the statistics do and do not mean.
2. Before a public release, check current RuneLite Plugin Hub requirements, supported APIs, dependency/network policy, metadata, privacy, and data migration behavior. Verify ordinary boss capture, profile separation, restarts, and recovery from malformed or older files.
3. Submit/publish only after the build and release materials are reviewable and the user approves that concrete release. Preserve a local development path for future updates.

**Done when:** Friends can install and use the plugin with their own saved data, and a public release has passed the relevant review process.

## 1. Project summary

**Working name:** Loot Luck Tracker (alternative: Spoon Meter)  
**Target:** Old School RuneScape, as a **RuneLite plugin** written in Java  
**Goal:** Track how far above or below the expected GP value the player is for each boss, each kill, and across selectable time ranges. Save **every individual loot event**, including item details and valuations, so the user can analyze their luck retrospectively, not just see total earnings.

**Example:** If a boss's expected loot value is 100,000 GP per kill, receiving 10,000 GP yields a **−90,000 GP** result in red; receiving 200,000 GP yields **+100,000 GP** in green. The plugin updates and persists the cumulative luck balance. The expected value of 100,000 GP is illustrative, not an actual Maggot King value.

Start with **Maggot King**, then add Vorkath and Zulrah, followed by more bosses. Aim eventually for RuneLite Plugin Hub compatibility. Initially develop/test as a local plugin.

## 2. Core product requirements

### 2.1 Event-level capture and persistence (mandatory)

- Listen for supported RuneLite loot events (investigate `LootReceived` and other appropriate API hooks in the current RuneLite version).
- Store each **individual loot event**, not only summary totals.
- For each event, capture at minimum:
  - Unique internal event identifier and attribution/deduplication metadata.
  - Account/profile identifier (avoid exposing sensitive identifiers).
  - Loot source/boss identity and, where relevant, reward/loot method.
  - Timestamp stored in UTC.
  - Session identifier.
  - Item IDs, item quantities, GP unit valuation at capture time, and item classifications, when available.
  - Total actual GP, expected GP used at capture, and difference (`actual - expected`).
  - Version/source of expected-value data and price data; clearly mark unavailable or estimated valuations.
- Records should survive RuneLite restarts and account switches. Keep different accounts' data separate.
- **Important:** A loot event is not always identical to one boss kill. Design separate event and kill/reward attribution concepts for bosses with multi-event, delayed, or alternative rewards. Avoid double-counting; do not assume every event increments KC by one.
- Use safe, atomic persistence and support future schema migrations. Decide between SQLite and a simpler local store after evaluating RuneLite plugin constraints and packaging rules.

### 2.2 GP luck calculations (mandatory)

Per kill/reward, when properly attributed:

```text
luck_delta_gp = actual_loot_gp - expected_loot_gp
```

For a selected set of kills:

```text
actual_total_gp   = sum(actual_loot_gp)
expected_total_gp = sum(expected_loot_gp)
net_luck_gp       = actual_total_gp - expected_total_gp
luck_percent     = (actual_total_gp / expected_total_gp - 1) * 100
luck_gp_per_kill  = net_luck_gp / counted_kills
```

Handle zero expected totals and zero kills explicitly; do not divide by zero. Do not confuse **profit** (which requires costs) with **gross loot GP luck**. Initial scope is gross loot value, not net profit after supplies, deaths, fees, etc.

**Historical accuracy:** Freeze each event's expected value and price valuation at collection time. Optionally add a separate user-invoked **revalue using current prices** view, without silently altering historical values.

### 2.3 Time windows and sessions (mandatory)

The user wants stats for:

- Current session and previous sessions.
- Today, this week, this month, this year.
- All time and arbitrary custom date ranges.
- Potentially last N kills and rolling 24-hour periods.

Record sessions separately from timestamps. Decide and document session boundaries (logout/reconnect, RuneLite restarts, manual new-session button). Internally store UTC; show day/week/month/year windows based on user-configurable local timezone and documented week-start convention.

### 2.4 Boss ranking and sorting (mandatory)

Provide a boss leaderboard with filtering by time period and sorting in both directions (luckiest/unluckiest), including:

1. **Net GP luck:** total actual minus total expected.
2. **Percentage luck:** relative amount above/below expectation.
3. **GP luck per kill:** average GP above/below expectation per kill.
4. **Unique-drop luck:** actual GP value from selected rare/unique drops versus expected rare-drop contribution.
5. **Common-loot luck:** same comparison for non-unique rewards.
6. Optional: longest dry streak for a specified unique/drop, total unique count, GP earned, KC.

Add a configurable minimum kill count to prevent one-off drops from dominating percentage rankings. Display kill count alongside ranked values. Use clear names: GP above expectation is not necessarily evidence of statistical rarity.

### 2.5 Rare/unique drop classification (important)

- Support **full expected GP**, **expected common GP**, and **expected rare/unique GP** views.
- Prefer automatic boss-specific classification from validated Wiki drop-table metadata when feasible, with user manual classification/override as a fallback.
- Preserve enough item data and drop-table definitions to revise classifications later.
- Do **not** calculate a unique-luck figure by subtracting all rare items from observed loot while leaving the full expected GP denominator unchanged. Actual and expected categories must match.
- Differentiate monetary luck from statistically unusual drops: expensive low-rate uniques can make most players fall below average until one drops.
- Account for loot modes, such as Maggot King's alternate **Take-eggs** vs normal reward; expectations may differ by method. Verify precise mechanics and mappings before implementation.

### 2.6 User interface (mandatory after engine works)

A RuneLite sidebar with four areas:

- **Overview:** current boss/selection, kills, expected GP/kill, actual total, expected total, net luck, last kill delta, active session summary.
- **Boss Rankings:** luckiest/unluckiest sorting by chosen metric, time range, minimum KC, boss filtering.
- **Boss Details:** event history, per-kill deltas, cumulative luck graph over kills or time, drops and unique-specific statistics, best/worst kill, streaks.
- **Settings & Data:** notifications, pricing/EV source, loot categories, session handling, imports/exports/backups.

Color convention: positive GP difference green; negative red, with text signs and labels for accessibility. Optional in-game chat notifications for new loot (`−90,000 GP this kill`, `+1.2M GP overall`) and possible future overlays. Keep RuneLite UI work isolated from statistics logic.

### 2.7 Import pre-existing Loot Tracker history (later phase)

The user wants the plugin to reconstruct a historical baseline **even if installed after hundreds of boss kills**. Investigate whether RuneLite's native Loot Tracker historical data is accessible from third-party plugins via supported stable APIs. **Do not assume it is.** Avoid brittle or disallowed reflection/internal access or private cloud endpoints.

Fallback: allow manual import of **verified historical aggregate KC + total loot value per boss**, optionally from user-provided exports. Mark aggregate imports as distinct from event-level data:

- Aggregate data does not prove specific timestamps or per-kill item distributions.
- Choose/document an expected GP baseline for the imported period; historical EV may not be reconstructible exactly.
- Avoid merging/double counting overlap with newly recorded live events; show import provenance and effective date.
- Design future import formats with idempotency/deduplication and a preview/confirmation step.

## 3. Wiki expected-value integration

Research and validate current capabilities of:

- OSRS Wiki item price API (for mapping item IDs to price estimates).
- OSRS Wiki boss money-making guide / drop tables / relevant machine-readable data.
- RuneLite item pricing APIs.

**Important distinction:** An item price feed does not inherently provide a boss's **expected GP per kill**. Expected value might need a curated boss catalog and drop-rate calculations, or another legitimate machine-readable source. Do not hardcode unverified values or scrape HTML on every kill.

Proposed architecture:

- `ExpectedValueService` resolves source + loot method + as-of snapshot to expected GP and optionally common/unique contributions.
- `WikiDataClient` retrieves validated data respectfully (cache, rate limits, documented User-Agent requirements if applicable); gracefully use last known good snapshot during outages.
- A local catalog starts with a few bosses, makes boss-specific quirks explicit, and can expand.
- Allow a manual expected GP override for testing and unsupported bosses, visually labeled.
- Store source/version and valuation method with saved records.
- Respect all current RuneLite Plugin Hub rules on external connections, client threading, and packaging.

**Previous conversation mentioned illustrative Maggot King Wiki figures of ~489,466 GP normal kill and ~32,361 GP without two major uniques. These were point-in-time claims, NOT verified specifications. Re-check live Wiki data and semantics before using any numbers.**

## 4. Initial Java architecture (proposed, subject to actual template conventions)

```text
loot-luck-tracker/
  src/main/java/com/lootluck/
    LootLuckPlugin.java
    LootLuckConfig.java
    model/
      LootEventRecord.java
      LootItemRecord.java
      BossDefinition.java
      ExpectedValueSnapshot.java
      LuckStats.java
      HistoricalAggregate.java
    service/
      LootCaptureService.java
      BossAttributionService.java
      ExpectedValueService.java
      StatisticsService.java
      SessionService.java
    storage/
      LootRepository.java
      [concrete storage implementation]
    wiki/
      WikiDataClient.java
    ui/
      LootLuckPanel.java
      OverviewPanel.java
      RankingsPanel.java
      BossDetailsPanel.java
  src/test/java/com/lootluck/
    [tests]
```

Prefer immutable or effectively immutable event records. Use `long` for GP and quantities where needed. Separate pure Java analytics and domain types from RuneLite adapters and Swing UI. Consider a provider interface for EV lookup so test fixtures and manual values can be injected. Never do blocking HTTP or disk work on the RuneLite client thread.

### Illustrative record (not a locked schema)

```json
{
  "eventId": "generated-unique-event-id",
  "profileKey": "local-profile-key",
  "source": "Maggot King",
  "rewardMethod": "normal",
  "timestampUtc": "2026-11-09T02:30:00Z",
  "sessionId": "session-001",
  "items": [{ "itemId": 995, "quantity": 10000, "unitPriceGp": 1, "category": "common" }],
  "actualGp": 10000,
  "expectedGp": 100000,
  "expectedCommonGp": 30000,
  "expectedUniqueGp": 70000,
  "valuationSource": "illustrative-test-fixture",
  "attributionStatus": "confirmed"
}
```

The example values are intentionally fake, for demonstrating shape only. Real event IDs, account keys, enum values, and pricing need implementation.

## 5. Original milestones and definition of done

This was the initial build order. Phases 1–3 and parts of 4–5 are implemented in the local prototype. Follow the **Current direction and priority order** at the top of this document for new work.

### Phase 1 — Local development setup

- Get the current official RuneLite example plugin template (`https://github.com/runelite/example-plugin`).
- Verify current build tool/JDK requirements rather than assuming a Java version.
- Create `Loot Luck Tracker` plugin and configuration.
- Run RuneLite locally in the IDE and show a placeholder sidebar panel.
- Set up Git and a private repository if desired.

**Done:** plugin runs locally and shows its panel.

### Phase 2 — Live loot capture + persistent database

- Subscribe to the appropriate loot event(s).
- Resolve/normalize boss identity, reward method, account and session.
- Persist itemized events atomically with duplicate protection.
- Test with event fixtures before relying on real kills.

**Done:** a Maggot King loot collection appears in the plugin and remains after restart, without duplicates.

### Phase 3 — GP pricing and expected-value service

- Implement price lookup and snapshotting.
- Implement boss-level EV definitions and cache/refresh policies.
- Begin with manually provided/test EV values, then verified Wiki integration.
- Support missing-price and unsupported-boss states.

**Done:** correct per-kill positive/negative delta on a supported boss.

### Phase 4 — Pure analytics + filters

- Aggregate by account, boss, time range, session, and category.
- Implement net GP, percentage luck, GP per kill, common/unique luck.
- Implement luckiest/unluckiest ranking and minimum KC filter.
- Add deterministic unit tests around dates, zero denominators, category definitions, and aggregates.

**Done:** statistics match independently computed fixtures.

### Phase 5 — Polished RuneLite UI

- Overview, Boss Rankings, Boss Details, Settings tabs.
- Custom time ranges and sorting.
- Kill/event table and cumulative-luck chart.
- Optional chat messages; readable red/green indicators.

**Done:** everything in MVP is accessible in RuneLite without developer tools.

### Phase 6 — Historic data imports

- Research supported access to RuneLite native history.
- If necessary, implement manual per-boss aggregate imports with clear limitations.
- Implement preview, provenance, overlap handling, backup/export.

**Done:** older data can contribute a clearly identified estimated baseline without corrupting recorded per-kill history.

### Phase 7 — Hardening and distribution

- Test unusual loot systems, rapid events, disconnects, multilogging/account switches, profile separation, price failures, and schema upgrades.
- Verify RuneLite Plugin Hub rules and prepare release metadata and documentation.

**Done:** plugin is robust, testable, and ready for formal submission review.

## 6. Original version 0.1 (MVP) checklist

This is the original planning checklist, not a live status report. See the README for verified implementation status.

- [ ] Build and run a RuneLite plugin locally.
- [ ] Show a sidebar UI panel.
- [ ] Capture Maggot King loot events.
- [ ] Store itemized event records persistently.
- [ ] Get actual GP values; use a clearly labeled initial expected GP value.
- [ ] Display current kill's luck delta.
- [ ] Display cumulative boss luck delta.
- [ ] Filter current session.
- [ ] Display basic luckiest/unluckiest boss rankings once multiple bosses are supported.

Do not delay a working MVP to implement all Wiki parsing, historical imports, or elaborate charts up front.

## 7. Explicit constraints / open design questions

These are intentionally **unresolved** and should be researched or decided during implementation, not silently guessed:

1. Exact current RuneLite API signatures and availability of `LootReceived` or relevant events in the plugin environment.
2. Boss-specific attribution semantics, especially alternative rewards and multiple loot events per kill.
3. Reliable, permissible machine-readable expected GP/kill data versus curated static calculations.
4. Live price convention (GE median/latest, alch value, untradeables, noted items, quantities).
5. Storage technology, location, size/retention policy, backups, and Plugin Hub suitability.
6. Safe way (if any) to access existing Loot Tracker history via supported APIs.
7. Definition of a session and user timezone/week start.
8. How rare-drop classifications/overrides are maintained as game drops change.
9. Which boss variants/modes deserve separate leaderboard entries.
10. How to handle old baseline imports if item prices or EV assumptions are unavailable.

## 8. Original instructions to Codex — first action (completed)

The original handoff requested a compiling local plugin with a sidebar, README, and small increments toward event capture. That setup is complete. Follow the current priority order at the top of this document for future development.

**User development preference:** They are comfortable with Java and software engineering; explain key architecture decisions but prioritize practical code, local setup, and a runnable prototype.
