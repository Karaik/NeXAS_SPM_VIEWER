package com.karaik.spmviewer.model;

import java.nio.charset.Charset;
import java.util.prefs.Preferences;

public class Settings {

    private static final String CHARSET_KEY = "charset";
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String PARSING_MODE_KEY = "parsingMode";
    private static final String DEFAULT_CHARSET = "windows-31j";

    /**
     * 定义SPM文件的解析模式（方言）。
     * 每种模式对应一种特定的文件结构，尤其是在HitArea部分。
     */
    public enum ParsingMode {
        // 方言A (多态版): SPM VER-2.00 (for bhe)
        VER_2_00_BHE("SPM VER-2.00 (bhe)"),
        // 方言C (旧版变种): SPM VER-2.00 (for bsdx)
        VER_2_00_BSDX("SPM VER-2.00 (bsdx)"),
        // 方言B (旧版标准): SPM VER-2.02
        VER_2_02("SPM VER-2.02");

        private final String displayName;

        ParsingMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static String currentCharsetName = DEFAULT_CHARSET;
    private static ParsingMode currentParsingMode = ParsingMode.VER_2_00_BHE;

    static {
        Preferences prefs = Preferences.userNodeForPackage(Settings.class);
        currentCharsetName = prefs.get(CHARSET_KEY, DEFAULT_CHARSET);

        String modeName = prefs.get(PARSING_MODE_KEY, ParsingMode.VER_2_00_BHE.name());
        try {
            currentParsingMode = ParsingMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            currentParsingMode = ParsingMode.VER_2_00_BHE;
        }
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
                Charset.forName(charsetName);
                currentCharsetName = charsetName;
                Preferences prefs = Preferences.userNodeForPackage(Settings.class);
                prefs.put(CHARSET_KEY, currentCharsetName);
            } catch (Exception e) {
                System.err.println("Invalid charset name provided: " + charsetName);
            }
        }
    }

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

    public static ParsingMode getParsingMode() {
        return currentParsingMode;
    }

    public static void setParsingMode(ParsingMode mode) {
        if (mode != null) {
            currentParsingMode = mode;
            Preferences prefs = Preferences.userNodeForPackage(Settings.class);
            prefs.put(PARSING_MODE_KEY, currentParsingMode.name());
        }
    }
}