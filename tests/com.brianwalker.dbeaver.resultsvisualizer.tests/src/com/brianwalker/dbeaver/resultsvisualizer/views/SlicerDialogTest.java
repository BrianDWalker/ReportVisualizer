/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.brianwalker.dbeaver.resultsvisualizer.model.NormalizedDataType;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerOperator;
import com.brianwalker.dbeaver.resultsvisualizer.visualization.SlicerDefinition;
import org.junit.Test;

/** Guards the TitleAreaDialog initialization order exercised by live DBeaver. */
public class SlicerDialogTest {
    @Test
    public void ignoresLayoutBeforeDialogFormIsAssigned() {
        SlicerDialog.layoutIfReady(null);
    }

    @Test public void keepsSelectValuesForNumericAndDateFields() {
        assertEquals(SlicerOperator.IN, SlicerDialog.operatorsFor(NormalizedDataType.NUMBER).get(0));
        assertTrue(SlicerDialog.operatorsFor(NormalizedDataType.NUMBER).contains(SlicerOperator.BETWEEN));
        assertEquals(SlicerOperator.IN, SlicerDialog.operatorsFor(NormalizedDataType.DATE).get(0));
        assertTrue(SlicerDialog.operatorsFor(NormalizedDataType.DATE).contains(SlicerOperator.LAST_N_DAYS));
    }

    @Test public void presentsSqlStyleNotEqualsLabel() {
        assertEquals("<>", SlicerOperator.NOT_EQUALS.toString());
    }

    @Test public void findsAnExistingSlicerForCaseInsensitiveEditing() {
        SlicerDefinition saved = SlicerDefinition.predicate("InvoiceDate",
                SlicerOperator.BETWEEN, "2026-01-01", "2026-12-31");
        assertEquals(saved, SlicerDialog.existingFor(java.util.List.of(saved), "invoicedate"));
    }
}
