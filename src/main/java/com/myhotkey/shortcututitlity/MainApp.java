package com.myhotkey.shortcututitlity;

import com.myhotkey.shortcututitlity.model.Shortcut;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.util.List;

import javax.swing.*;


public class MainApp extends Application {
    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {

        this.stage = stage;
        Platform.setImplicitExit(false);

        // 1. Initialize Storage and Load Shortcuts
        JsonManager jsonManager = new JsonManager();
        List<Shortcut> sharedShortcutList = jsonManager.loadShortcuts();

        // 2. Setup the Background Service with the shared list
        GlobalHotkeyService hotkeyService = new GlobalHotkeyService();
        // Use a setter method instead of constructor parameter
        hotkeyService.setShortcuts(sharedShortcutList);

        // Start service in a background thread
        Thread hookThread = new Thread(() -> hotkeyService.startHook());
        hookThread.setDaemon(true);
        hookThread.start();

        // 3. Load UI and pass the shared list to the Controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        // Use the correct method name from your controller
        controller.setDependencies(sharedShortcutList, jsonManager);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("KeyFlow Utility");
        stage.setScene(scene);
        stage.show();

        createTrayIcon(stage);

        stage.setOnCloseRequest(event -> {
            event.consume(); // Prevent the actual closing
            stage.hide();    // Just hide the window
        });
    }

    private void createTrayIcon(Stage stage) {
        if(SystemTray.isSupported()){
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png"));

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Open KeyFlow");
            showItem.addActionListener(e -> Platform.runLater(stage::show));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                System.exit(0); // Fully close the app
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "KeyFlow Utility", popup);
            trayIcon.setImageAutoSize(true);

            // Double click tray to open app
            trayIcon.addActionListener(e -> Platform.runLater(stage::show));

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
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