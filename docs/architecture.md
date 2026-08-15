# Architecture

## Part 1 decision record

The plug-in foundation depends only on public Eclipse runtime and workbench
APIs. It deliberately has no compile-time dependency on DBeaver internals yet.
This keeps startup stable and gives Part 2 a narrow place to introduce a
DBeaver result-viewer adapter.

The bundle is separated by responsibility:

| Package | Responsibility |
| --- | --- |
| `commands` | Workbench command handlers and user-triggered actions |
| `views` | SWT workbench views and presentation composition |
| `services` | DBeaver integration adapters and orchestration services |
| `model` | Plug-in-owned result and visualization data types |
| `visualization` | Visualization state, aggregation, and rendering |
| `calculatedfields` | Restricted expression parsing and evaluation |

Only `commands` and `views` contain behavior in Part 1. The remaining packages
are documented source boundaries, not fake implementations. Later parts can
add behavior without moving the command or view entry points.

## Error isolation

The command catches workbench view-initialization failures, records them in the
Eclipse/DBeaver Error Log, and shows a concise error dialog. The activator owns
the log entry point. Part 1 does not register long-lived listeners or allocate
custom SWT resources.

## Build and packaging

Tycho builds one Eclipse bundle, one test fragment, one installable feature,
and one p2 repository.
The target combines DBeaver Community's current p2 repository with Eclipse
2026-06. JavaSE-21 matches DBeaver Community 26.1.x on macOS.

The bundle uses broad, bounded Eclipse API ranges so it can load in compatible
DBeaver updates while refusing a future incompatible Eclipse 4.x API.

## Part 2 result-set integration

`DBeaverResultSetService` is the sole DBeaver result-viewer adapter. It uses the
exported `IResultSetController`, `IResultSetListener`, and
`ResultSetHandlerMain` APIs to resolve the current result. The service listens
for result loads/model changes plus Eclipse part and SWT focus/selection events;
it does not poll continuously.

The adapter converts DBeaver bindings and rows immediately into immutable
plug-in models (`ResultSetSnapshot`, `ResultColumn`, and `ResultRow`). The view,
future chart code, and future calculated-field code never retain DBeaver result
objects. Values are normalized into ordinary Java scalar/time types where
possible, with safe display strings for vendor-specific values.

The snapshot copies exactly the rows already present in DBeaver's model, so the
connection/editor fetch limit remains authoritative. The plug-in neither adds a
second row cap nor asks DBeaver to fetch additional rows.

The source selector defaults to the normal result controller. It can also use
DBeaver's grouping-result controller after the Grouping panel has been opened
and focused. If no grouping result exists, the view gives an actionable message
instead of silently switching back to normal results.

Type normalization uses JDBC type codes, DBeaver semantic data kinds, and the
declared database type name. Semantic boolean/date-time information takes
precedence where drivers expose those values through generic JDBC codes (for
example SQLite `DATETIME`).

## Part 3 local visualization (original milestone)

`ResultsVisualizerView` composes three native SWT areas: the Fields panel, the
Chart canvas, and the Chart Configuration controls. The Fields and Chart panels
share a horizontal `SashForm`, so the visualizer remains a normal dockable and
resizable DBeaver view. Configuration uses read-only SWT combos for chart type,
X axis, and Y axis.

The chart layer is independent of DBeaver APIs:

| Type | Responsibility |
| --- | --- |
| `ChartDataBuilder` | Chooses sensible defaults and projects a result snapshot into chart points |
| `ChartConfiguration` | Records chart type plus X/Y column selections |
| `ChartDataset` / `ChartPoint` | Immutable renderer input |
| `ChartRenderer` | Small extension point implemented by each chart type |
| `ChartRendererRegistry` | Maps chart types to renderers and exposes available types to the UI |
| `ChartCanvas` | Double-buffered SWT canvas that delegates painting to the registry |

Bar, Line, and Scatter are separate renderers. Adding another chart does not
require changing result-set integration: implement `ChartRenderer`, add a
`ChartType`, and register it. Shared axis, scale, label, and empty-state drawing
lives in `ChartDrawing`.

Rendering is entirely in-process using SWT `GC`. There is no embedded browser,
web server, external JavaScript runtime, Python process, cloud API, or network
request. SQL nulls remain null in immutable snapshots; numeric nulls and
non-finite values are skipped when building chart points.

## Part 4 interactive builder

`VisualizationConfiguration` is the immutable state for the X, Values, and
optional Series wells, chart type, and aggregation. `-1` represents an
unassigned well, allowing Reset Visualization to clear state without clearing
the active result snapshot. Compatible assignments are retained across result
refreshes; a changed field name or normalized type causes safe default mapping.

Fields are displayed in a compact Field/Type table using the database type name.
Read-only X/Values/Series combos are the single assignment mechanism; the earlier
drag/drop and context-menu paths were removed. Explicit accessible names and
command focus support keyboard and assistive-technology navigation.

The Values well (and the Matrix/Pivot Values well) accepts columns of any
normalized type, not just numeric ones. `Aggregation.compatibleWith(type)`
determines which aggregations are offered for the selected column: numeric
columns keep the full SUM/AVG/MIN/MAX/COUNT/COUNT DISTINCT set, while string,
boolean, and date/time columns are restricted to COUNT/COUNT DISTINCT (MIN/MAX
of a non-numeric value has no well-defined numeric chart-axis representation
without a larger, deliberately out-of-scope calendar-aware axis feature). The
Aggregation drop-down is repopulated whenever the Values selection changes and
automatically falls back to COUNT if the previously selected aggregation is no
longer valid for the new column's type.

`ChartDataBuilder` performs SUM, AVG, MIN, MAX, COUNT, and COUNT DISTINCT over the
plug-in-owned snapshot. A stable insertion-ordered group key consists of the X
value and optional Series value. Aggregation therefore never modifies or reruns
SQL. `ChartPoint` carries a series name, and `ChartDataset` exposes categories,
series names, and per-series points. Each renderer consumes those helpers to
draw grouped bars or separately colored line/scatter series plus a local SWT
legend.

COUNT DISTINCT canonicalizes numeric raw values (via `BigDecimal` with trailing
zeros stripped, plus an explicit zero special-case to avoid a known
cross-JDK `BigDecimal.ZERO.stripTrailingZeros()` inconsistency) before adding
them to the distinct set, so values that arrive as different Java wrapper
types depending on JDBC driver/type (`Integer 1`, `Double 1.0`,
`BigDecimal("1.00")`) are correctly treated as the same distinct value.

## Part 5 calculated fields

`CalculatedFieldService` projects user-defined calculated columns onto the
immutable `ResultSetSnapshot`. A projection appends `CALCULATED` numeric columns
and produces a new snapshot; it never writes through to DBeaver, changes the SQL
text, or reruns the query. Definitions remain owned by the visualizer view and
are reapplied when compatible result rows refresh.

`ExpressionCompiler` is a purpose-built recursive-descent parser. Its grammar
supports case-insensitive `[field]` references, numeric constants, arithmetic
operators, parentheses, unary signs; the numeric functions `ABS`, `ROUND`,
`CEIL`, `FLOOR`, `SQRT`, `POWER`, `MIN`, `MAX`, `LOG`, `EXP`, and `MOD`; the
null-tolerant functions `COALESCE`, `NULLIF`, and `IF(condition, whenTrue,
whenFalse)`; the comparison operators `= <> != > < >= <=`; and the logical
operators `AND`, `OR`, `NOT`. Precedence (loosest to tightest) is
Or → And → Not → Comparison → Additive → Term → Unary → Primary. Boolean-ish
results are represented internally as `1.0`/`0.0`/`null`; `AND`/`OR`/`NOT`
return `null` ("unknown") if either operand is `null` — a deliberate
simplification of full SQL three-valued logic that keeps the grammar small
while still failing safe. It has no Java, JavaScript, shell, SQL, reflection,
filesystem, or process execution facility. Unsupported tokens and function
names fail during validation before a definition is accepted.

Compiled expressions evaluate one immutable row at a time. Null, boolean,
temporal, incompatible, divide-by-zero, non-finite, and invalid-logarithm
inputs produce a null calculated value for that row, with two deliberate
exceptions: `COALESCE`, `NULLIF`, and `IF` inspect nulls explicitly (first
non-null argument, null-if-equal, condition-gated propagation) instead of the
generic math-function path's fail-fast-on-any-null-argument behavior. One
invalid definition is reported separately and does not prevent valid
definitions from being projected. Definitions are compiled and resolved
against column indexes once per projection rather than parsing an expression
for every row.

`CalculatedFieldSqlTranslator` expands validated formulas into SQL for Source
Query generation. Field references are substituted with quoted SQL
identifiers, then `MIN(a, b)`/`MAX(a, b)` (SQL aggregate functions, not
two-argument scalars) are rewritten to `LEAST`/`GREATEST`, `MOD(a, b)` is
rewritten to `(a % b)` (several dialects have no `MOD` function), and
`IF(c, t, f)` is rewritten to `CASE WHEN c THEN t ELSE f END` (`IF(...)` as an
expression is a MySQL-specific extension, not standard SQL). The rewrite uses
a paren-and-comma-depth-aware argument splitter (not naive regex) so nested
calls translate correctly and a malformed/unexpected-arity match is left
untouched rather than guessed at. `COALESCE`, `NULLIF`, comparisons, and
`AND`/`OR`/`NOT` are already standard SQL and pass through unchanged. `LOG` is
a known, documented limitation: it is left as a pass-through because its
argument order and base (natural vs. base-10) are not consistent across SQL
dialects, so multi-argument `LOG` formulas used inside the Source Query should
be verified against the target database.

`CalculatedFieldDialog` provides the Name and Expression editor, a field/type
list with double-click insertion, a one-line formula hint plus a
"Formula Help…" button that opens the full function/operator/example
reference, and Create/Cancel actions. After creation, the new column uses the
same field table, dropdown selectors, aggregation pipeline, and chart
renderers as database fields.

Automatic chart scaling chooses a readable 1/2/2.5/5/10 upper bound rather than
using the exact data maximum. `VisualizationConfiguration` can carry a user Y
maximum override, which is propagated through `ChartDataset` to every renderer.

## Current visualization surface

The current view replaces the original Fields table and sash with one full-width
visualization canvas plus dropdown field selectors. It adds matrix/heatmap,
additional charts, snapshot slicers, general server-side SQL dimensions, and a
reviewable full-source aggregate query. See
[`visualization-expansion.md`](visualization-expansion.md) for the current
behavior and safety boundary.

## v1.1 additions: per-result sessions, SQL rewrite safety, presets

Three architectural additions were made after the surface described above,
without changing the immutable-snapshot rendering model:

- **Per-result sessions** (`VisualizerSessionManager`, `VisualizerSession`):
  chart configuration, matrix options, calculated fields, slicers, sort rules,
  and source-query state are keyed by a stable result identity
  (`DBeaverResultSetService#activeResultIdentity()` — derived from the active
  controller and result mode, not the editor title) instead of being global to
  `ResultsVisualizerView`. The session map is LRU-bounded
  (`VisualizerSessionManager.MAX_SESSIONS`) and cleared on view disposal, since
  there is no dedicated per-controller disposal callback available from the
  DBeaver result-set API surface used here. `switchSessionIfNeeded` persists
  the outgoing session's state and only ever *replaces* it in the map — it no
  longer removes the outgoing session, which previously discarded a panel's
  configuration the moment focus moved away from it, defeating the purpose of
  per-result persistence. `VisualizerSession.DisplayMode` (`SOURCE`/
  `AGGREGATE`) tracks whether the view is currently rendering the original
  DBeaver result (`baseSnapshot`) or the last executed Source Query aggregate
  result (`aggregateSnapshot`); both snapshots persist/restore together with
  the session so a "Back to Original" action can re-render the held source
  snapshot without re-running SQL or touching DBeaver.
- **DBeaver-aware SQL rewrite strategy** (`DBeaverSqlDialectService`,
  `AggregateQueryBuilder`, `AggregateQuery`): source-query aggregation chooses
  between an optimized direct `GROUP BY` rewrite of the original `FROM` clause
  and a derived-table fallback (`SELECT ... FROM (original query) rv_source`).
  The decision walks the SQL text tracking parenthesis depth and single-quoted
  string literals to find a genuinely top-level `FROM`/disqualifying keyword,
  rather than relying on a plain regex split, so CTEs, joins, set operations,
  and structurally ambiguous queries fall back safely. Identifier quoting is
  derived from `SQLDialect.getIdentifierQuoteStrings()` on the active
  datasource's dialect — the same declarative quote-pair table DBeaver's own
  dialect implementations expose — via `DBeaverSqlDialectService.QuoteStyle`,
  which stores an independent open/close pair rather than assuming a single
  symmetric quote character. This correctly supports asymmetric dialects (SQL
  Server's `[` / `]` brackets) as well as symmetric ones (ANSI `"`, MySQL
  `` ` ``), and falls back to ANSI double quotes only when no dialect/datasource
  is available. Embedded quote characters are escaped by doubling the closing
  quote character, which is correct for both symmetric quoting and T-SQL's own
  bracket-escaping convention (`]` → `]]`).
  - Earlier iterations of this fix attempted to *detect* the active dialect's
    quote character by calling `dialect.getQuotedIdentifier("sample", false,
    false)` and inspecting whether the result changed length. This was a
    functional no-op: DBeaver's `AbstractSQLDialect.getQuotedIdentifier` only
    quotes when `mustBeQuoted(...)` or `forceQuotes` is true, and a benign
    lowercase non-keyword sample is never considered quote-worthy for any
    dialect, so the detection always silently produced the ANSI fallback
    regardless of the real active dialect. The current implementation reads
    `getIdentifierQuoteStrings()` directly instead of round-tripping through a
    quoting-necessity heuristic, which avoids this failure mode entirely.

- **Saved visualization presets** (`VisualizerPreset`, `VisualizerPresetStore`):
  a named chart/matrix layout is persisted via Eclipse `InstanceScope`
  preferences, keyed by a source signature (result source name plus column
  name/type shape) rather than any live DBeaver object reference, so loading a
  preset against a changed/incompatible schema is a safe no-op rather than a
  silent misapplication of stale field indexes.

Build reproducibility: DBeaver does not publish a version-pinned public p2
repository for the CE product (only the floating
`https://dbeaver.io/update/ce/latest/` alias exists; the version-pinned
`repo.dbeaver.net/p2/ce/<version>/` repositories are a separate third-party
dependency ("deps") set and do not contain `org.jkiss.dbeaver.model`). As a
practical substitute, `.github/workflows/build.yml` and `release.yml` resolve
and record the exact DBeaver core version each build actually validated
against (via the `dbeaver-core-version` artifact and, for tagged releases,
the release notes), so historical builds remain traceable to a specific
DBeaver version even though the update site itself floats.

## Large-result and runtime validation

- **Automated large-row validation**: `AggregateQueryBuilderTest#buildsChartDatasetsWithoutExceptionsAtLargeRowCounts`
  builds synthetic `ResultSetSnapshot` data at 10,000, 50,000, and 100,000 rows
  and asserts `ChartDataBuilder.build` completes without throwing and produces
  a non-empty, correctly-shaped dataset at each size. This is a real,
  automated, unit-level regression guard against pathological slowdowns or
  exceptions at scale in the in-memory charting path.
- **Dialect-quoting validation**: `DBeaverSqlDialectServiceTest` exercises
  `QuoteStyle` derivation and escaping against literal quote-string tables
  copied from real DBeaver dialect source (`MySQLDialect.MYSQL_QUOTE_STRINGS`,
  `SQLServerDialectBase.SQLSERVER_QUOTE_STRINGS`), covering symmetric and
  asymmetric quote pairs, embedded-quote escaping, and fallback behavior when
  no dialect/quote-strings table is available, plus end-to-end direct-rewrite
  and derived-table SQL generation with a non-default quote style installed.
- **No live DBeaver runtime validation has been performed in this development
  environment.** There is no DBeaver desktop installation or live datasource
  connection available here, so the large-row and quoting fixes above have
  only been exercised through synthetic in-memory data and unit tests — not
  against an actual running DBeaver instance, a real JDBC datasource, or a
  live SQL Server/MySQL/PostgreSQL connection. Anyone installing this update
  site into a real DBeaver instance should still perform the following manual
  checks before relying on this build broadly:
  1. Connect to a SQL Server (or other bracket-quoting) datasource, open a
     result set with a column name that needs quoting (e.g. containing a
     space or a reserved word), enable an aggregation/Source Query grouping
     on that column, and confirm the generated SQL preview uses `[` / `]`
     brackets rather than ANSI double quotes.
  2. Repeat against a MySQL/MariaDB datasource and confirm backtick quoting.
  3. Run an aggregate/grouping query against a real result set with roughly
     10k–100k rows (a real backing table, not synthetic data) and confirm the
     visualizer builds its chart/matrix without noticeable UI hang or memory
     pressure, respecting the connection's configured result-set row limit.
  4. Open several different result panels/Grouping panels in the same
     DBeaver session, switch between them repeatedly, and confirm each
     retains its own visualization state (this exercises the LRU session
     cache under real controller lifecycle events rather than only the unit
     test's synthetic session identities).
  5. Close a result tab/editor and confirm the corresponding session is
     reclaimed (either immediately via `partClosed`, or eventually via LRU
     eviction) without a memory leak growing unbounded across many
     open/close cycles — there is no dedicated per-controller disposal
     callback in the DBeaver API surface used here, so this is a bounded-cache
     mitigation rather than a deterministic guarantee, and is worth confirming
     under real, sustained use.

