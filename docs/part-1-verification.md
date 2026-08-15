# Part 1 verification

## Automated/build checks

Run:

```sh
JAVA_HOME="/Applications/DBeaver.app/Contents/Eclipse/jre/Contents/Home" \
  ./mvnw clean verify
```

Expected result: all four reactor projects report `SUCCESS`, producing the
bundle JAR, feature JAR, and p2 update-site metadata.

Inspect the repository content if needed:

```sh
find releng/com.brianwalker.dbeaver.resultsvisualizer.repository/target/repository \
  -maxdepth 2 -type f | sort
```

## Manual DBeaver smoke test

1. Install the generated local repository using the procedure in `README.md`.
2. Restart DBeaver and confirm it starts normally.
3. Confirm **Window > Visualize Results** exists.
4. Confirm the toolbar contains **Visualize Results**.
5. Invoke either contribution.
6. Confirm a dockable **Results Visualizer** view opens.
7. Confirm it shows `No result set selected.`
8. Open **Help > Installation Details > Plug-ins** and confirm bundle
   `com.brianwalker.dbeaver.resultsvisualizer` is present at version `1.0.0`.
9. Review the DBeaver Error Log and confirm there is no Results Visualizer
   startup error.

The smoke test does not execute SQL, access a database, or call an external
runtime service.

## Verified on 2026-08-13

Part 1 was built and smoke-tested against a writable copy of DBeaver Community
26.1.2 on macOS/aarch64. The test used a separate Eclipse workspace and the
feature was installed from the generated local p2 repository.

Results:

| Acceptance criterion | Result |
| --- | --- |
| Tycho reactor builds | Pass — parent, bundle, feature, and repository |
| Feature installs | Pass — p2 Director installed feature `1.0.0` |
| DBeaver starts | Pass — workbench opened normally |
| Toolbar command exists | Pass — **Open Results Visualizer** button visible |
| Menu command exists | Pass — **Window > Visualize Results** visible |
| Dockable view opens | Pass — **Results Visualizer** opened in the view stack |
| Placeholder appears | Pass — `No result set selected.` visible |
| Error isolation | Pass — no Results Visualizer error in the Eclipse log |
| External runtime service | Pass — none used or required by the plug-in |
