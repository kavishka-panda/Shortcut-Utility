package com.myhotkey.shortcututitlity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;

/**
 * Enum representing system-level actions that can be triggered by keyboard
 * shortcuts.
 * Supports media controls (volume, playback) and display brightness
 * adjustments.
 * 
 * Execution methods (in order of preference):
 * 1. NirCmd (if available) - Fastest, most reliable
 * 2. VBS fire-and-forget - Fast, always available on Windows
 * 3. PowerShell WMI for brightness control
 */
public enum SystemAction {

    VOLUME_UP("Volume Up", 0xAF) {
        @Override
        public void execute() {
            executeMediaKey(175, "changesysvolume 2000");
        }
    },

    VOLUME_DOWN("Volume Down", 0xAE) {
        @Override
        public void execute() {
            executeMediaKey(174, "changesysvolume -2000");
        }
    },

    MUTE("Mute/Unmute", 0xAD) {
        @Override
        public void execute() {
            executeMediaKey(173, "mutesysvolume 2");
        }
    },

    PLAY_PAUSE("Play/Pause", 0xB3) {
        @Override
        public void execute() {
            executeMediaKey(179, "sendkeypress 0xB3");
        }
    },

    NEXT_TRACK("Next Track", 0xB0) {
        @Override
        public void execute() {
            executeMediaKey(176, "sendkeypress 0xB0");
        }
    },

    PREV_TRACK("Previous Track", 0xB1) {
        @Override
        public void execute() {
            executeMediaKey(177, "sendkeypress 0xB1");
        }
    },

    BRIGHTNESS_UP("Brightness Up", 0x00) {
        @Override
        public void execute() {
            adjustBrightness(true);
        }
    },

    BRIGHTNESS_DOWN("Brightness Down", 0x00) {
        @Override
        public void execute() {
            adjustBrightness(false);
        }
    };

    private static final Logger LOGGER = Logger.getLogger(SystemAction.class.getName());
    private static final int BRIGHTNESS_STEP = 10;

    // Cache for NirCmd availability
    private static volatile Boolean nircmdAvailable = null;
    private static volatile String nircmdPath = null;

    private final String displayName;
    private final int virtualKeyCode;

    SystemAction(String displayName, int virtualKeyCode) {
        this.displayName = displayName;
        this.virtualKeyCode = virtualKeyCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getVirtualKeyCode() {
        return virtualKeyCode;
    }

    @JsonValue
    public String toJsonValue() {
        return this.name();
    }

    @JsonCreator
    public static SystemAction fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SystemAction value cannot be null or empty");
        }

        try {
            return SystemAction.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid SystemAction value: " + value, e);
            throw new IllegalArgumentException("Invalid system action: " + value +
                    ". Valid values: " + getAllActionNames(), e);
        }
    }

    private static String getAllActionNames() {
        StringBuilder sb = new StringBuilder();
        for (SystemAction action : values()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(action.name());
        }
        return sb.toString();
    }

    public abstract void execute();

    public CompletableFuture<Boolean> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                execute();
                return true;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing " + this.name() + " asynchronously", e);
                return false;
            }
        });
    }

    /**
     * Executes a media key action using the best available method.
     * Priority: NirCmd (if available) > Fire-and-forget VBS
     * 
     * @param vbsKeyCode    VBS virtual key code
     * @param nircmdCommand NirCmd command (if NirCmd is available)
     */
    protected static void executeMediaKey(int vbsKeyCode, String nircmdCommand) {
        if (!isWindows()) {
            LOGGER.warning("Media key execution is only supported on Windows");
            return;
        }

        // Try NirCmd first (fastest and most reliable)
        if (isNirCmdAvailable()) {
            executeNirCmd(nircmdCommand);
        } else {
            // Fall back to VBS fire-and-forget (no waiting)
            executeVBSFireAndForget(vbsKeyCode);
        }
    }

    /**
     * Checks if NirCmd is available on the system.
     * Caches the result for performance.
     * 
     * @return true if NirCmd is available
     */
    private static boolean isNirCmdAvailable() {
        if (nircmdAvailable != null) {
            return nircmdAvailable;
        }

        // 1. Define the temporary path where NirCmd will live
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), "nircmd.exe");
        nircmdPath = tempPath.toString();

        // 2. If it's already there, we're good
        if (Files.exists(tempPath)) {
            nircmdAvailable = true;
            return true;
        }

        // 3. Extraction Logic: Pull from Resources to Temp Folder
        // Based on your structure, the resource path is "/image/nircmd.exe"
        try (InputStream is = SystemAction.class.getResourceAsStream("/image/nircmd.exe")) {
            if (is != null) {
                Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
                nircmdAvailable = true;
                LOGGER.info("NirCmd extracted successfully to: " + nircmdPath);
                return true;
            } else {
                LOGGER.warning("NirCmd not found in resources at /image/nircmd.exe");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to extract NirCmd to temporary directory", e);
        }

        // 4. Final fallback: Check if it's just in the system PATH
        try {
            Process process = new ProcessBuilder("nircmd.exe", "help").start();
            if (process.waitFor(500, TimeUnit.MILLISECONDS) && process.exitValue() == 0) {
                nircmdPath = "nircmd.exe";
                nircmdAvailable = true;
                return true;
            }
        } catch (Exception e) {
            /* Not in path */ }

        nircmdAvailable = false;
        return false;
    }

    /**
     * Executes a NirCmd command (very fast, ~10ms response time).
     * 
     * @param command NirCmd command to execute
     */
    protected static void executeNirCmd(String command) {
        CompletableFuture.runAsync(() -> {
            try {
                String[] cmd = command.split("\\s+");
                String[] fullCmd = new String[cmd.length + 1];
                fullCmd[0] = nircmdPath != null ? nircmdPath : "nircmd.exe";
                System.arraycopy(cmd, 0, fullCmd, 1, cmd.length);

                Process process = new ProcessBuilder(fullCmd).start();
                boolean completed = process.waitFor(500, TimeUnit.MILLISECONDS);

                if (!completed) {
                    process.destroyForcibly();
                } else if (process.exitValue() == 0) {
                    LOGGER.fine("NirCmd executed successfully: " + command);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "NirCmd execution failed", e);
            }
        });
    }

    /**
     * Executes VBS in fire-and-forget mode (no waiting for completion).
     * This eliminates timeout warnings and provides instant response.
     * 
     * @param virtualKeyCode Windows virtual key code
     */
    protected static void executeVBSFireAndForget(int virtualKeyCode) {
        if (virtualKeyCode == 0) {
            LOGGER.warning("Invalid virtual key code: 0");
            return;
        }

        String command = String.format(
                "mshta vbscript:CreateObject(\"WScript.Shell\").SendKeys(chr(%d))(window.close)",
                virtualKeyCode);

        // Fire and forget - don't wait for completion
        CompletableFuture.runAsync(() -> {
            try {
                Runtime.getRuntime().exec(command);
                LOGGER.fine("VBS command sent (fire-and-forget): " + virtualKeyCode);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to execute VBS command", e);
            }
        });
    }

    /**
     * Adjusts screen brightness using PowerShell (Windows only).
     * Uses WMI to control monitor brightness.
     * 
     * @param increase true to increase brightness, false to decrease
     */
    protected static void adjustBrightness(boolean increase) {
        if (!isWindows()) {
            LOGGER.warning("Brightness control is only supported on Windows");
            return;
        }

        // Check if NirCmd is available for brightness control (much faster)
        if (isNirCmdAvailable()) {
            String command = increase ? "changebrightness " + BRIGHTNESS_STEP : "changebrightness -" + BRIGHTNESS_STEP;
            executeNirCmd(command);
            return;
        }

        // Fall back to PowerShell WMI method
        String script = String.format(
                "try { " +
                        "$monitor = Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightness -ErrorAction Stop; " +
                        "$current = $monitor.CurrentBrightness; " +
                        "$new = $current %s %d; " +
                        "if ($new -lt 0) { $new = 0 }; " +
                        "if ($new -gt 100) { $new = 100 }; " +
                        "if ($new -ne $current) { " +
                        "  (Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, $new) | Out-Null "
                        +
                        "} " +
                        "} catch { " +
                        "  Write-Error \"Failed to adjust brightness: $($_.Exception.Message)\" " +
                        "}",
                increase ? "+" : "-",
                BRIGHTNESS_STEP);

        executePowerShellAsync(script);
    }

    /**
     * Executes a PowerShell script asynchronously.
     * 
     * @param script PowerShell script to execute
     */
    protected static void executePowerShellAsync(String script) {
        if (!isWindows()) {
            LOGGER.warning("PowerShell execution is only supported on Windows");
            return;
        }

        CompletableFuture.runAsync(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-WindowStyle", "Hidden",
                    "-ExecutionPolicy", "Bypass",
                    "-Command", script);

            pb.redirectErrorStream(true);

            try {
                Process process = pb.start();
                final Process finalProcess = process;

                // Read output asynchronously
                CompletableFuture<String> outputFuture = CompletableFuture
                        .supplyAsync(() -> readProcessOutput(finalProcess));

                boolean completed = process.waitFor(3, TimeUnit.SECONDS);

                if (!completed) {
                    process.destroyForcibly();
                    LOGGER.warning("PowerShell script timed out");
                } else {
                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        String output = outputFuture.getNow("");
                        LOGGER.warning("PowerShell failed with exit code " + exitCode +
                                (output.isEmpty() ? "" : ": " + output));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "PowerShell execution failed", e);
            }
        });
    }

    /**
     * Reads all output from a process.
     * 
     * @param process Process to read from
     * @return Process output as string
     */
    private static String readProcessOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading process output", e);
        }
        return output.toString().trim();
    }

    /**
     * Checks if this action is supported on the current operating system.
     * 
     * @return true if the action can be executed on this OS
     */
    public boolean isSupported() {
        return isWindows();
    }

    /**
     * Checks if the current OS is Windows.
     * 
     * @return true if running on Windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows");
    }

    /**
     * Gets the category of this action for UI grouping.
     * 
     * @return Category name
     */
    public String getCategory() {
        return switch (this) {
            case VOLUME_UP, VOLUME_DOWN, MUTE -> "Volume Control";
            case PLAY_PAUSE, NEXT_TRACK, PREV_TRACK -> "Media Control";
            case BRIGHTNESS_UP, BRIGHTNESS_DOWN -> "Display Control";
        };
    }

    /**
     * Returns a description of what this action does.
     * 
     * @return Action description
     */
    public String getDescription() {
        return switch (this) {
            case VOLUME_UP -> "Increases system volume";
            case VOLUME_DOWN -> "Decreases system volume";
            case MUTE -> "Toggles system mute";
            case PLAY_PAUSE -> "Plays or pauses media playback";
            case NEXT_TRACK -> "Skips to next media track";
            case PREV_TRACK -> "Returns to previous media track";
            case BRIGHTNESS_UP -> "Increases screen brightness by " + BRIGHTNESS_STEP + "%";
            case BRIGHTNESS_DOWN -> "Decreases screen brightness by " + BRIGHTNESS_STEP + "%";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
