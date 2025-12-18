package com.myhotkey.shortcututitlity;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.myhotkey.shortcututitlity.model.Shortcut;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalHotkeyService implements NativeKeyListener {
    private Robot robot;
    private java.util.List<Shortcut> shortcuts;
    private JsonManager jsonManager;

    public GlobalHotkeyService() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void setShortcuts(List<Shortcut> shortcuts) {
        this.shortcuts = shortcuts;
    }

    public void startHook() {
        // Disable JNativeHook's noisy logging
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            System.out.println("Native Hook Registered Successfully.");
        } catch (NativeHookException ex) {
            System.err.println("Could not register native hook: " + ex.getMessage());
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
// Now 'shortcuts' is available here!
        String pressedKey = NativeKeyEvent.getKeyText(e.getKeyCode());
        int modifiers = e.getModifiers();

        // Build your canonical string (Ctrl+Alt+Key)
        StringBuilder sb = new StringBuilder();
        if ((modifiers & NativeKeyEvent.CTRL_L_MASK) != 0) sb.append("Ctrl+");
        if ((modifiers & NativeKeyEvent.ALT_L_MASK) != 0) sb.append("Alt+");
        if ((modifiers & NativeKeyEvent.SHIFT_L_MASK) != 0) sb.append("Shift+");
        sb.append(pressedKey);

        String currentCombo = sb.toString();

        // Loop through the SHARED list
        for (Shortcut s : shortcuts) {
            if (s.getKeyCombo().equalsIgnoreCase(currentCombo)) {
                s.getAction().execute(robot);
                break;
            }
        }
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == NativeKeyEvent.VC_CONTROL ||
                keyCode == NativeKeyEvent.VC_ALT ||
                keyCode == NativeKeyEvent.VC_SHIFT ||
                keyCode == NativeKeyEvent.VC_META;
    }
}
