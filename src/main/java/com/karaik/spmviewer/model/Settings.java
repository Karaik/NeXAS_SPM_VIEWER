package com.karaik.spmviewer.model;

import java.nio.charset.Charset;
import java.util.prefs.Preferences;

public class Settings {

    private static final String CHARSET_KEY = "charset";
    private static final String LAST_DIRECTORY_KEY = "lastDirectory"; // ADDED
    private static final String DEFAULT_CHARSET = "windows-31j";

    private static String currentCharsetName = DEFAULT_CHARSET;

    // Load settings from persistent storage when the class is loaded
    static {
        Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        currentCharsetName = prefs.get(CHARSET_KEY, DEFAULT_CHARSET);
    }

    public static String getCharsetName() {
        return currentCharsetName;
    }

    public static Charset getCharset() {
        try {
            return Charset.forName(currentCharsetName);
        } catch (Exception e) {
            return Charset.forName(DEFAULT_CHARSET);
        }
    }

    public static void setCharsetName(String charsetName) {
        if (charsetName != null && !charsetName.isEmpty()) {
            try {
                // Verify that the charset is valid before setting it
                Charset.forName(charsetName);
                currentCharsetName = charsetName;
                // Save to persistent storage
                Preferences prefs = Preferences.userNodeForPackage(Settings.class);
                prefs.put(CHARSET_KEY, currentCharsetName);
            } catch (Exception e) {
                System.err.println("Invalid charset name provided: " + charsetName);
            }
        }
    }

    // --- ADDED: Methods for last directory memory ---

    public static String getLastDirectory() {
        Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        return prefs.get(LAST_DIRECTORY_KEY, null);
    }

    public static void setLastDirectory(String path) {
        if (path != null && !path.isEmpty()) {
            Preferences prefs = Preferences.userNodeForPackage(Settings.class);
            prefs.put(LAST_DIRECTORY_KEY, path);
        }
    }
}