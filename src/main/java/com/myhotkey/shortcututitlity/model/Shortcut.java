package com.myhotkey.shortcututitlity.model;

import com.myhotkey.shortcututitlity.enums.SystemAction;

public class Shortcut {
    private String keys;
    private SystemAction action;

    public Shortcut() {
    }

    public Shortcut(String keys, SystemAction action) {
        this.keys = keys;
        this.action = action;
    }

    public String getKeyCombo() {
        return keys;
    }

    public void setKeyCombo(String keys) {
        this.keys = keys;
    }

    public SystemAction getAction() {
        return action;
    }

    public void setAction(SystemAction action) {
        this.action = action;
    }
}
