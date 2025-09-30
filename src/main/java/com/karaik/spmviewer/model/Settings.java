package com.karaik.spmviewer.model;

import javafx.scene.paint.Color;

import java.nio.charset.Charset;
import java.util.prefs.Preferences;

public class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private static final String CHARSET_KEY = "charset";
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String PARSING_MODE_KEY = "parsingMode";
    private static final String BG_MODE_KEY = "backgroundMode";
    private static final String BG_COLOR_KEY = "backgroundColor";
    private static final String AUTO_PLAY_KEY = "autoPlayAnimations";
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
        ParsingMode(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum BackgroundMode {
        CHECKERBOARD,
        SOLID_COLOR
    }

    private static String currentCharsetName = DEFAULT_CHARSET;
    private static ParsingMode currentParsingMode = ParsingMode.VER_2_00_BHE;
    private static BackgroundMode currentBackgroundMode = BackgroundMode.CHECKERBOARD;
    private static Color currentBackgroundColor = Color.rgb(30, 30, 30);
    private static boolean currentAutoPlayEnabled = true;

    static {
        currentCharsetName = PREFS.get(CHARSET_KEY, DEFAULT_CHARSET);

        String modeName = PREFS.get(PARSING_MODE_KEY, ParsingMode.VER_2_00_BHE.name());
        try {
            currentParsingMode = ParsingMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            currentParsingMode = ParsingMode.VER_2_00_BHE;
        }

        // Background settings
        String bgModeName = PREFS.get(BG_MODE_KEY, BackgroundMode.CHECKERBOARD.name());
        try {
            currentBackgroundMode = BackgroundMode.valueOf(bgModeName);
        } catch (IllegalArgumentException e) {
            currentBackgroundMode = BackgroundMode.CHECKERBOARD;
        }

        String colorString = PREFS.get(BG_COLOR_KEY, Color.rgb(30, 30, 30).toString());
        try {
            currentBackgroundColor = Color.web(colorString);
        } catch (Exception e) {
            currentBackgroundColor = Color.rgb(30, 30, 30);
        }

        currentAutoPlayEnabled = PREFS.getBoolean(AUTO_PLAY_KEY, true);
    }

    public static String getCharsetName() { return currentCharsetName; }
    public static Charset getCharset() {
        try { return Charset.forName(currentCharsetName); }
        catch (Exception e) { return Charset.forName(DEFAULT_CHARSET); }
    }
    public static void setCharsetName(String charsetName) {
        if (charsetName != null && !charsetName.isEmpty()) {
            try {
                Charset.forName(charsetName);
                currentCharsetName = charsetName;
                PREFS.put(CHARSET_KEY, currentCharsetName);
            } catch (Exception e) {
                System.err.println("Invalid charset name provided: " + charsetName);
            }
        }
    }

    public static String getLastDirectory() {
        return PREFS.get(LAST_DIRECTORY_KEY, null);
    }

    public static void setLastDirectory(String path) {
        if (path != null && !path.isEmpty()) {
            PREFS.put(LAST_DIRECTORY_KEY, path);
        }
    }

    public static ParsingMode getParsingMode() { return currentParsingMode; }
    public static void setParsingMode(ParsingMode mode) {
        if (mode != null) {
            currentParsingMode = mode;
            PREFS.put(PARSING_MODE_KEY, currentParsingMode.name());
        }
    }

    public static BackgroundMode getBackgroundMode() { return currentBackgroundMode; }
    public static void setBackgroundMode(BackgroundMode mode) {
        if (mode != null) {
            currentBackgroundMode = mode;
            PREFS.put(BG_MODE_KEY, currentBackgroundMode.name());
        }
    }

    public static Color getBackgroundColor() { return currentBackgroundColor; }
    public static void setBackgroundColor(Color color) {
        if (color != null) {
            currentBackgroundColor = color;
            PREFS.put(BG_COLOR_KEY, color.toString());
        }
    }

    public static boolean isAutoPlayEnabled() {
        return currentAutoPlayEnabled;
    }

    public static void setAutoPlayEnabled(boolean enabled) {
        currentAutoPlayEnabled = enabled;
        PREFS.putBoolean(AUTO_PLAY_KEY, enabled);
    }
}
