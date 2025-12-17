package com.myhotkey.shortcututitlity.enums;

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
                runWindowsCommand(175);
            }
        }
    },
    VOLUME_DOWN {
        @Override
        public void execute(Robot robot) {
            runWindowsCommand(174);
        }
    },
    BRIGHTNESS_UP {
        @Override
        public void execute(Robot robot) {
            runWindowsCommand(191);
        }
    },
    BRIGHTNESS_DOWN{
        @Override
        public void execute(Robot robot) {
            runWindowsCommand(192);
        }
    };

    public abstract void execute(Robot robot);

    protected void runWindowsCommand(int code) {
        String cmd = "mshta vbscript:CreateObject(\"WScript.Shell\").SendKeys(chr(" + code + "))(window.close)";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception runtimeEx) {
            runtimeEx.printStackTrace();
        }
    }
    }
