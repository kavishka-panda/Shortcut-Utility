package com.myhotkey.shortcututitlity;

import com.myhotkey.shortcututitlity.model.Shortcut;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.List;

import javax.swing.SwingUtilities;

import atlantafx.base.theme.PrimerDark;

public class MainApp extends Application {
    private Stage stage;
    private GlobalHotkeyService hotkeyService;
    private JsonManager jsonManager;
    private List<Shortcut> sharedShortcutList;

    @Override
    public void init() throws Exception {
        // 1. Initialize Storage and Load Shortcuts
        jsonManager = new JsonManager();
        sharedShortcutList = jsonManager.loadShortcuts();

        // 2. Setup the Background Service with the shared list
        hotkeyService = new GlobalHotkeyService();
        hotkeyService.setShortcuts(sharedShortcutList);

        // Start service in a background thread after a short delay to prevent startup
        // freezes
        Thread hookThread = new Thread(() -> {
            try {
                // If started from startup, wait for the system to settle
                if (getParameters().getRaw().contains("--tray")) {
                    Thread.sleep(15000);
                }
                hotkeyService.startHook();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        hookThread.setDaemon(true);
        hookThread.start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        Platform.setImplicitExit(false);

        SwingUtilities.invokeLater(() -> createTrayIcon(stage));
        boolean startMinimized = getParameters().getRaw().contains("--tray");
        if (startMinimized) {
            System.out.println("Starting minimized");
        } else {
            stage.show();
        }

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
        if (!startMinimized) {
            stage.show();
            stage.centerOnScreen();
        }

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
            showItem.addActionListener(e -> showStage());

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
            trayIcon.addActionListener(e -> showStage());

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
    }

    private void showStage() {
        Platform.runLater(() -> {
            if (stage != null) {
                stage.show();
                stage.setIconified(false);
                stage.toFront();
                stage.requestFocus();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (hotkeyService != null) {
            hotkeyService.unregisterService();
        }
        super.stop();
    }

}