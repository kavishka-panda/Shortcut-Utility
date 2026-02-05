package com.myhotkey.shortcututitlity;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    
    public UpdateChecker() {
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("New Version Available: " + updateInfo.version);
        
        // Create content with clickable link
        VBox content = new VBox(10);
        
        Label messageLabel = new Label(
            "A new version of KeyFlow is available!\n\n" +
            "Current Version: " + currentVersion + "\n" +
            "Latest Version: " + updateInfo.version + "\n\n" +
            "Click the link below to download the latest version:"
        );
        
        Hyperlink downloadLink = new Hyperlink(updateInfo.downloadUrl);
        downloadLink.setOnAction(e -> {
            try {
                // Open the download page in the default browser
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(updateInfo.downloadUrl));
                }
            } catch (Exception ex) {
                System.err.println("Failed to open browser: " + ex.getMessage());
            }
        });
        
        content.getChildren().addAll(messageLabel, downloadLink);
        alert.getDialogPane().setContent(content);
        
        // Add buttons
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        
        alert.showAndWait();
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
