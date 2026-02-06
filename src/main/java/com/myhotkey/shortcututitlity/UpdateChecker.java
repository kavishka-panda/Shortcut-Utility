package com.myhotkey.shortcututitlity;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles checking for software updates from GitHub releases
 */
public class UpdateChecker {
    
    private String currentVersion;
    private String githubRepoOwner;
    private String githubRepoName;
    private String githubApiUrl;
    private Stage primaryStage;
    
    public UpdateChecker() {
        loadConfiguration();
    }
    
    public UpdateChecker(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadConfiguration();
    }
    
    /**
     * Loads configuration from application.properties
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            if (input != null) {
                props.load(input);
                currentVersion = props.getProperty("app.version", "v1.0.0");
                githubRepoOwner = props.getProperty("github.owner", "kavishka-panda");
                githubRepoName = props.getProperty("github.repo", "keyflow_utility");
            } else {
                // Fallback to defaults if properties file not found
                currentVersion = "v1.0.0";
                githubRepoOwner = "kavishka-panda";
                githubRepoName = "keyflow_utility";
            }
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            // Use defaults
            currentVersion = "v1.0.0";
            githubRepoOwner = "kavishka-panda";
            githubRepoName = "keyflow_utility";
        }
        
        githubApiUrl = String.format(
            "https://api.github.com/repos/%s/%s/releases/latest",
            githubRepoOwner,
            githubRepoName
        );
    }
    
    /**
     * Checks for updates in a background thread and notifies the user if a new version is available
     */
    public void checkForUpdates() {
        // Run in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                System.out.println("Checking for updates...");
                System.out.println("Current version: " + currentVersion);
                System.out.println("GitHub API URL: " + githubApiUrl);
                
                // Fetch the latest version from GitHub
                UpdateInfo updateInfo = fetchLatestVersionFromGitHub();
                
                if (updateInfo != null && !currentVersion.equals(updateInfo.version)) {
                    // New version available - notify user on JavaFX thread
                    Platform.runLater(() -> {
                        showUpdateNotification(updateInfo);
                    });
                    System.out.println("New version available: " + updateInfo.version);
                } else {
                    System.out.println("Software is up to date. Current version: " + currentVersion);
                }
                
            } catch (Exception e) {
                System.err.println("Update check failed: " + e.getMessage());
                // Don't show error to user - fail silently for better UX
            }
        }).start();
    }
    
    /**
     * Fetches the latest release information from GitHub API
     * @return UpdateInfo object with version and download URL
     * @throws Exception if the request fails
     */
    private UpdateInfo fetchLatestVersionFromGitHub() throws Exception {
        URL url = new URL(githubApiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Set request properties
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "KeyFlow-Updater");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse JSON response using Jackson
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.toString());
                
                String latestVersion = rootNode.path("tag_name").asText();
                String downloadUrl = rootNode.path("html_url").asText();
                String releaseNotes = rootNode.path("body").asText();
                
                System.out.println("Latest version from GitHub: " + latestVersion);
                
                return new UpdateInfo(latestVersion, downloadUrl, releaseNotes);
                
            } else {
                throw new Exception("GitHub API returned status code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Shows an update notification dialog to the user
     * @param updateInfo Information about the available update
     */
    private void showUpdateNotification(UpdateInfo updateInfo) {
    Platform.runLater(() -> {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT); // Title bar එක අයින් කරයි
        
        // Taskbar එකේ පෙන්වීම වැළැක්වීමට (primaryStage ඇතුළත් කරන්න)
        if (this.primaryStage != null) { 
            stage.initOwner(this.primaryStage); 
        }
        
        stage.setAlwaysOnTop(true);

        // UI Container එක
        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #2b2b2b; " +
                     "-fx-padding: 25; " +
                     "-fx-background-radius: 15; " +
                     "-fx-border-color: #3d3d3d; " +
                     "-fx-border-radius: 15; " +
                     "-fx-border-width: 2;");
        root.setAlignment(Pos.CENTER);

        // Heading
        Label titleLabel = new Label("UPDATE AVAILABLE");
        titleLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-weight: bold; -fx-font-size: 16px;");

        // Version Details
        Label versionInfo = new Label("A new version is ready to download!");
        versionInfo.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        Label versions = new Label(currentVersion + "  ➔  " + updateInfo.version);
        versions.setStyle("-fx-text-fill: #aaaaaa; -fx-font-family: 'Consolas';");

        // Download Button (Hyperlink එක වෙනුවට)
        Button downloadBtn = new Button("Download Now");
        downloadBtn.setStyle("-fx-background-color: #00ffcc; -fx-text-fill: black; " +
                           "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        downloadBtn.setMinWidth(150);
        
        downloadBtn.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(updateInfo.downloadUrl));
                    stage.close();
                }
            } catch (Exception ex) {
                System.err.println("Failed to open browser: " + ex.getMessage());
            }
        });

        // Close Button (පහළින් ඇති කුඩා button එකක්)
        Button closeBtn = new Button("Later");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888888; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(titleLabel, versionInfo, versions, downloadBtn, closeBtn);

        Scene scene = new Scene(root);
        scene.setFill(null); // පසුබිම විනිවිද පෙනෙන ලෙස තබයි
        stage.setScene(scene);

        // තිරයේ මැදට වන්නට පෙන්වීම
        stage.show();
        
        // මැදට position කිරීම
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    });
}
    
    /**
     * Inner class to hold update information
     */
    private static class UpdateInfo {
        final String version;
        final String downloadUrl;
        final String releaseNotes;
        
        UpdateInfo(String version, String downloadUrl, String releaseNotes) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.releaseNotes = releaseNotes;
        }
    }
}
