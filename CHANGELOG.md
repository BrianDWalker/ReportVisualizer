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

## 1.0.0 — 2026-08-14

- Initial stable Results Visualizer release for DBeaver Community.
- Charts, multi-value Matrix/Pivot, slicers, formulas, sorting, and source-query aggregation.
- Theme-aware UI and a latest-only p2 update site.
