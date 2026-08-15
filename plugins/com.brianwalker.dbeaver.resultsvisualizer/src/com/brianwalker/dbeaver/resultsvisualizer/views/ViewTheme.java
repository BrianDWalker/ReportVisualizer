/* Copyright (c) 2026 Brian Walker. SPDX-License-Identifier: EPL-2.0 */
package com.brianwalker.dbeaver.resultsvisualizer.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

/** Small theme helper that keeps custom dialog content readable in light and dark DBeaver themes. */
final class ViewTheme {
    private ViewTheme() {}

    static void improveContrast(Composite root) {
        apply(root);
    }

    static void compact(Composite root) {
        FontData[] data = root.getFont().getFontData();
        for (FontData item : data) item.setHeight(Math.max(9, item.getHeight() - 1));
        Font compact = new Font(root.getDisplay(), data);
        root.addDisposeListener(event -> compact.dispose());
        applyFont(root, compact);
    }

    static void insertPlainText(Text control, String value) {
        Point selection = control.getSelection();
        String original = control.getText();
        String updated = original.substring(0, selection.x) + value + original.substring(selection.y);
        control.setText(updated);
        int caret = selection.x + value.length();
        control.setFocus();
        control.setSelection(caret, caret);
    }

    private static void apply(Control control) {
        if (control instanceof Table || control instanceof Text || control instanceof Combo) {
            control.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
            control.setForeground(control.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
            if (control instanceof Table table) {
                for (var item : table.getItems()) item.setForeground(table.getForeground());
            }
        }
        Color background = control.getBackground();
        if (control instanceof Label && control.getParent() != null) background = control.getParent().getBackground();
        if (!(control instanceof Table || control instanceof Text || control instanceof Combo)) {
            control.setForeground(contrasting(control, background));
        }
        if (control instanceof Composite composite) {
            for (Control child : composite.getChildren()) apply(child);
        }
    }

    private static Color contrasting(Control control, Color background) {
        var rgb = background.getRGB();
        double luminance = (0.2126 * rgb.red + 0.7152 * rgb.green + 0.0722 * rgb.blue) / 255d;
        return control.getDisplay().getSystemColor(luminance < 0.48 ? SWT.COLOR_WHITE : SWT.COLOR_BLACK);
    }

    private static void applyFont(Control control, Font font) {
        if (control instanceof Button || control instanceof Label || control instanceof Combo) {
            control.setFont(font);
        }
        if (control instanceof Composite composite) {
            for (Control child : composite.getChildren()) applyFont(child, font);
        }
    }
}
