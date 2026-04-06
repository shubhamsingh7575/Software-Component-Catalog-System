package edu.software.project.frontend;

import edu.software.project.frontend.ui.AppFrame;
import edu.software.project.frontend.ui.ModernTheme;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ModernTheme.install();
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}

