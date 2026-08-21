# Results Visualizer for DBeaver

A local Eclipse/DBeaver extension that adds a dockable **Results Visualizer**
view to DBeaver desktop editions. It turns the active SQL result or Grouping
result into interactive charts and matrix/pivot views without requiring a
separate service.

[![Latest release](https://img.shields.io/github/v/release/BrianDWalker/ReportVisualizer?display_name=tag&include_prereleases=false)](https://github.com/BrianDWalker/ReportVisualizer/releases/latest)

**Current release line:** `1.2.2`

## Install

1. In DBeaver, choose **Help > Install New Software**.
2. Select **Add** and enter:
   `https://briandwalker.github.io/ReportVisualizer/`
3. Select **Results Visualizer for DBeaver**, complete the wizard, accept the
   EPL-2.0 license, and restart DBeaver.
4. Open **Window > Show View > Other > Results Visualizer > Results Visualizer**.

The public p2 repository contains only the latest release. To update, use
**Help > Check for Updates**. To uninstall, use **Help > Installation Details >
Installed Software**, select **Results Visualizer for DBeaver**, choose
**Uninstall**, and restart DBeaver.

Release downloads and checksums are available on the
[GitHub Releases page](https://github.com/BrianDWalker/ReportVisualizer/releases).

## Current behavior

- **Window > Show View > Other > Results Visualizer > Results Visualizer** registration
- **Visualize Results** button in the main toolbar
- Dockable **Results Visualizer** workbench view
- Detects the currently active DBeaver SQL result tab
- Uses the full view width for visualization; fields are selected only in the
  configuration dropdowns
- Source selector switches between standard Results and DBeaver Grouping output
- Renders Bar, Horizontal Bar, Stacked Bar, 100% Stacked Bar, Line, Area,
  Stacked Area, 100% Stacked Area, Column + Line, Scatter, Bubble, Pie, Donut,
  Heatmap, and Matrix/Pivot visualizations
- Automatically chooses a categorical X field and numeric Y field when possible
- Compact dropdown selectors assign X-Axis, Values, and optional Series
  fields. Values accepts any column type: numeric columns support the full
  SUM/AVG/MIN/MAX/COUNT/COUNT DISTINCT aggregation set, while string,
  boolean, and date/time columns support COUNT and COUNT DISTINCT
- Matrix/Pivot switches those roles to Rows, Values, and Columns, supports
  inline ordered hierarchical Row and Column fields, optional subtotals,
  optional row/column totals, and horizontal/vertical scrolling
- Result-derived slicers offer Select Values for text, numeric, and date/time
  fields before local aggregation and warn when the DBeaver row limit may
  make the local distinct list incomplete. Numeric/date fields also retain
  their comparison, range, null, and relative-date predicate modes
- Add/Edit Slicer preloads and replaces an existing field slicer, including
  checked distinct values and typed predicate inputs
- Slicers can preview a full-source `SELECT DISTINCT` query
- General custom SQL fields accept a reusable database field expression;
  double-clicking a result field inserts valid SQL
- Formula management and the unified Source Query builder support multiple
  definitions plus Add, Edit, and Delete workflows
- Source Query has Available Fields, Custom SQL Fields, and Aggregations sections;
  it supports multiple grouping fields and named aggregations without custom-field
  type switching, and shows the full generated SQL.
  Execute runs the aggregate in the current visualization and follows its result
- Local SUM, AVG, MIN, MAX, COUNT, and COUNT DISTINCT aggregation without
  rewriting SQL
- Multi-series bar, line, and scatter rendering with an in-chart legend
- Combo charts can place line measures on an independently scaled secondary
  Y-axis. Bubble charts use the first selected Value as Y and the second as
  bubble size.
- Matrix Values supports multiple ordered measures of any compatible
  aggregation in one pivot, following the same numeric/non-numeric
  aggregation rules as the chart Values well
- Chart type switching that preserves compatible field assignments
- Report-style Matrix options include stepped/tabular layouts, hierarchical
  collapse/expand, configurable subtotal levels and totals, Top-N, number and
  percentage formats, thousands separators, heat scales, and data bars
- Automatic readable Y-axis ceilings (for example 91 becomes 100), with an
  editable Y Max override
- **Reset Visualization** clears chart configuration without touching SQL/results
- Formula editor with Name and Expression inputs
- Calculated fields appear beside result fields and can be assigned to any
  compatible field well; numeric formulas are expanded to source expressions in Source Query
- Calculated field expressions support `[field]` references, numeric
  constants, `+`, `-`, `*`, `/`, parentheses, unary signs; the numeric
  functions `ABS`, `ROUND`, `CEIL`, `FLOOR`, `SQRT`, `POWER`, `MIN`, `MAX`,
  `LOG`, `EXP`, and `MOD`; the null-tolerant functions `COALESCE`, `NULLIF`,
  and `IF(condition, whenTrue, whenFalse)`; comparison operators
  (`= <> != > < >= <=`); and the logical operators `AND`, `OR`, `NOT`
- The calculated-field guide includes examples and supports double-click field insertion
- Unknown fields and malformed expressions produce useful validation messages
- Null, incompatible, divide-by-zero, and non-finite calculations resolve to
  null without interrupting the remaining rows or visualization
- Uses a compact theme-integrated visualization builder without stretched
  background bands
- Refreshes after query reruns, result changes, and result-tab switches
- Graceful `No active result set available.` state
- Plug-in-owned immutable snapshots keep DBeaver objects out of visualization code
- Local views use exactly the rows fetched by DBeaver after its
  connection/editor result limit
- Centralized Eclipse error logging for command/view startup failures
- Installable p2 update-site repository
- Entirely local SWT rendering: no localhost service, external JavaScript,
  Node.js, Python, cloud service, or database modification at runtime
- Build inputs are hardened with immutable GitHub Actions commits and a
  SHA-256-verified Maven wrapper distribution
- Visualization state (chart configuration, matrix layout, slicers, sorts,
  calculated fields) is scoped per active result/Grouping panel, keyed by a
  stable result identity rather than the editor title, and survives switching
  between panels. Sessions are bounded (LRU, capped) and cleared when the view
  is disposed
- Source-query aggregation picks a safe SQL strategy per query: a direct
  `GROUP BY` rewrite of the original `FROM` clause when structurally safe, or
  an automatic derived-table fallback (`SELECT ... FROM (original query) rv_source`)
  for CTEs, joins, `UNION`/`INTERSECT`/`EXCEPT`, multi-statement text, or other
  structurally ambiguous SQL. The generated SQL preview always reflects the
  strategy actually used
- Source Query aggregate results rebind their returned aliases to the active
  visualization controls; reopening Source Query always uses the original
  result schema and SQL, while **Back to Original** restores the pre-aggregate
  visualization
- Typed slicers distinguish SQL `NULL` from the literal text `(null)` and
  compare numeric-looking values numerically rather than as raw strings
- Date Level acts on the active DATE/DATETIME X/Series or Matrix Row/Column
  directly, with Original, Year, Quarter, Month, Day, Drill Up, and Drill Down;
  field dropdowns contain only real result fields
- **Save Preset / Load Preset / Delete Preset** persist and restore the complete
  visualization state per result shape (chart type, ordered rows/columns and
  multi-value Matrix values, aggregation, Y maximum, totals/subtotals, slicers,
  ordered sorts, and referenced formulas). Load and Delete present a selectable
  list of compatible saved presets; exact-name, versioned storage prevents
  sanitized-name collisions and ignores corrupt entries safely
- Visualization export: **Save PNG**, **Save JPEG**, **Save SVG**, **Save PDF**,
  and **Copy Image** (to the system clipboard) for the currently rendered
  chart or matrix, sharing the same on-screen rendering pipeline. Copy Image
  completes silently and opens a dialog only when capture or clipboard
  transfer fails

## Known limitations

- Visualization export supports PNG, JPEG, SVG, and PDF (plus Copy Image to
  clipboard). SVG export is real vector output for standard charts and
  pie/donut/scatter/heatmap/Matrix content built from the same shared
  rendering pipeline used on screen. PDF export currently embeds a
  high-resolution raster of the rendered visualization on a single,
  appropriately sized page rather than emitting true vector PDF content; this
  is a deliberate scope decision to avoid a disproportionate increase in
  packaging complexity for this release, documented here rather than silently
  shipped as if it were vector output
- DBeaver does not expose a dedicated close lifecycle callback for every
  result controller, so ReportVisualizer retains visualization sessions in a
  bounded cache and automatically cleans them up
- Large datasets up to 100,000 rows are covered by automated tests, but live
  interactive performance may vary by database, driver, machine, and DBeaver
  configuration
- DBeaver's public CE build target tracks the latest release rather than
  exposing a permanently version-pinned target repository. Each
  ReportVisualizer release records the exact DBeaver version used during
  build and validation
- Matrix/Pivot export renders the entire matrix content (all rows and
  columns), not just the currently visible scrolled viewport. Very large
  matrices are still subject to the existing 2,500-cell on-screen rendering
  cap

## SQL compatibility

SQL identifier quoting follows the active DBeaver datasource/dialect. If
dialect metadata is unavailable, standard ANSI double quotes are used as a
safe fallback. Quoting is derived directly from the active datasource's
`SQLDialect.getIdentifierQuoteStrings()` (the same DBeaver API the platform
itself uses to decide quote characters), so it correctly follows the active
dialect's declared quote pair, including asymmetric styles such as SQL
Server's `[` / `]` brackets, not just symmetric single-character styles like
ANSI `"` or MySQL `` ` ``. See [`docs/architecture.md`](docs/architecture.md)
for the implementation details.

## Compatibility

Results Visualizer is an Eclipse/DBeaver extension designed for DBeaver
desktop editions that support Eclipse extensions.

Built and validated against:
- DBeaver Community 26.1.4 (or the exact currently recorded resolved build;
  see the `dbeaver-core-version` build artifact and release notes)

Expected compatible desktop editions:
- DBeaver Community
- DBeaver Lite
- DBeaver Enterprise
- DBeaver Ultimate

The plugin uses shared DBeaver core, SQL-editor, datasource/dialect, and
result-set APIs rather than functionality specific to Community Edition.
Community is currently the primary automated build/test target. Other
desktop editions are supported by design but should be runtime-validated
before claiming edition-specific certification. This project does not make
compatibility claims for CloudBeaver or other server/browser products, which
are architecturally distinct from the Eclipse/DBeaver desktop extension model
this plugin targets.

- Java 21 JDK or JRE (DBeaver distributions normally bundle a compatible runtime)
- Internet access during the build to resolve Maven/Tycho and the target platform

## Build

From a fresh clone, use Java 21 and run:

```sh
./scripts/build-update-site.sh
```

The script runs the full Maven/Tycho build and test suite, creates a clean p2
repository from scratch, and validates that exactly one Results Visualizer
version is exposed. The Eclipse qualifier (the `.qualifier` suffix in the
bundle/feature versions) is generated fresh by Tycho on every build from the
current build timestamp — it is not pinned in `.mvn/maven.config` or anywhere
else, so each published build gets a distinct, newer qualifier and DBeaver's
**Help > Check for Updates** can detect it as an available update. Product
releases use semantic versions in the bundle and feature definitions.

The generated installable repository is:

```text
releng/com.brianwalker.dbeaver.resultsvisualizer.repository/target/repository
```

## Use a locally built update site

1. Build the project.
2. In DBeaver, choose **Help > Install New Software**.
3. Select **Add**, then **Local**, and choose the generated `target/repository`
   directory above.
4. Select **Results Visualizer for DBeaver**, complete the wizard, accept the
   EPL-2.0 license, and restart DBeaver.
5. Choose **Window > Show View > Other > Results Visualizer > Results
   Visualizer**, or use the toolbar button.
6. With no completed query, confirm the dockable **Results Visualizer** view
   displays `No active result set available.`
7. Execute a grouped query with a category and numeric measure. Confirm its
   fields appear, the category and measure are selected for X and Y, and the
   chart is rendered.
8. Select fields for **X**, **Values**, and optionally **Series**. Select an
   aggregation and confirm the chart redraws. Use **Sort…** to add ordered
   multi-field ASC/DESC keys.
9. Change **Chart Type**, rerun the query, and confirm compatible assignments
   remain active with the refreshed rows.
10. Choose **Formulas…**, add `Profit` with `[revenue] - [cost]`, then choose
    **Apply**. Confirm the new field appears
    in the field dropdowns and can be selected as Values while category remains
    the X-Axis.
11. Choose **Results panel** or **Grouping panel** from Source. Grouping must
    first be opened and populated in DBeaver.
12. Leave **Y Max** on Auto for a rounded ceiling, or type a custom maximum.
13. Choose **Reset Visualization** and confirm only the visualization state is cleared.

Local visualization follows DBeaver's own fetched row count, including the
result limit configured for the connection/editor. The full-source aggregate
action is an explicit exception: it generates a separate read-only query for
review and execution in DBeaver without altering the underlying result.

DBeaver installed under `/Applications` can be write-protected. If the install
wizard cannot write there, use a writable copy of `DBeaver.app` for development
testing, as recommended by DBeaver's Eclipse extension documentation.

## Project layout

```text
plugins/   Eclipse UI bundle (commands, views, services, models, rendering)
tests/     OSGi unit tests for plug-in-owned models and type normalization
features/  Installable Results Visualizer feature
releng/    Tycho project that generates the p2 repository under target/
docs/      Architecture, compatibility, and verification notes
scripts/   Local build and p2 validation commands
.github/   Continuous-integration and controlled release workflows
```

Generated deployment files are deliberately absent from `main`. The release
workflow replaces the `gh-pages` branch with the newly generated latest-only p2
repository. Previous releases remain available through Git history, tags, and
GitHub Releases rather than through active p2 metadata.

## Troubleshooting

- If no software appears, remove any older ReportVisualizer update-site entry,
  add the root URL above again, and clear **Group items by category** if needed.
- If DBeaver reports a missing older artifact, remove the stale update-site
  entry, restart DBeaver, add the root URL again, and install the current build.
- If installation cannot write into the application directory, run DBeaver
  from a user-writable location or use an account with permission to update it.
- If the visualizer says no result is available, execute a query and focus its
  Results or Grouping panel before refreshing the view.
- Build failures should first be checked for Java 21 and network access to the
  DBeaver and Eclipse p2 repositories declared in `pom.xml`.

See [docs/architecture.md](docs/architecture.md),
[docs/part-1-verification.md](docs/part-1-verification.md), and
[docs/part-2-verification.md](docs/part-2-verification.md), and
[docs/part-3-verification.md](docs/part-3-verification.md), and
[docs/part-4-verification.md](docs/part-4-verification.md), and
[docs/part-5-verification.md](docs/part-5-verification.md), and
[docs/phase-5-release-verification.md](docs/phase-5-release-verification.md), and
[docs/refinements-verification.md](docs/refinements-verification.md), and
[docs/visualization-expansion.md](docs/visualization-expansion.md).

Release history is maintained in [CHANGELOG.md](CHANGELOG.md).
