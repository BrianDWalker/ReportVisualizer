/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.calculatedfields;

import com.brianwalker.dbeaver.resultsvisualizer.model.ResultSetSnapshot;
import java.util.List;

/** Result of applying calculated definitions to a snapshot. */
public record CalculatedFieldProjection(ResultSetSnapshot snapshot, List<String> errors) {
    public CalculatedFieldProjection { errors = List.copyOf(errors); }
}
