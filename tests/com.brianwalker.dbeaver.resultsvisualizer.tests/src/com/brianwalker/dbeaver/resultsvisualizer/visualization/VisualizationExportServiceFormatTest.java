/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.visualization.VisualizationExportService.ExportFormat;
import org.junit.Test;

/** Verifies the Export dropdown's format metadata (extension, filters, default file name). */
public class VisualizationExportServiceFormatTest {

    @Test
    public void everyFormatHasAUniqueLowercaseExtensionAndMatchingDefaultFileName() {
        for (ExportFormat format : ExportFormat.values()) {
            assertTrue(format + " extension should be lowercase and non-empty",
                    !format.extension().isBlank() && format.extension().equals(format.extension().toLowerCase()));
            assertTrue(format + " default file name should end with its extension",
                    format.defaultFileName().endsWith("." + format.extension()));
            assertTrue(format + " should declare at least one filter extension",
                    format.filterExtensions().length > 0);
        }
    }

    @Test
    public void filterExtensionsArrayIsDefensivelyCopied() {
        ExportFormat format = ExportFormat.PNG;
        String[] first = format.filterExtensions();
        first[0] = "*.mutated";
        assertEquals("*.png", format.filterExtensions()[0]);
    }

    @Test
    public void allFourRequiredFormatsArePresent() {
        assertEquals(4, ExportFormat.values().length);
        assertTrue(contains(ExportFormat.PNG));
        assertTrue(contains(ExportFormat.JPEG));
        assertTrue(contains(ExportFormat.SVG));
        assertTrue(contains(ExportFormat.PDF));
    }

    private static boolean contains(ExportFormat format) {
        for (ExportFormat value : ExportFormat.values()) {
            if (value == format) return true;
        }
        return false;
    }
}
