# Part 5 verification

Verified on August 13, 2026 with DBeaver Community 26.1.4 on macOS.

## Automated verification

```sh
JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" \
  ./mvnw -DskipTests=false clean verify
```

All five Tycho reactor modules built successfully. Twenty-one tests completed
with no failures or errors. Part 5 coverage verifies precedence, parentheses,
unary signs, `ABS`, `ROUND`, unknown and incomplete field references, restricted
grammar rejection, null and incompatible values, divide-by-zero, projection,
chained definitions, refreshed rows, and isolation of an invalid definition.
The existing Parts 2 through 4 tests remain green.

## DBeaver acceptance verification

The generated p2 repository was installed into an isolated writable DBeaver
copy. The Results Visualizer was exercised using 100 rows from the sample SQLite
database:

```sql
SELECT BillingCountry AS category,
       Total AS revenue,
       Total * 0.60 AS cost
FROM Invoice
ORDER BY BillingCountry
LIMIT 100;
```

Observed behavior:

- The view displayed category, revenue, and cost from the active 100-row result.
- **+ Calculated Field** opens a Name/Expression editor with Create and Cancel.
- A `Profit` definition using `[revenue] - [cost]` is validated and evaluated
  locally against the current snapshot.
- The calculated numeric column appears beside database fields and is available
  to the Values well while category remains assigned to X Axis.
- The existing local SUM aggregation and chart renderer consume projected
  Profit values without special-case chart code.
- The SQL text and underlying DBeaver result are not modified or rerun by
  calculated-field creation.
- Invalid grammar and unknown fields are rejected before creation; per-row null,
  incompatible, divide-by-zero, and non-finite results remain null and do not
  interrupt other rows.

The runtime uses no script engine, reflection bridge, shell, SQL evaluator,
filesystem access, process launch, server, or network service for expressions.
