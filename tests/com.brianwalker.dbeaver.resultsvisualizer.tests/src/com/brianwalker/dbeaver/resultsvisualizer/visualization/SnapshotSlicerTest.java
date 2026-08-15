/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.model.Nullability;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultColumn;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultRow;
import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class SnapshotSlicerTest {
    @Test public void combinesDistinctValueSlicersWithoutMutatingSource() {
        ResultSetSnapshot source = new ResultSetSnapshot("test", List.of(
                new ResultColumn(0, "region", "region", Types.VARCHAR, "VARCHAR",
                        NormalizedDataType.STRING, Nullability.NULLABLE),
                new ResultColumn(1, "year", "year", Types.INTEGER, "INTEGER",
                        NormalizedDataType.INTEGER, Nullability.NULLABLE)),
                List.of(new ResultRow(1, List.of("East", 2025)),
                        new ResultRow(2, List.of("East", 2026)),
                        new ResultRow(3, List.of("West", 2026))), 3, false, Instant.now());

        ResultSetSnapshot filtered = SnapshotSlicer.apply(source, List.of(
                new SlicerDefinition("region", Set.of("East")),
                new SlicerDefinition("year", Set.of("2026"))));

        assertEquals(1, filtered.rows().size());
        assertEquals(3, source.rows().size());
    }
}
