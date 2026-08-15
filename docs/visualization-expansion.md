# Visualization expansion

Implemented on August 13, 2026.

## Local visualization

The visualizer now provides Bar, Horizontal Bar, Stacked Bar, Line, Area,
Stacked Area, Scatter, Pie, Donut, Heatmap, and Matrix/Pivot. Matrix and Heatmap
use X/Rows, Series/Columns, Values, and the selected aggregation. Rows and
Columns each accept multiple fields, represented as ordered pivot hierarchies.
They include row totals, column totals, a grand total, scrolling, and a
2,500-cell safety guard.

The earlier Fields table is removed. Field assignment is dropdown-only and
dropdown items use plain display names without type markers.

## Slicers

Each slicer selects one or more distinct values from a field in the immutable
current Results or Grouping snapshot. Multiple slicers combine with AND logic
before chart aggregation. The source result remains unchanged. A slicer can
also generate a full-source `SELECT DISTINCT` query for review in DBeaver.

## Custom SQL and source-query aggregation

Custom SQL fields are intentionally separate from safe local calculated fields.
They accept any single database expression as either a reusable Field or an
Aggregation. Existing fields can be double-clicked into the SQL editor. Statement
terminators and SQL comments are rejected so the input remains one expression.

The unified Source Query builder has three sections: multi-select Available
Fields, Custom SQL Fields, and multiple named Aggregations. Custom SQL fields
are reusable expressions without a separate type selector and can be grouped or
aggregated. Local calculated fields are expanded to their source expressions,
so a formula such as `[revenue] - [cost]` becomes valid database SQL instead of
referencing a result-only alias.

**Source Query** wraps the original SQL as `rv_source`, applies the selected
aggregations, custom dimensions, slicer predicates, and sorts, and shows the
complete SQL. Choosing **Execute** runs the read query through the current
visualization and restores the returned row/column/value aliases without opening
another SQL editor. DBeaver's client fetch limit does not limit the database-side
input to the aggregate, although an explicit `LIMIT` inside the original SQL
remains in effect.

Aggregation options include SUM, AVG, MIN, MAX, COUNT, and COUNT DISTINCT.

## DBeaver registration

The direct Window menu command is removed. The view is registered under
**Window > Show View > Other > Results Visualizer > Results Visualizer**. The
toolbar shortcut remains available.
