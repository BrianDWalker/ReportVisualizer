# Part 2 verification

## Automated/build checks

Run:

```sh
JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" \
  ./mvnw clean verify
```

Expected result: all five reactor projects report `SUCCESS` and all five tests
pass. The tests cover immutable snapshot boundaries and JDBC/DBeaver type
normalization, including drivers that report semantic date-time or boolean
columns through generic JDBC codes.

## Manual DBeaver acceptance test

1. Install the generated local p2 repository into a writable DBeaver copy.
2. Open **Results Visualizer** before executing SQL and confirm it displays
   `No active result set available.`
3. Execute a query and confirm the active result's field names, labels, types,
   database types, and nullability appear.
4. Rerun with different columns and confirm the view updates automatically.
5. Execute a statement in a new result tab, switch between result tabs, and
   confirm the fields follow the selected tab.
6. Confirm **Window > Visualize Results**, the toolbar button, and the dockable
   view still work.
7. Review the DBeaver Error Log for Results Visualizer errors.

## Verified on 2026-08-13

Part 2 was built and manually tested against a writable copy of DBeaver
Community 26.1.4 on macOS/aarch64. `DBEAVER_DATA` and `DBEAVER_WORKSPACE`
pointed to an isolated test directory, so the normal DBeaver workspace and the
application under `/Applications` were not modified.

Queries against DBeaver's isolated SQLite sample database exercised customer
and invoice results. The invoice query included INTEGER, DECIMAL, and declared
DATETIME columns.

| Acceptance criterion | Result |
| --- | --- |
| Clean Tycho reactor build | Pass — all five modules |
| Automated tests | Pass — 5 tests, 0 failures/errors |
| Active result detection | Pass — customer and invoice results detected |
| Column names and metadata | Pass — names, labels, types, DB types, nullability |
| Type normalization | Pass — INTEGER, STRING, DECIMAL, and DATETIME observed |
| Query rerun refresh | Pass — customer fields replaced by invoice fields |
| Active result-tab switch | Pass — switched between `Invoice 1` and `Customer 2` |
| No active result | Pass — graceful empty state after isolated restart |
| Part 1 behavior | Pass — command, toolbar button, and dockable view retained |
| External runtime service | Pass — none used or required |
