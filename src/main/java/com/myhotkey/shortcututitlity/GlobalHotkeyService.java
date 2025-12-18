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
    private List<Shortcut> shortcuts;
    private boolean enabled = true;

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
        this.enabled = true;
        if (GlobalScreen.isNativeHookRegistered()) {
            System.out.println("Native Hook already registered. Enabling soft hook.");
            return;
        }

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

    public boolean isActive() {
        return enabled;
    }

    public void stopHook() {
        this.enabled = false;
        System.out.println("Soft Hook Disabled (Native hook remains registered).");
    }

    public void unregisterService() {
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
                System.out.println("Native Hook Unregistered Successfully during cleanup.");
            }
        } catch (NativeHookException ex) {
            System.err.println("Could not unregister native hook: " + ex.getMessage());
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!enabled) {
            return;
        }

        String pressedKey = NativeKeyEvent.getKeyText(e.getKeyCode());
        int modifiers = e.getModifiers();

        StringBuilder sb = new StringBuilder();
        if ((modifiers & NativeKeyEvent.CTRL_L_MASK) != 0)
            sb.append("Ctrl+");
        if ((modifiers & NativeKeyEvent.ALT_L_MASK) != 0)
            sb.append("Alt+");
        if ((modifiers & NativeKeyEvent.SHIFT_L_MASK) != 0)
            sb.append("Shift+");
        sb.append(pressedKey);

        String currentCombo = sb.toString();

        if (shortcuts != null) {
            for (Shortcut s : shortcuts) {
                if (s.getKeyCombo().equalsIgnoreCase(currentCombo)) {
                    s.getAction().execute(robot);
                    break;
                }
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == NativeKeyEvent.VC_CONTROL ||
                keyCode == NativeKeyEvent.VC_ALT ||
                keyCode == NativeKeyEvent.VC_SHIFT ||
                keyCode == NativeKeyEvent.VC_META;
    }
}
