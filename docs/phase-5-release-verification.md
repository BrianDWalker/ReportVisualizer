# Phase 5 release verification

Verified on August 20, 2026 on Apple silicon macOS with the Java runtime bundled
with DBeaver Community.

## Release scope

The `1.2.0` release line closes the visualization expansion with:

- stable result binding, safe aggregate-query correlation, and stacked-SQL
  rejection;
- typed slicers, date hierarchies, and per-result session/preset state;
- multi-value charts, combo and bubble rendering, 100% stacked variants,
  optional combo secondary axis, and chart presentation options;
- report-style Matrix layouts, hierarchy collapse/expand, configurable
  subtotal and total levels, Top-N, number formatting, conditional color/data
  bars, scrolling, and the 2,500-logical-cell guard.

## Automated regression and performance checks

Run from a clean reactor:

```sh
./scripts/build-update-site.sh
```

The build runs `./mvnw clean verify`, all five Tycho modules, the headless SWT
test application, and `scripts/validate-update-site.sh`. The Phase 5 candidate
completed 122 tests with zero failures or errors. Coverage includes all chart
types through the shared SVG rendering path, combo secondary-axis output,
bubble-size assignment, preset v6 round trips, Matrix sizing/cell caps, typed
slicers and date levels, stale aggregate result rejection, cancellation, SQL
rewrite safety, and exact result-session restoration.

The large-result regression constructs and aggregates synthetic snapshots of
10,000, 50,000, and 100,000 rows in the normal `ChartDataBuilder` path. Matrix
tests independently exercise larger logical grids and the render cap. These
tests are deterministic performance guards; real UI responsiveness still
depends on the driver, DBeaver fetch limit, machine, and datasource.

## Security and supply-chain review

- Runtime code contains no process launching, script engine, socket server,
  reflection bridge, or remote visualization service. Visualization and
  expression evaluation remain local and strongly typed.
- Source SQL normalization rejects stacked statements outside comments and
  quoted literals. Generated aggregate SQL uses quoted identifiers and a safe
  direct-rewrite or derived-table strategy.
- GitHub Actions dependencies are pinned to immutable commit SHAs (with the
  readable release tag retained in comments).
- Maven 3.9.11 is locked by `distributionSha256Sum` in the wrapper properties.
- The p2 repository validator rejects mixed Results Visualizer versions and
  requires the expected plugin and feature artifacts.

## Live DBeaver acceptance

The generated `1.2.0.202608210053` p2 repository was installed into an isolated
writable copy of DBeaver Community 26.1.4 with an isolated workspace. Equinox
reported the Results Visualizer bundle `ACTIVE`; the toolbar command and
dockable view registered, and the empty-result state rendered correctly. A
real query against DBeaver's SQLite sample database returned four columns and
200 fetched rows. The view displayed DBeaver's row-limit warning and rendered
the default local `SUM(revenue)` chart grouped by month.

The remaining chart variants (including combo secondary-axis and 100% stacked
rendering), Matrix paths, state restoration, and export formats are covered by
the shared headless renderer/model tests described above. The normal DBeaver
installation and workspace were not modified.

## Remaining limitations

- The public DBeaver CE p2 target is a floating `latest` repository. CI and the
  release workflow record the exact resolved DBeaver core version for each
  build and release.
- PDF export embeds a high-resolution raster; SVG remains the vector export.
- Matrix rendering is capped at 2,500 logical value cells after collapse and
  Top-N. Use slicers or Top-N for larger reports.
- Live behavior in Lite, Enterprise, and Ultimate is expected from shared APIs
  but is not edition-certified by this Community-based validation.
