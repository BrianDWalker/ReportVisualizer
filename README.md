# Results Visualizer for DBeaver

Public Eclipse p2 update site for **Results Visualizer**, a DBeaver Community
plug-in for charts, matrices, formulas, slicers, sorting, and aggregate source
queries.

Current version: `1.0.0.202608141800`

## Install directly in DBeaver

1. Open **Help > Install New Software**.
2. Select **Add**.
3. Enter `Results Visualizer` as the name.
4. Enter this update-site URL:

   ```text
   https://briandwalker.github.io/ReportVisualizer/
   ```

5. Select **Results Visualizer for DBeaver** and complete the wizard.
6. Review and accept the EPL-2.0 license and unsigned-content prompt.
7. Restart DBeaver.
8. Open **Window > Show View > Other > Results Visualizer > Results Visualizer**.

## Manual download

If DBeaver cannot reach the update site directly but a browser can, download
[Results Visualizer 1.0.0.202608141800](https://briandwalker.github.io/ReportVisualizer/downloads/Results-Visualizer-1.0.0.202608141800.zip),
extract it, and select the extracted update-site folder using **Help > Install
New Software > Add > Local**.

The repository root is a composite p2 site. `current/` contains the latest
release, while `history/` retains earlier artifacts so cached DBeaver updates
remain installable.
