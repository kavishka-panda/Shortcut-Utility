# KeyFlow Utility

KeyFlow Utility is a desktop application for Windows that allows you to create system-wide shortcuts to automate common tasks.

## Features

*   **Global Hotkeys**: Create shortcuts that work even when the application is not in focus.
*   **Custom Actions**: Assign actions like opening applications, files, or system commands to your shortcuts.
*   **Modern UI**: A clean and modern user interface built with JavaFX, styled with a dark theme.
*   **System Tray Integration**: The application can run in the background and is accessible from the system tray.
*   **Run on Startup**: Configure the application to start automatically with Windows.

## Building and Running the Project

This project is built with Apache Maven.

### Prerequisites

*   Java Development Kit (JDK) 21 or later.
*   Apache Maven.

### Build

To build the project and create an executable JAR file, run the following command in the project's root directory:

```bash
mvn clean package
```

This will generate a `ShortcutUtility-1.0.jar` file in the `target` directory. The executable `ShortcutUtility-1.0.exe` is also created in the root directory.

### Run

You can run the application in multiple ways:

1.  **From the command line (using Maven)**:
    ```bash
    mvn clean javafx:run
    ```

2.  **By executing the JAR file**:
    ```bash
    java -jar target/ShortcutUtility-1.0.jar
    ```
3.  **By running the .exe file**:
    Double-click `ShortcutUtility-1.0.exe` in the root folder of the project.

## Dependencies

This project uses the following main libraries:

*   **JavaFX**: For the user interface.
*   **JNativeHook**: For listening to global keyboard events.
*   **Jackson**: For saving and loading shortcuts to/from JSON.
*   **AtlantaFX & FlatLaf**: For the application's look and feel.
*   **JUnit**: For testing.
