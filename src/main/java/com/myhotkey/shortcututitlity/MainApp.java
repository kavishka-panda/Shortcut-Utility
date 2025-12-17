package com.myhotkey.shortcututitlity;

import com.formdev.flatlaf.FlatDarkLaf;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.*;


public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Scene scene = new Scene(loader.load());

        // CSS is the most important part for JavaFX "Theme"
        String css = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("KeyFlow Utility");
        stage.setScene(scene);
        stage.show();

        // Start Hook
        new Thread(() -> {
            GlobalHotkeyService service = new GlobalHotkeyService();
            service.startHook();
        }).start();
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