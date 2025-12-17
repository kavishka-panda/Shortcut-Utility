package com.myhotkey.shortcututitlity;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalHotkeyService implements NativeKeyListener {
    private Robot robot;

    public GlobalHotkeyService() {
        try{
            this.robot = new Robot();
        }catch (AWTException exception){
            System.err.println("GlobalHotkeyService caught exception: "+exception.getMessage());
        }
    }

    public void startHook(){
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try{
            GlobalScreen.registerNativeHook();
            System.out.println("Register native hook");
        }catch (NativeHookException ex){
            System.err.println("There was a problem registering the native hook: "+ex.getMessage());
        }
        GlobalScreen.addNativeKeyListener(this);

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        int KeyCode = nativeEvent.getKeyCode();
        boolean isAltDown = (nativeEvent.getModifiers() & NativeKeyEvent.ALT_L_MASK) != 0;

        if(isAltDown && KeyCode == NativeKeyEvent.VC_U){
            handleVolumeUp();
        }

        if(isAltDown && KeyCode == NativeKeyEvent.VC_D){
            handelVolumeDown();
        }
    }

    public void handelVolumeDown(){
        if(robot != null){
                String cmd = "mshta vbscript:CreateObject(\"WScript.Shell\").SendKeys(chr(174))(window.close)";
                try {
                    Runtime.getRuntime().exec(cmd);
                    System.out.println("Volume Down triggered via VBScript fallback.");
                } catch (Exception runtimeEx) {
                    runtimeEx.printStackTrace();
                }
        }
    }

    public void handleVolumeUp() {
        if (robot != null) {
            try {
                robot.keyPress(521);
                robot.keyRelease(521);
            } catch (IllegalArgumentException e) {
                String cmd = "mshta vbscript:CreateObject(\"WScript.Shell\").SendKeys(chr(175))(window.close)";
                try { Runtime.getRuntime().exec(cmd); } catch (Exception ex) {}
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {

    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {

    }
}
