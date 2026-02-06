package com.myhotkey.shortcututitlity;

import com.myhotkey.shortcututitlity.model.Shortcut;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
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
import javafx.util.Duration;

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
        hotkeyService.setOnKeyPressedListener(this::showNotification);

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

        // In your init() or start() method
        hotkeyService.setOnKeyPressedListener(actionName -> {
            String displayMessage;
            String icon;

            switch (actionName) {
                case "VOLUME_UP":
                    displayMessage = "Volume Up";
                    icon = "🔊";
                    break;
                case "VOLUME_DOWN":
                    displayMessage = "Volume Down";
                    icon = "🔉";
                    break;
                case "MUTE":
                    displayMessage = "Mute";
                    icon = "🔇";
                    break;
                case "BRIGHTNESS_UP":
                    displayMessage = "Brightness Up";
                    icon = "☀️";
                    break;
                case "BRIGHTNESS_DOWN":
                    displayMessage = "Brightness Down";
                    icon = "🌙";
                    break;
                default:
                    displayMessage = actionName.replace("_", " ");
                    icon = "✨";
                    break;
            }

            showNotification(displayMessage + " " + icon);
        });
    }

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        this.stage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        Platform.setImplicitExit(false);

        // Check for updates when the application starts
        UpdateChecker updateChecker = new UpdateChecker();
        updateChecker.checkForUpdates();

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

    public void showNotification(String functionName) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);

            // Styling the notification box
            Label label = new Label(functionName);
            label.setStyle("-fx-background-color: rgba(40, 40, 40, 0.9); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 12 20; " +
                    "-fx-background-radius: 10; " +
                    "-fx-font-family: 'Segoe UI', sans-serif; " +
                    "-fx-font-size: 14px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

            StackPane root = new StackPane(label);
            root.setStyle("-fx-background-color: transparent;");

            Scene scene = new Scene(root);
            scene.setFill(null);
            stage.setScene(scene);

            // Calculate Bottom-Right Position
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // We must show the stage to calculate its actual width/height
            stage.show();

            double x = screenBounds.getMaxX() - stage.getWidth() - 30; // 30px margin from right
            double y = screenBounds.getMaxY() - stage.getHeight() - 30; // 30px margin from bottom

            stage.setX(x);
            stage.setY(y);

            // Stay visible for 1.5 seconds, then close
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> stage.close());
            delay.play();
        });
    }

}