# Part 3 verification

Verified on August 13, 2026 with DBeaver Community 26.1.4 on macOS.

## Automated verification

The project was built with DBeaver's bundled Java runtime:

```sh
JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" \
  ./mvnw -DskipTests=false clean verify
```

All five Tycho reactor modules built successfully. The test fragment ran nine
tests with no failures or errors. Part 3 tests cover default category/measure
selection, chart dataset construction, null-measure handling, numeric scatter
X values, immutable SQL-null preservation, and the Part 2 type normalization.

## DBeaver acceptance verification

The generated p2 repository was installed in an isolated writable copy of
DBeaver. The Results Visualizer opened as a dockable workbench view and first
showed the no-active-result state.

The following query was run against DBeaver's sample SQLite database:

```sql
SELECT BillingCountry AS category,
       ROUND(SUM(Total), 2) AS revenue,
       COUNT(*) AS invoice_count
FROM Invoice
GROUP BY BillingCountry
ORDER BY revenue DESC
LIMIT 8;
```

Observed results:

- The view refreshed to three fields and eight rows without a manual copy step.
- `category [STRING]` was selected as X and `revenue [NUMBER]` as Y.
- A Bar chart rendered the eight grouped countries and their revenue values.
- The Fields panel showed normalized and database types plus nullability.
- The Chart Configuration panel exposed Bar, Line, and Scatter through the
  renderer registry, with X and Y selectors.
- The toolbar command, menu command, dockable view, no-result state, and Part 2
  active-result behavior remained available.

The rendering path uses only local SWT drawing and does not start or contact an
external service.
