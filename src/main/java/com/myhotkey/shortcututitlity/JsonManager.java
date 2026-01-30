package com.myhotkey.shortcututitlity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.myhotkey.shortcututitlity.model.Shortcut;
import com.myhotkey.shortcututitlity.enums.SystemAction;

public class JsonManager {
    private final String filePath;
    private final ObjectMapper mapper;

    public JsonManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Determine the persistent storage path
        this.filePath = getAppDataPath();
    }

    private String getAppDataPath() {
        // Get the Windows AppData path
        String workingDir = System.getenv("APPDATA");

        // Fallback for non-Windows or local testing
        if (workingDir == null) {
            workingDir = System.getProperty("user.home");
        }

        File appFolder = new File(workingDir, "KeyFlowUtility");

        // Create the folder if it doesn't exist
        if (!appFolder.exists()) {
            appFolder.mkdirs();
        }

        return new File(appFolder, "shortcuts.json").getAbsolutePath();
    }

    public void saveShortcuts(List<Shortcut> shortcuts) {
        try {
            mapper.writeValue(new File(filePath), shortcuts);
            System.out.println("Shortcuts saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Could not save shortcuts: " + e.getMessage());
        }
    }

    public List<Shortcut> loadShortcuts() {
        File file = new File(filePath);

        // Check if the file doesn't exist OR is empty
        if (!file.exists() || file.length() == 0) {
            System.out.println("No existing shortcut file found. Initializing defaults...");
            List<Shortcut> defaults = createDefaultShortcuts();

            // Save defaults to AppData immediately so the file exists for next time
            saveShortcuts(defaults);

            return new CopyOnWriteArrayList<>(defaults);
        }

        try {
            List<Shortcut> loadedList = mapper.readValue(file, new TypeReference<List<Shortcut>>() {
            });
            return new CopyOnWriteArrayList<>(loadedList);
        } catch (IOException e) {
            System.err.println("Error loading shortcuts: " + e.getMessage());
            // If the file is corrupted, it's safer to return the defaults rather than an
            // empty list
            return new CopyOnWriteArrayList<>(createDefaultShortcuts());
        }
    }

    private List<Shortcut> createDefaultShortcuts() {
        List<Shortcut> defaults = new ArrayList<>();

        // Use your specific enum actions and key combos
        defaults.add(new Shortcut("Ctrl+F12", SystemAction.VOLUME_UP));
        defaults.add(new Shortcut("Ctrl+F11", SystemAction.VOLUME_DOWN));
        defaults.add(new Shortcut("Ctrl+F10", SystemAction.MUTE));
        defaults.add(new Shortcut("Ctrl+F9", SystemAction.BRIGHTNESS_UP));
        defaults.add(new Shortcut("Ctrl+F8", SystemAction.BRIGHTNESS_DOWN));
        defaults.add(new Shortcut("Ctrl+F6", SystemAction.PLAY_PAUSE));
        defaults.add(new Shortcut("Ctrl+F5", SystemAction.PREV_TRACK));
        defaults.add(new Shortcut("Ctrl+F7", SystemAction.NEXT_TRACK));

        return defaults;
    }
}