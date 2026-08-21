# Phase 1 hardening verification

## Automated checks

Run with Java 21:

```sh
JAVA_HOME=/path/to/jdk-21 ./mvnw -B clean verify
```

The suite covers:

- independent cached sessions containing 500, 600, and 1,000 loaded rows;
- pre-aggregate configuration isolation across result identities;
- current, stale, cross-session, and old-source aggregate request correlation;
- cancellation updates carrying request context with no partial snapshot;
- SQL line/block comments, quoted comment markers, parentheses, one trailing
  semicolon, and stacked-statement rejection;
- SQLite-style `strftime(...)`, grouping, a commented-out `WHERE`, and a
  trailing semicolon without `; ) rv_source` output;
- SUM, COUNT, MIN, MAX, AVG, and COUNT DISTINCT local re-aggregation metadata.

## Live DBeaver checks

These require an installed build and several real Results/Grouping panels:

1. Focus Results A, configure every Builder control, then click those controls
   repeatedly. Confirm the session never jumps to Results B.
2. Focus Results B. Confirm one snapshot is taken, then operate Chart,
   Aggregation, Y Max, Slicers, Sort, Presets, Formulas, Source Query, Export,
   and chart options without another result-model copy.
3. Repeat with 500, 600, and 1,000 loaded rows and multiple result tabs.
4. Execute a Source Query in A, switch to B, then return to A and choose Back to
   Original. Confirm A's exact pre-aggregate configuration returns.
5. Launch two Source Queries in A and switch to B while they run. Confirm an
   older or late A result never updates B and never overwrites A's newer result.
6. Cancel a Source Query and confirm no partial aggregate is displayed.
7. For AVG or COUNT DISTINCT aggregate output, remove/change a grouping field.
   Confirm the chart explains that a new Source Query is required. Confirm
   COUNT totals, MIN, MAX, and SUM can be safely regrouped.

The live checks are intentionally listed separately; the headless Tycho suite
does not simulate DBeaver's real SWT focus ordering or JDBC cancellation UI.
