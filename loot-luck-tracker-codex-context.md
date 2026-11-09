# Loot Luck Tracker — Codex Project Context

> **Purpose:** Paste this document into a Codex workspace or keep it as `PROJECT_CONTEXT.md` in the repository. It captures the product requirements, technical design intentions, implementation order, and caveats discussed with ChatGPT. It is a planning brief, **not** an assertion that code or dependencies have already been implemented.

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

## 5. Milestones and definition of done

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

## 6. Version 0.1 (MVP) checklist

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

## 8. Instructions to Codex — first action

**Please treat this file as the product requirements and project handoff.** Start with Phase 1, not the whole roadmap. Inspect the current RuneLite example plugin and confirm the active JDK/Gradle/plugin APIs. Scaffold a clean, compiling local RuneLite plugin named **Loot Luck Tracker** with a panel and config, using the actual current template conventions. Keep architectural seams for event storage, EV, analytics, and UI, but do not overbuild yet. Add a brief README for running locally and state what was verified versus stubbed. Then proceed to event capture in small, testable increments.

**User development preference:** They are comfortable with Java and software engineering; explain key architecture decisions but prioritize practical code, local setup, and a runnable prototype.
