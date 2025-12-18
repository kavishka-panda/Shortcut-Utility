package com.myhotkey.shortcututitlity;

import javafx.fxml.FXML;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import com.myhotkey.shortcututitlity.enums.SystemAction;
import javafx.scene.input.KeyCode;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import com.myhotkey.shortcututitlity.model.Shortcut;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class MainController {
    @FXML
    private TableView<?> tableView;

    @FXML
    private VBox shortcutListContainer;

    private List<Shortcut> sharedShortcutList;
    private JsonManager jsonManager;
    private GlobalHotkeyService hotkeyService;

    @FXML
    private javafx.scene.shape.Circle statusDot;
    @FXML
    private Label statusLabel;
    @FXML
    private HBox statusBadge; // Added statusBadge field
    @FXML
    private SVGPath powerIcon;
    @FXML
    private Button toggleButton;
    private final Set<KeyCode> activeKeys = new HashSet<>();

    @FXML
    private void handleDeleteShortcut() {
        System.out.println("Delete Shortcut");
    }

    private void addShortcutToUI(Shortcut shortcut) {
        // Extract data from the object inside the method
        String keys = shortcut.getKeyCombo();
        String functionName = shortcut.getAction().toString();

        HBox card = new HBox(20);
        card.getStyleClass().add("shortcut-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Keyboard Visuals (The "KBD" look)
        HBox keysBox = new HBox(5);
        keysBox.setAlignment(Pos.CENTER);
        for (String key : keys.split("\\+")) {
            Label k = new Label(key.trim());
            k.getStyleClass().add("kbd-key");
            keysBox.getChildren().add(k);
        }

        VBox details = new VBox(2);
        Label title = new Label(functionName);
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("System automated trigger");
        subtitle.getStyleClass().add("card-subtitle");
        details.getChildren().addAll(title, subtitle);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            // Since shortcut is an object in the shared list, remove(shortcut) works
            // perfectly
            shortcutListContainer.getChildren().remove(card);
            if (sharedShortcutList != null) {
                sharedShortcutList.remove(shortcut);
                jsonManager.saveShortcuts(sharedShortcutList);
            }
        });

        card.getChildren().addAll(keysBox, details, deleteBtn);
        shortcutListContainer.getChildren().add(card);
    }

    @FXML
    private javafx.scene.control.TextField shortcutInputField;

    @FXML
    private void initialize() {
        // Setup the ComboBox
        functionComboBox.setItems(FXCollections.observableArrayList(SystemAction.values()));

        // Setup the Recording Logic for the TextField
        shortcutInputField.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
            updateShortcutText();
            event.consume();
        });

        shortcutInputField.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
            event.consume();
        });

        // Set initial UI state
        updateStatusUI();
    }

    // 2. MANUAL: Called by MainApp to pass shared data.
    public void setDependencies(List<Shortcut> sharedShortcutList, JsonManager jsonManager,
            GlobalHotkeyService hotkeyService) {
        this.sharedShortcutList = sharedShortcutList;
        this.jsonManager = jsonManager;
        this.hotkeyService = hotkeyService;
        updateStatusUI();

        shortcutListContainer.getChildren().clear();
        if (this.sharedShortcutList != null) {
            for (Shortcut s : this.sharedShortcutList) {
                addShortcutToUI(s); // This now matches the new signature
            }
        }
    }

    @FXML
    private void handleToggleService() {
        if (hotkeyService != null) {
            if (hotkeyService.isActive()) {
                hotkeyService.stopHook();
            } else {
                hotkeyService.startHook();
            }
            updateStatusUI();
        }
    }

    private void updateStatusUI() {
        if (hotkeyService != null && hotkeyService.isActive()) {
            statusDot.setFill(javafx.scene.paint.Color.web("#22c55e"));
            if (statusDot.getEffect() instanceof DropShadow ds) {
                ds.setColor(javafx.scene.paint.Color.web("#22c55e"));
            }
            statusLabel.getStyleClass().remove("status-label-offline");
            statusBadge.getStyleClass().remove("status-badge-offline");
            statusLabel.setText("LIVE");
            powerIcon.setStroke(javafx.scene.paint.Color.web("#ef4444")); // Red for "Stop" action
            toggleButton.setStyle("-fx-text-fill: #ef4444;");
        } else {
            statusDot.setFill(javafx.scene.paint.Color.web("#ef4444"));
            if (statusDot.getEffect() instanceof DropShadow ds) {
                ds.setColor(javafx.scene.paint.Color.web("#ef4444"));
            }
            statusLabel.setText("OFFLINE");
            if (!statusLabel.getStyleClass().contains("status-label-offline")) {
                statusLabel.getStyleClass().add("status-label-offline");
            }
            if (!statusBadge.getStyleClass().contains("status-badge-offline")) {
                statusBadge.getStyleClass().add("status-badge-offline");
            }
            powerIcon.setStroke(javafx.scene.paint.Color.web("#22c55e")); // Green for "Start" action
            toggleButton.setStyle("-fx-text-fill: #22c55e;");
        }
    }

    private void updateShortcutText() {
        StringBuilder sb = new StringBuilder();

        // Match the canonical order: Ctrl -> Alt -> Shift -> Win -> Key
        if (activeKeys.contains(KeyCode.CONTROL))
            sb.append("Ctrl+");
        if (activeKeys.contains(KeyCode.ALT))
            sb.append("Alt+");
        if (activeKeys.contains(KeyCode.SHIFT))
            sb.append("Shift+");
        if (activeKeys.contains(KeyCode.WINDOWS) || activeKeys.contains(KeyCode.COMMAND))
            sb.append("Win+");

        // Find the non-modifier key
        for (KeyCode code : activeKeys) {
            if (!code.isModifierKey()) {
                sb.append(code.getName());
                break; // Only take the first non-modifier key
            }
        }

        shortcutInputField.setText(sb.toString());
    }

    @FXML
    private void handleAddShortcut() {
        String keys = shortcutInputField.getText();
        SystemAction action = functionComboBox.getValue();

        if (keys == null || keys.isEmpty() || action == null)
            return;

        Shortcut newShortcut = new Shortcut(keys, action);

        if (sharedShortcutList == null)
            sharedShortcutList = new ArrayList<>();

        sharedShortcutList.add(newShortcut);
        jsonManager.saveShortcuts(sharedShortcutList);

        addShortcutToUI(newShortcut); // Much cleaner call

        // UI Cleanup
        shortcutInputField.clear();
        activeKeys.clear(); // Important: reset the set so the next recording is fresh
        functionComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private ComboBox<SystemAction> functionComboBox;
}
