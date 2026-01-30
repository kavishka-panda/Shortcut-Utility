package com.myhotkey.shortcututitlity;

import javafx.application.Application;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.net.ServerSocket;

public class Launcher {
    private static final int PORT = 58432;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.println("KeyFlow Utility is already running.");
            System.exit(0); 
            return;
        }

        System.setProperty("jnativehook.lib.path", System.getProperty("java.io.tmpdir"));
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

        Application.launch(MainApp.class, args);
    }
}