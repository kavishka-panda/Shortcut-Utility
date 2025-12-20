package com.myhotkey.shortcututitlity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.awt.*;
import java.awt.event.KeyEvent;

public enum SystemAction {
    VOLUME_UP {
        @Override
        public void execute() {
            executeVBS(175);
        }
    },
    VOLUME_DOWN {
        @Override
        public void execute() {
            executeVBS(174);
        }
    },
    MUTE {
        @Override
        public void execute(){
            executeVBS(173);
        }
    },
    PLAY_PAUSE {
        @Override
        public void execute() {
            executeVBS(179);
        }
    },
    NEXT_TRACK {
        @Override
        public void execute() {
            executeVBS(176);
        }
    },
    PREV_TRACK {
        @Override
        public void execute() {
            executeVBS(177);
        }
    },
    BRIGHTNESS_UP {
        @Override
        public void execute() {
            String script = "$b = (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightness).CurrentBrightness; " +
                    "if($b -le 90) { (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $b + 20) }";
            executePowerShell(script);
        }
    },
    BRIGHTNESS_DOWN{
        @Override
        public void execute() {
            String script = "$b = (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightness).CurrentBrightness; " +
                    "if($b -ge 10) { (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $b - 20) }";
            executePowerShell(script);
        }
    };

    @JsonCreator
    public static SystemAction fromString(String value) {
        return SystemAction.valueOf(value.toUpperCase());
    }

    public abstract void execute();

    protected void executePowerShell(String script) {
        try {
            String command = "powershell.exe -ExecutionPolicy Bypass -Command \"" + script + "\"";
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Your existing VBS helper
    protected void executeVBS(int code) {
        String cmd = "mshta vbscript:CreateObject(\"WScript.Shell\").SendKeys(chr(" + code + "))(window.close)";
        try { Runtime.getRuntime().exec(cmd); } catch (Exception ignored) {}
    }
}
