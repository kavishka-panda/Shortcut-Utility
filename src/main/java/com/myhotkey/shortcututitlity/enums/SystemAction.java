package com.myhotkey.shortcututitlity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.awt.*;

public enum SystemAction {
    VOLUME_UP {
        @Override
        public void execute(Robot robot) {
            try {
                // Implementation for volume up action
                robot.keyPress(521);
                robot.keyRelease(521);
            } catch (IllegalArgumentException exception) {
                executeVBS(175);
            }
        }
    },
    VOLUME_DOWN {
        @Override
        public void execute(Robot robot) {
            executeVBS(174);
        }
    },
    BRIGHTNESS_UP {
        @Override
        public void execute(Robot robot) {
            String script = "$b = (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightness).CurrentBrightness; " +
                    "if($b -le 90) { (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $b + 10) }";
            executePowerShell(script);
        }
    },
    BRIGHTNESS_DOWN{
        @Override
        public void execute(Robot robot) {
            String script = "$b = (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightness).CurrentBrightness; " +
                    "if($b -ge 10) { (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $b - 10) }";
            executePowerShell(script);
        }
    };

    @JsonCreator
    public static SystemAction fromString(String value) {
        return SystemAction.valueOf(value.toUpperCase());
    }

    public abstract void execute(Robot robot);

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
