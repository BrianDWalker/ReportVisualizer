/*
 * Copyright (c) 2026 Brian Walker.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.brianwalker.dbeaver.resultsvisualizer.visualization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public class SlicerValueTest {

    @Test
    public void distinguishesNullFromLiteralNullText() {
        SlicerDefinition slicer = SlicerDefinition.typed("status",
                Set.of(SlicerValue.nullValue(), SlicerValue.fromDisplayValue("(null)")));

        assertEquals(2, slicer.selectedValues().size());
        assertTrue(slicer.selectedValues().stream().anyMatch(SlicerValue::sqlNull));
        assertTrue(slicer.selectedValues().stream().anyMatch(value -> !value.sqlNull() && "(null)".equals(value.displayValue())));
    }

    @Test
    public void comparesEquivalentNumericValues() {
        SlicerValue one = SlicerValue.fromValue(1);
        SlicerValue onePointZero = SlicerValue.fromValue(1.0);

        assertTrue(one.matches(1.0));
        assertTrue(onePointZero.matches(1));
        assertTrue(one.sqlLiteral().equals("1"));
    }
}
