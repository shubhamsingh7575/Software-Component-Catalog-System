package edu.software.project.frontend.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;

public final class ModernTheme {
    private ModernTheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) {
        }

        FontUIResource baseFont = new FontUIResource(new Font("Dialog", Font.PLAIN, 14));
        FontUIResource titleFont = new FontUIResource(new Font("Dialog", Font.BOLD, 14));
        ColorUIResource page = new ColorUIResource(0xF6F7F9);
        ColorUIResource panel = new ColorUIResource(0xFFFFFF);
        ColorUIResource panelSoft = new ColorUIResource(0xF0F4F8);
        ColorUIResource ink = new ColorUIResource(0x1F2937);
        ColorUIResource muted = new ColorUIResource(0x6B7280);
        ColorUIResource accent = new ColorUIResource(0x2563EB);
        ColorUIResource accentSoft = new ColorUIResource(0xBFDBFE);
        ColorUIResource border = new ColorUIResource(0xCBD5E1);
        BorderUIResource lineBorder = new BorderUIResource(BorderFactory.createLineBorder(border, 1, true));

        UIManager.put("control", page);
        UIManager.put("info", panel);
        UIManager.put("nimbusBase", accent);
        UIManager.put("text", ink);
        UIManager.put("window", page);
        UIManager.put("Panel.background", page);
        UIManager.put("Viewport.background", panel);
        UIManager.put("TabbedPane.background", page);
        UIManager.put("TabbedPane.selected", panel);
        UIManager.put("TabbedPane.foreground", ink);
        UIManager.put("TabbedPane.contentAreaColor", page);
        UIManager.put("TabbedPane.focus", accent);
        UIManager.put("Label.font", baseFont);
        UIManager.put("Label.foreground", ink);
        UIManager.put("Button.font", titleFont);
        UIManager.put("Button.background", panel);
        UIManager.put("Button.foreground", ink);
        UIManager.put("Button.select", panelSoft);
        UIManager.put("Button.border", lineBorder);
        UIManager.put("Button.margin", new InsetsUIResource(10, 18, 10, 18));
        UIManager.put("TextField.font", baseFont);
        UIManager.put("PasswordField.font", baseFont);
        UIManager.put("TextArea.font", baseFont);
        UIManager.put("Table.font", baseFont);
        UIManager.put("TableHeader.font", titleFont);
        UIManager.put("TabbedPane.font", titleFont);
        UIManager.put("TextField.background", panel);
        UIManager.put("TextField.foreground", ink);
        UIManager.put("TextField.caretForeground", ink);
        UIManager.put("TextField.inactiveForeground", muted);
        UIManager.put("TextField.selectionBackground", accentSoft);
        UIManager.put("TextField.selectionForeground", ink);
        UIManager.put("TextField.border", lineBorder);
        UIManager.put("PasswordField.background", panel);
        UIManager.put("PasswordField.foreground", ink);
        UIManager.put("PasswordField.caretForeground", ink);
        UIManager.put("PasswordField.inactiveForeground", muted);
        UIManager.put("PasswordField.selectionBackground", accentSoft);
        UIManager.put("PasswordField.selectionForeground", ink);
        UIManager.put("PasswordField.border", lineBorder);
        UIManager.put("TextArea.background", panel);
        UIManager.put("TextArea.foreground", ink);
        UIManager.put("TextArea.caretForeground", ink);
        UIManager.put("TextArea.selectionBackground", accentSoft);
        UIManager.put("TextArea.selectionForeground", ink);
        UIManager.put("ComboBox.background", panel);
        UIManager.put("ComboBox.foreground", ink);
        UIManager.put("ComboBox.selectionBackground", accentSoft);
        UIManager.put("ComboBox.selectionForeground", ink);
        UIManager.put("ComboBox.buttonBackground", panelSoft);
        UIManager.put("ComboBox.buttonDarkShadow", border);
        UIManager.put("ComboBox.buttonHighlight", panel);
        UIManager.put("Table.background", panel);
        UIManager.put("Table.foreground", ink);
        UIManager.put("Table.selectionBackground", accentSoft);
        UIManager.put("Table.selectionForeground", ink);
        UIManager.put("Table.gridColor", border);
        UIManager.put("TableHeader.background", panelSoft);
        UIManager.put("TableHeader.foreground", ink);
        UIManager.put("TabbedPane.contentBorderInsets", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    }
}
