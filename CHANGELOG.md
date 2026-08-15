# Changelog

All notable product releases are documented here. Eclipse/Tycho build qualifiers identify a particular build of a semantic product version.

## Unreleased

## 1.1.1 — 2026-08-15

- Saved presets now restore the full visualization state, including ordered
  chart/Matrix field assignments, multiple Matrix values, aggregation, Y
  maximum, totals/subtotals, slicers, sorts, and referenced calculated fields.
  Loading no longer reinitializes and overwrites saved X/Values/Series choices.
  Load and Delete use a selectable compatible-preset list, while exact-name
  versioned preference keys prevent sanitized-name collisions and corrupt saved
  entries are ignored safely.

## 1.1.0 — 2026-08-15

- Export ▾ now offers PNG, JPEG, vector SVG, and PDF export in addition to Copy
  Image, driven by a new shared `ChartGraphics` rendering abstraction so every
  format renders through the exact same chart/matrix layout code as the
  on-screen view (no duplicated per-format drawing logic, no visual drift
  between formats). SVG is a real vector document (not a rasterized
  screenshot); PDF embeds a high-resolution JPEG raster of the chart on a
  single page — a documented raster fallback rather than true vector PDF
  content, since generating vector PDF content streams would require a second
  full reimplementation of every renderer with no corresponding user-visible
  benefit for this release. Neither format required adding any new external
  dependency (SVG is hand-built XML; PDF is a minimal hand-written PDF/JPEG
  wrapper). Matrix/Pivot Table and Heatmap export (PNG, JPEG, SVG, and PDF)
  now always captures the entire matrix content, not just the currently
  visible, scrolled viewport — previously PNG/Copy Image export of a matrix
  larger than the on-screen area silently produced a cropped image.
- Per-result visualization sessions: chart/matrix/slicer/sort/calculated-field
  state is now scoped to a stable result identity (not the editor title) and
  is retained (bounded, LRU-capped) across panel switches; the identity is
  never removed from the LRU cache on a mere focus switch (only replaced via
  natural LRU eviction or explicit disposal), fixing a defect where switching
  between panels of the same editor silently discarded the outgoing panel's
  state. An explicit Source-vs-Aggregate display mode now tracks whether the
  chart is showing the original DBeaver result or the last executed Source
  Query aggregate result, with a "Back to Original" action that re-renders
  the held source snapshot without re-running SQL.
- Values field no longer restricted to numeric columns: string, boolean, and
  date/time columns can now be dropped into Values (and the Matrix/Pivot
  Values well) and aggregated with COUNT / COUNT DISTINCT. The Aggregation
  drop-down now only lists aggregations that are valid for the selected
  column's type (numeric columns keep the full SUM/AVG/MIN/MAX/COUNT/COUNT
  DISTINCT set) and automatically falls back to COUNT if the current
  aggregation becomes invalid after a Values column change. MIN/MAX are
  intentionally still numeric-only in this pass: a non-numeric MIN/MAX has no
  well-defined numeric chart-axis representation without a larger,
  out-of-scope calendar-aware axis feature.
- COUNT DISTINCT now canonicalizes numeric values before comparing them, so
  `1` (Integer), `1.0` (Double), and `1.00` (BigDecimal) — which can arrive as
  different Java wrapper types depending on JDBC driver/type — are correctly
  counted as a single distinct value instead of three.
- Calculated-field formula language expanded: `LOG`, `EXP`, `MOD`, `COALESCE`,
  `NULLIF`, `IF(condition, whenTrue, whenFalse)`, comparison operators
  (`= <> != > < >= <=`), and `AND` / `OR` / `NOT` are now supported, with a
  new precedence grammar (Or → And → Not → Comparison → Additive → Term →
  Unary → Primary) and null-tolerant evaluation for `COALESCE`/`NULLIF`/`IF`
  (unlike the existing math functions, these do not fail the whole formula
  just because one branch is null). The Source Query SQL translator now
  rewrites `MOD(a, b)` to `(a % b)` and `IF(c, t, f)` to
  `CASE WHEN c THEN t ELSE f END` for cross-dialect compatibility (some
  dialects, e.g. SQL Server/SQLite, have no `MOD` function, and `IF(...)` as
  an expression is a MySQL-specific extension); `COALESCE`, `NULLIF`,
  comparisons, and `AND`/`OR`/`NOT` are already standard SQL and pass through
  unchanged. `LOG` is a documented known limitation: it is left as a
  pass-through in generated SQL since its argument order and base (natural
  vs. base-10) are not consistent across dialects — verify multi-argument
  `LOG` formulas used inside the Source Query against the target database.
- Consolidated the Preset actions (Save/Load/Delete) and the Export actions
  (now Save PNG/Save JPEG/Save SVG/Save PDF/Copy Image) behind single
  "Presets ▾" and "Export ▾" drop-down buttons respectively, reducing
  configuration-panel button clutter.
- The verbose inline formula guide in the calculated-field dialog was
  replaced with a one-line hint plus a "Formula Help…" button that opens the
  full function/operator/example reference (updated to document the new
  `LOG`/`EXP`/`MOD`/`COALESCE`/`NULLIF`/`IF`/comparison/logical additions).
- DBeaver-aware SQL rewrite strategy: source-query aggregation now uses a
  paren-and-string-aware scanner to safely choose between an optimized direct
  `GROUP BY` rewrite and a derived-table fallback, instead of brittle
  regex-only detection.
- Typed slicers correctly distinguish SQL `NULL` from the literal `(null)`
  text and compare numeric-looking values by numeric equivalence.
- Saved visualization presets: Save/Load/Delete a named chart+matrix layout
  per result shape, backed by Eclipse workspace preferences.
- Chart export: Save PNG and Copy Image to clipboard for the current chart.
- Build/release traceability: since DBeaver publishes no version-pinned
  public p2 repository for the CE product, the build still targets the
  floating "latest" update site, but CI and releases now resolve and record
  the exact DBeaver core version actually validated against, instead of
  silently building against an untracked moving target.
- Dialect-aware identifier quoting: quote characters are now derived directly
  from the active datasource's `SQLDialect.getIdentifierQuoteStrings()`,
  supporting asymmetric quote pairs (e.g. SQL Server `[`/`]` brackets) as well
  as symmetric ones (ANSI `"`, MySQL `` ` ``), replacing a prior
  detection heuristic that silently always fell back to ANSI quoting
  regardless of the active dialect. Falls back to ANSI double quotes only when
  no dialect/datasource metadata is available.
- Added an automated large-row regression test (10k/50k/100k synthetic rows)
  covering chart dataset construction; see "Large-result and runtime
  validation" in `docs/architecture.md` for what is and is not covered by
  automated testing versus manual live-DBeaver validation.
- Known limitation: there remains no dedicated per-controller disposal hook
  in the DBeaver result-set API surface used here; the bounded LRU session
  cache plus explicit view-level cleanup is the deliberate mitigation.
- Known/deferred: chart hover tooltips, point-level highlighting, and
  zoom/pan interactions are not implemented; the built-in legend (already
  present) is the only interactive affordance today. Matrix/Pivot visual
  polish beyond the existing theming is also deferred. Release-artifact
  signing was not investigated in this pass. These are called out explicitly
  rather than claimed as done.

## 1.0.1 — 2026-08-15

- Updated DBeaver dialect-aware identifier quoting fix and verification for
  live update-site publication.
- Improved quote handling for symmetric and asymmetric SQL dialects and
  documented the remaining runtime validation limitations.

## 1.0.0 — 2026-08-14

- Initial stable Results Visualizer release for DBeaver Community.
- Charts, multi-value Matrix/Pivot, slicers, formulas, sorting, and source-query aggregation.
- Theme-aware UI and a latest-only p2 update site.
