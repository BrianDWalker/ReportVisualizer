/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import org.junit.Test;

/** Guards the TitleAreaDialog initialization order exercised by live DBeaver. */
public class SlicerDialogTest {
    @Test
    public void ignoresLayoutBeforeDialogFormIsAssigned() {
        SlicerDialog.layoutIfReady(null);
    }
}
