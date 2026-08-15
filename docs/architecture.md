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

`ChartDataBuilder` performs SUM, AVG, MIN, MAX, COUNT, and COUNT DISTINCT over the
plug-in-owned snapshot. A stable insertion-ordered group key consists of the X
value and optional Series value. Aggregation therefore never modifies or reruns
SQL. `ChartPoint` carries a series name, and `ChartDataset` exposes categories,
series names, and per-series points. Each renderer consumes those helpers to
draw grouped bars or separately colored line/scatter series plus a local SWT
legend.

## Part 5 calculated fields

`CalculatedFieldService` projects user-defined calculated columns onto the
immutable `ResultSetSnapshot`. A projection appends `CALCULATED` numeric columns
and produces a new snapshot; it never writes through to DBeaver, changes the SQL
text, or reruns the query. Definitions remain owned by the visualizer view and
are reapplied when compatible result rows refresh.

`ExpressionCompiler` is a purpose-built recursive-descent parser. Its grammar is
limited to case-insensitive `[field]` references, numeric constants, arithmetic
operators, parentheses, unary signs, and the numeric functions `ABS`, `ROUND`,
`CEIL`, `FLOOR`, `SQRT`, `POWER`, `MIN`, and `MAX`. It has no Java, JavaScript,
shell, SQL, reflection, filesystem, or
process execution facility. Unsupported tokens and function names fail during
validation before a definition is accepted.

Compiled expressions evaluate one immutable row at a time. Null, boolean,
temporal, incompatible, divide-by-zero, and non-finite inputs produce a null
calculated value for that row. One invalid definition is reported separately
and does not prevent valid definitions from being projected. Definitions are
compiled and resolved against column indexes once per projection rather than
parsing an expression for every row.

`CalculatedFieldDialog` provides the Name and Expression editor, a field/type
list with double-click insertion, a formula guide, practical examples, and
Create/Cancel actions. After creation, the new column uses the same field table,
dropdown selectors, aggregation pipeline, and chart renderers as database fields.

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
