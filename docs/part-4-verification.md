# Part 4 verification

Verified on August 13, 2026 with DBeaver Community 26.1.4 on macOS.

## Automated verification

```sh
JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" \
  ./mvnw -DskipTests=false clean verify
```

All five Tycho reactor modules built successfully. Twelve tests completed with
no failures or errors. The Part 4 coverage verifies all five aggregations,
repeated-category grouping, independent series construction, empty/reset state,
and the existing Parts 2 and 3 snapshot, type, and chart-data behavior.

## DBeaver acceptance verification

The p2 repository was installed into an isolated writable DBeaver copy. The
interactive builder was exercised using 100 raw invoice rows from the sample
SQLite database:

```sql
SELECT BillingCountry AS category,
       CASE WHEN CustomerId % 2 = 0
            THEN 'Even customers' ELSE 'Odd customers' END AS region,
       Total AS revenue
FROM Invoice
ORDER BY BillingCountry
LIMIT 100;
```

Observed behavior:

- The fields list displayed `ABC` for category/region and `123` for revenue.
- X Axis and Values defaulted to category and revenue.
- SUM(revenue) grouped the 100 raw rows locally by country.
- X Axis, Values, and Series were visible drop-enabled wells with direct-select
  alternatives; the fields table supplied right-click assignment actions.
- The aggregation selector exposed SUM, AVG, MIN, MAX, and COUNT.
- Bar, Line, and Scatter remained selectable while reusing compatible fields.
- Series-aware datasets and renderers produced separate colors and a legend.
- Reset Visualization cleared all wells and displayed the builder prompt without
  changing the SQL text or its 100-row result.
- Rerunning a compatible result retained configuration and rebuilt from new rows.

No external process, server, network service, or SQL rewrite is involved.
