/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.services;

/** Selects which DBeaver result model supplies the visualization snapshot. */
public enum ResultSource {
    RESULTS("Results panel"),
    GROUPING("Grouping panel");

    private final String displayName;

    ResultSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
