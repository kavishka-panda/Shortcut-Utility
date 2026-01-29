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

public class JsonManager {
    private static final String FILE_PATH = "shortcuts.json";
    private final ObjectMapper mapper;

    public JsonManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void saveShortcuts(List<Shortcut> shortcuts){
        try {
            mapper.writeValue(new File(FILE_PATH), shortcuts);
            System.out.println("Shortcuts saved successfully.");
        } catch (IOException e) {
           System.err.println("Could not save shortcuts: " + e.getMessage());
        }
    }

    public List<Shortcut> loadShortcuts(){
       File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("No existing shortcut file found. Starting fresh.");
            return new CopyOnWriteArrayList<>();
        }

        try {
            List<Shortcut> loadedList = mapper.readValue(file, new TypeReference<List<Shortcut>>() {});
            return new CopyOnWriteArrayList<>(loadedList);
        } catch (IOException e) {
            System.err.println("Error loading shortcuts: " + e.getMessage());
            return new CopyOnWriteArrayList<>();
        }
    }
}
