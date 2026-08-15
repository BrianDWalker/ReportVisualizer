# Changelog

All notable product releases are documented here. Eclipse/Tycho build qualifiers identify a particular build of a semantic product version.

## Unreleased

- Per-result visualization sessions: chart/matrix/slicer/sort/calculated-field
  state is now scoped to a stable result identity (not the editor title) and
  is retained (bounded, LRU-capped) across panel switches.
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
- Known limitation: identifier quoting remains a fixed ANSI double-quote and
  is not yet derived from the active DBeaver `SQLDialect`.

## 1.0.0 — 2026-08-14

- Initial stable Results Visualizer release for DBeaver Community.
- Charts, multi-value Matrix/Pivot, slicers, formulas, sorting, and source-query aggregation.
- Theme-aware UI and a latest-only p2 update site.
