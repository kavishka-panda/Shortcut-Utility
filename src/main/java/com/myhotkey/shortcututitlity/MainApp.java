package com.myhotkey.shortcututitlity;

import com.myhotkey.shortcututitlity.model.Shortcut;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.List;

import javax.swing.*;

import atlantafx.base.theme.PrimerDark;

public class MainApp extends Application {
    private Stage stage;
    private GlobalHotkeyService hotkeyService;

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.stage = stage;
        Platform.setImplicitExit(false);

        // 1. Initialize Storage and Load Shortcuts
        JsonManager jsonManager = new JsonManager();
        List<Shortcut> sharedShortcutList = jsonManager.loadShortcuts();

        // 2. Setup the Background Service with the shared list
        hotkeyService = new GlobalHotkeyService();
        hotkeyService.setShortcuts(sharedShortcutList);

        // Start service in a background thread
        Thread hookThread = new Thread(() -> hotkeyService.startHook());
        hookThread.setDaemon(true);
        hookThread.start();

        // 3. Load UI and pass the shared list to the Controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setDependencies(sharedShortcutList, jsonManager, hotkeyService);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("KeyFlow Utility");

        // Use fully qualified name to avoid ambiguity with java.awt.Image
        try {
            javafx.scene.image.Image appIcon = new javafx.scene.image.Image(getClass().getResourceAsStream("icon.png"));
            stage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Could not load window icon: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.show();

        createTrayIcon(stage);

        stage.setOnCloseRequest(event -> {
            event.consume(); // Prevent the actual closing
            stage.hide(); // Just hide the window
        });
    }

    private void createTrayIcon(Stage stage) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // Use fully qualified name or specific AWT Image
            java.awt.Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png"));

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Open KeyFlow");
            showItem.addActionListener(e -> Platform.runLater(stage::show));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.exit(); // This will trigger the stop() method for cleanup
                System.exit(0);
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "KeyFlow Utility", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(stage::show));

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (hotkeyService != null) {
            hotkeyService.unregisterService();
        }
        super.stop();
    }

    public static void main(String[] args) {
        // FlatLaf must be initialized before launch for Swing components
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } catch (Exception e) {
                System.err.println("FlatLaf failed to initialize.");
            }
        });

        launch(args);
    }
}