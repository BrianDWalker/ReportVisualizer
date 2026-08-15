# Workflow refinements verification

Verified on August 13, 2026 against DBeaver Community 26.1.4 on macOS.

- The Fields table contains only Field and Type.
- Type displays DBeaver's database-reported type name.
- Drag/drop and field context-menu assignment paths are removed; X Axis,
  Values, and Series use the dropdowns.
- Snapshot size follows the rows DBeaver has already fetched under its own limit.
- Source offers Results panel and Grouping panel, with an actionable empty state
  when grouping output has not been opened.
- Auto Y scaling rounds 91 to 100, 204.96 to 250, and 0.91 to 1; Y Max accepts
  a numeric override.
- The calculated-field editor supplies formula guidance, examples, field types,
  and double-click insertion.
- Restricted expression tests cover the expanded numeric functions and retain
  the existing rejection/null-isolation behavior.

The clean Tycho reactor build completed successfully with 23 tests and no
failures or errors.

The p2 build qualifier is pinned to `202608140052`, so both repository metadata
and artifact filenames remain stable across clean rebuilds and update retries.
