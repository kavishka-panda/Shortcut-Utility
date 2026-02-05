# KeyFlow Utility Documentation

## Overview

**KeyFlow Utility** is a powerful, lightweight desktop application designed for Windows that enables users to create and manage system-wide keyboard shortcuts (hotkeys). It allows for the automation of common system tasks such as adjusting volume, screen brightness, and controlling media playback, even when the application is not in focus or is minimized to the system tray.

## Key Features

- **Global Hotkeys**: Shortcuts work across the entire operating system, regardless of which application is currently active.
- **Customizable Actions**: Users can assign specific keyboard combinations to a variety of system-level actions.
- **Modern User Interface**: A sleek, dark-themed UI built with JavaFX and AtlantaFX, providing a user-friendly experience for managing shortcuts.
- **System Tray Integration**: The application can run entirely in the background, accessible via an icon in the Windows system tray.
- **Persistent Storage**: All user-defined shortcuts are automatically saved and reloaded upon application restart.
- **On-Screen Notifications**: Provides visual feedback in the corner of the screen whenever a shortcut is triggered.
- **Debounce Protection**: Built-in mechanism to prevent accidental double-triggering of actions during rapid key presses.
- **Run on Startup Support**: Can be configured to launch automatically with Windows (via the `--tray` command-line argument for a silent start).

## Technologies Used

- **Java 21**: The core programming language.
- **JavaFX 21**: Used for building the modern, responsive user interface.
- **JNativeHook**: A library used to provide global keyboard and mouse listeners for Java.
- **Jackson**: Handles JSON serialization and deserialization for shortcut persistence.
- **AtlantaFX & FlatLaf**: Provide modern CSS themes (Primer Dark) for the JavaFX components.
- **NirCmd**: A small command-line utility bundled with the app to perform advanced system actions like brightness and volume control efficiently.
- **Apache Maven**: The project management and build automation tool.

## How to Use

### Installation and Building

To build KeyFlow Utility from source:

1. Ensure you have **JDK 21** and **Maven** installed.
2. Clone the repository.
3. Run the following command in the root directory:
   ```bash
   mvn clean package
   ```
4. This will generate a `ShortcutUtility-1.0.jar` in the `target` directory and an executable `ShortcutUtility-1.0.exe` in the root.

### Running the Application

- **Standard Run**: Double-click `ShortcutUtility-1.0.exe` or run `java -jar target/ShortcutUtility-1.0.jar`.
- **Minimized Start**: Run with the `--tray` argument to start the application directly in the system tray without showing the main window:
  ```bash
  java -jar target/ShortcutUtility-1.0.jar --tray
  ```

### Managing Shortcuts

1. **Adding a Shortcut**:
   - Click on the shortcut input field.
   - Press the desired key combination (e.g., `Ctrl + Alt + V`). The app will record the keys automatically.
   - Select the desired action from the dropdown menu.
   - Click the **Add** button.
2. **Deleting a Shortcut**:
   - Find the shortcut in the list.
   - Click the **✕** (Delete) button on the right side of the shortcut card.
3. **Toggling the Service**:
   - Use the Power icon/Toggle button in the bottom bar to enable (LIVE) or disable (OFFLINE) the global hotkey listener.

## Facilities and Infrastructure

### Background Service (`GlobalHotkeyService`)
The application uses a dedicated background service that leverages `JNativeHook`. This service runs in its own thread to ensure that the UI remains responsive while listening for system-wide keyboard events. It includes a normalization engine to ensure key combinations are recognized consistently.

### Persistent Storage
Shortcuts are stored in a JSON file named `shortcuts.json`. By default, this file is located in:
- Windows: `%APPDATA%\KeyFlowUtility\shortcuts.json`
- Others (Fallback): `user.home/KeyFlowUtility/shortcuts.json`

If the file does not exist, the application initializes with a set of default shortcuts.

### System Tray
Closing the main window does not exit the application. It remains active in the system tray. To exit completely, right-click the tray icon and select **Exit**.

### System Action Execution
Actions are executed using the most efficient method available:
1. **NirCmd**: Preferred for its speed and reliability.
2. **VBScript**: Used as a fallback for media keys.
3. **PowerShell (WMI)**: Used as a fallback for brightness control.

## Supported System Actions

| Action | Category | Description |
| :--- | :--- | :--- |
| **Volume Up** | Volume Control | Increases system volume. |
| **Volume Down** | Volume Control | Decreases system volume. |
| **Mute/Unmute** | Volume Control | Toggles the system mute state. |
| **Play/Pause** | Media Control | Toggles playback in media players. |
| **Next Track** | Media Control | Skips to the next track. |
| **Previous Track**| Media Control | Returns to the previous track. |
| **Brightness Up** | Display Control | Increases screen brightness by 10%. |
| **Brightness Down**| Display Control | Decreases screen brightness by 10%. |

## Default Shortcuts

Upon first run, the following shortcuts are pre-configured:

- `Ctrl + F12`: Volume Up
- `Ctrl + F11`: Volume Down
- `Ctrl + F10`: Mute
- `Ctrl + F9`: Brightness Up
- `Ctrl + F8`: Brightness Down
- `Ctrl + F6`: Play/Pause
- `Ctrl + F5`: Previous Track
- `Ctrl + F7`: Next Track
