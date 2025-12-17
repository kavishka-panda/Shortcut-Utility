package com.myhotkey.shortcututitlity;

import javafx.fxml.FXML;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

public class MainController {
    @FXML
    private TableView<?> tableView;

    @FXML
    private VBox shortcutListContainer;

    @FXML
    private void handleDeleteShortcut() {
        System.out.println("Delete Shortcut");
    }

    @FXML
    private void addShortcutToUI(String keys, String functionName) {
        HBox card = new HBox(20);
        card.getStyleClass().add("shortcut-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Keyboard Visuals
        HBox keysBox = new HBox(5);
        keysBox.setAlignment(Pos.CENTER);
        for (String key : keys.split("\\+")) {
            Label k = new Label(key.trim());
            k.setStyle(
                    "-fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            keysBox.getChildren().add(k);
        }

        VBox details = new VBox(2);
        Label title = new Label(functionName);
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label subtitle = new Label("System automated trigger");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        details.getChildren().addAll(title, subtitle);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> shortcutListContainer.getChildren().remove(card));

        card.getChildren().addAll(keysBox, details, deleteBtn);
        shortcutListContainer.getChildren().add(card);
    }

    @FXML
    private javafx.scene.control.TextField shortcutInputField;

    private final java.util.Set<javafx.scene.input.KeyCode> activeKeys = new java.util.LinkedHashSet<>();

    @FXML
    private void initialize() {
        if (shortcutInputField != null) {
            shortcutInputField.setOnKeyPressed(event -> {
                activeKeys.add(event.getCode());
                updateShortcutText();
                event.consume();
            });

            shortcutInputField.setOnKeyReleased(event -> {
                activeKeys.remove(event.getCode());
                event.consume();
            });

        }
    }

    private void updateShortcutText() {
        String shortcut = activeKeys.stream()
                .map(javafx.scene.input.KeyCode::getName)
                .collect(java.util.stream.Collectors.joining(" + "));
        shortcutInputField.setText(shortcut);
    }

    
    @FXML
    private void handleAddShortcut() {
        if(activeKeys != null && !activeKeys.isEmpty()){
            
        }
    }

}
