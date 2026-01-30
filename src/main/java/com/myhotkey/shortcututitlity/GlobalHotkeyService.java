package com.myhotkey.shortcututitlity;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.myhotkey.shortcututitlity.model.Shortcut;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * Service for managing global keyboard shortcuts across the entire system.
 * This service uses JNativeHook to capture keyboard events at the OS level.
 * 
 * Thread-safe implementation with optimized shortcut lookup.
 */
public class GlobalHotkeyService implements NativeKeyListener, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(GlobalHotkeyService.class.getName());

    // Thread-safe shortcut storage with fast lookup
    private final Map<String, Shortcut> shortcutMap = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Atomic flag for enabling/disabling the service
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    // Track if we've registered the native hook
    private final AtomicBoolean hooked = new AtomicBoolean(false);

    // Statistics tracking
    private final AtomicBoolean statisticsEnabled = new AtomicBoolean(false);
    private volatile long eventsProcessed = 0;
    private volatile long shortcutsTriggered = 0;
    private volatile long lastEventTimestamp = 0;

    // Debouncing to prevent double-triggering
    private static final long DEBOUNCE_THRESHOLD_MS = 50;
    private final Map<String, Long> lastTriggerTime = new ConcurrentHashMap<>();

    // Listener for raw key press events
    private Consumer<String> onKeyPressedListener;

    /**
     * Creates a new GlobalHotkeyService instance.
     */
    public GlobalHotkeyService() {
        LOGGER.info("GlobalHotkeyService initialized");
    }

    /**
     * Sets the list of shortcuts to monitor.
     * This method is thread-safe and rebuilds the internal shortcut map.
     * 
     * @param shortcuts List of shortcuts to set (null-safe, creates defensive copy)
     */
    public void setShortcuts(List<Shortcut> shortcuts) {
        lock.writeLock().lock();
        try {
            shortcutMap.clear();

            if (shortcuts != null) {
                for (Shortcut shortcut : shortcuts) {
                    if (shortcut != null && isValidShortcut(shortcut)) {
                        String normalizedCombo = normalizeKeyCombo(shortcut.getKeyCombo());
                        shortcutMap.put(normalizedCombo, shortcut);
                    } else {
                        LOGGER.warning("Skipping invalid shortcut: " + shortcut);
                    }
                }
                LOGGER.info("Loaded " + shortcutMap.size() + " valid shortcuts");
            } else {
                LOGGER.info("Shortcuts list is null, clearing all shortcuts");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a defensive copy of all registered shortcuts.
     * 
     * @return List of shortcuts (never null)
     */
    public List<Shortcut> getShortcuts() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(shortcutMap.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a single shortcut to the service.
     * 
     * @param shortcut The shortcut to add
     * @return true if added successfully, false otherwise
     */
    public boolean addShortcut(Shortcut shortcut) {
        if (shortcut == null || !isValidShortcut(shortcut)) {
            LOGGER.warning("Cannot add invalid shortcut: " + shortcut);
            return false;
        }

        lock.writeLock().lock();
        try {
            String normalizedCombo = normalizeKeyCombo(shortcut.getKeyCombo());
            shortcutMap.put(normalizedCombo, shortcut);
            LOGGER.info("Added shortcut: " + normalizedCombo);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a shortcut by its key combination.
     * 
     * @param keyCombo The key combination to remove
     * @return true if removed, false if not found
     */
    public boolean removeShortcut(String keyCombo) {
        if (keyCombo == null || keyCombo.trim().isEmpty()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            String normalizedCombo = normalizeKeyCombo(keyCombo);
            boolean removed = shortcutMap.remove(normalizedCombo) != null;
            if (removed) {
                LOGGER.info("Removed shortcut: " + normalizedCombo);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Starts the global keyboard hook.
     * This method is idempotent - calling it multiple times is safe.
     * 
     * @return true if hook started successfully or was already running
     */
    public boolean startHook() {
        if (hooked.get()) {
            LOGGER.info("Native hook already registered, enabling service");
            enabled.set(true);
            return true;
        }

        // Configure JNativeHook logging
        configureNativeHookLogging();

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            hooked.set(true);
            enabled.set(true);
            LOGGER.info("Native hook registered successfully");
            return true;
        } catch (NativeHookException ex) {
            LOGGER.log(Level.SEVERE, "Failed to register native hook", ex);
            return false;
        }
    }

    /**
     * Checks if the service is currently active (processing events).
     * 
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return enabled.get();
    }

    /**
     * Checks if the native hook is registered.
     * 
     * @return true if hooked, false otherwise
     */
    public boolean isHooked() {
        return hooked.get();
    }

    /**
     * Temporarily disables the service without unregistering the native hook.
     * This is useful for temporarily pausing shortcut processing.
     */
    public void stopHook() {
        enabled.set(false);
        LOGGER.info("Service disabled (native hook remains registered)");
    }

    /**
     * Re-enables the service after it was stopped.
     */
    public void resumeHook() {
        if (!hooked.get()) {
            LOGGER.warning("Cannot resume: native hook not registered. Call startHook() first.");
            return;
        }
        enabled.set(true);
        LOGGER.info("Service resumed");
    }

    /**
     * Completely unregisters the native hook and cleans up resources.
     * This should be called when the application shuts down.
     */
    public void unregisterService() {
        enabled.set(false);

        if (!hooked.get()) {
            LOGGER.info("Native hook not registered, nothing to unregister");
            return;
        }

        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            hooked.set(false);
            LOGGER.info("Native hook unregistered successfully");
        } catch (NativeHookException ex) {
            LOGGER.log(Level.SEVERE, "Failed to unregister native hook", ex);
        }
    }

    /**
     * AutoCloseable implementation for try-with-resources support.
     */
    @Override
    public void close() {
        unregisterService();
    }

   @Override
public void nativeKeyPressed(NativeKeyEvent e) {
    if (!enabled.get()) return;

    updateStatistics();
    if (isModifierKey(e.getKeyCode())) return;

    try {
        String currentCombo = buildKeyCombo(e);
        String normalizedCombo = normalizeKeyCombo(currentCombo);

        if (isDebouncedEvent(normalizedCombo)) return;

        lock.readLock().lock();
        try {
            Shortcut shortcut = shortcutMap.get(normalizedCombo);
            if (shortcut != null) {
                // We only execute and notify if a valid shortcut was found
                executeShortcut(shortcut, normalizedCombo);
            }
        } finally {
            lock.readLock().unlock();
        }
    } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Error processing key event", ex);
    }
}

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Can be used for key-up detection if needed in the future
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used for shortcut detection
    }

    /**
     * Builds a key combination string from the native key event.
     * 
     * @param e The native key event
     * @return Formatted key combination string (e.g., "Ctrl+Shift+A")
     */
    private String buildKeyCombo(NativeKeyEvent e) {
        StringBuilder sb = new StringBuilder(32); // Pre-sized for typical combos
        int modifiers = e.getModifiers();

        // Build in consistent order: Ctrl -> Alt -> Shift -> Win
        if ((modifiers & NativeKeyEvent.CTRL_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & NativeKeyEvent.ALT_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & NativeKeyEvent.SHIFT_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & NativeKeyEvent.META_MASK) != 0) {
            sb.append("Win+");
        }

        String keyText = NativeKeyEvent.getKeyText(e.getKeyCode());
        sb.append(keyText);

        return sb.toString();
    }

    /**
     * Normalizes a key combination for consistent lookup.
     * Handles case sensitivity and whitespace.
     * 
     * @param keyCombo The key combination to normalize
     * @return Normalized key combination
     */
    private String normalizeKeyCombo(String keyCombo) {
        if (keyCombo == null) {
            return "";
        }
        // Normalize to title case with consistent formatting
        return keyCombo.trim()
                .replaceAll("\\s+", "")
                .toLowerCase()
                .replace("ctrl", "Ctrl")
                .replace("alt", "Alt")
                .replace("shift", "Shift")
                .replace("win", "Win")
                .replace("meta", "Win");
    }

    /**
     * Validates a shortcut for correctness.
     * 
     * @param shortcut The shortcut to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidShortcut(Shortcut shortcut) {
        if (shortcut == null) {
            return false;
        }

        String keyCombo = shortcut.getKeyCombo();
        if (keyCombo == null || keyCombo.trim().isEmpty()) {
            LOGGER.warning("Invalid shortcut: empty key combination");
            return false;
        }

        if (shortcut.getAction() == null) {
            LOGGER.warning("Invalid shortcut: null action for " + keyCombo);
            return false;
        }

        return true;
    }

    /**
     * Checks if a key code represents a modifier key.
     * 
     * @param keyCode The key code to check
     * @return true if it's a modifier key
     */
    private boolean isModifierKey(int keyCode) {
        return keyCode == NativeKeyEvent.VC_CONTROL ||
                keyCode == NativeKeyEvent.VC_ALT ||
                keyCode == NativeKeyEvent.VC_SHIFT ||
                keyCode == NativeKeyEvent.VC_META;
    }

    /**
     * Checks if an event should be debounced (ignored due to rapid repetition).
     * 
     * @param keyCombo The key combination
     * @return true if event should be ignored
     */
    private boolean isDebouncedEvent(String keyCombo) {
        long now = System.currentTimeMillis();
        Long lastTime = lastTriggerTime.get(keyCombo);

        if (lastTime != null && (now - lastTime) < DEBOUNCE_THRESHOLD_MS) {
            return true;
        }

        lastTriggerTime.put(keyCombo, now);
        return false;
    }

    /**
     * Executes a shortcut action with proper error handling.
     * 
     * @param shortcut The shortcut to execute
     * @param keyCombo The key combination (for logging)
     */
private void executeShortcut(Shortcut shortcut, String keyCombo) {
    try {
        // 1. Perform the System Action
        shortcut.getAction().execute();
        
        // 2. Notify the UI only if a shortcut was successfully triggered
        if (onKeyPressedListener != null) {
            // We pass the Action Name directly, not the raw key combo
            onKeyPressedListener.accept(shortcut.getAction().name());
        }

        if (statisticsEnabled.get()) {
            shortcutsTriggered++;
        }
    } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Error executing shortcut", ex);
    }
}

    /**
     * Updates internal statistics.
     */
    private void updateStatistics() {
        if (statisticsEnabled.get()) {
            eventsProcessed++;
            lastEventTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Configures JNativeHook's logging to reduce noise.
     */
    private void configureNativeHookLogging() {
        Logger nativeLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        nativeLogger.setLevel(Level.WARNING);
        nativeLogger.setUseParentHandlers(false);
    }

    /**
     * Enables statistics collection.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setStatisticsEnabled(boolean enabled) {
        this.statisticsEnabled.set(enabled);
        if (!enabled) {
            eventsProcessed = 0;
            shortcutsTriggered = 0;
            lastEventTimestamp = 0;
        }
    }

    /**
     * Gets service statistics.
     * 
     * @return Map containing statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enabled.get());
        stats.put("hooked", hooked.get());
        stats.put("shortcutsRegistered", shortcutMap.size());
        stats.put("eventsProcessed", eventsProcessed);
        stats.put("shortcutsTriggered", shortcutsTriggered);
        stats.put("lastEventTimestamp", lastEventTimestamp);
        return stats;
    }

    /**
     * Clears debounce cache. Useful for testing or manual reset.
     */
    public void clearDebounceCache() {
        lastTriggerTime.clear();
        LOGGER.info("Debounce cache cleared");
    }

    /**
     * Gets the number of registered shortcuts.
     * 
     * @return Number of shortcuts
     */
    public int getShortcutCount() {
        return shortcutMap.size();
    }

    /**
     * Sets a listener to be notified when a key combination is pressed.
     * 
     * @param listener The consumer to accept the key combo string
     */
    public void setOnKeyPressedListener(Consumer<String> listener) {
        this.onKeyPressedListener = listener;
    }
}