package com.myhotkey.shortcututitlity;

import javafx.application.Application;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Launcher {
    public static void main(String[] args) {
        // 1. Configure Swing/FlatLaf on the Event Dispatch Thread
        // We do this BEFORE JavaFX starts to ensure the TrayIcon 
        // and any Swing dialogs look correct immediately.
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                } catch (Exception e) {
                    System.err.println("FlatLaf failed to initialize.");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Launch JavaFX
        Application.launch(MainApp.class, args);
    }
}