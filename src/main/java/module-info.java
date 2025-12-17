module com.myhotkey.shortcututitlity {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // Required for Swing/AWT and FlatLaf
    requires com.formdev.flatlaf; // Required to use FlatLaf
    requires com.github.kwhat.jnativehook;
    requires com.fasterxml.jackson.databind;

    opens com.myhotkey.shortcututitlity to javafx.fxml;

    exports com.myhotkey.shortcututitlity;
    exports com.myhotkey.shortcututitlity.model;
    opens com.myhotkey.shortcututitlity.model to javafx.fxml;
}