package com.myhotkey.shortcututitlity.model;

public class ShortcutConfig {
    private String keys;
    private String function;

    public ShortcutConfig(String keys, String function) {
        this.keys = keys;
        this.function = function;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }
}
