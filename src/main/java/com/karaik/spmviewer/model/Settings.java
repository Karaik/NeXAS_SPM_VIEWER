package com.karaik.spmviewer.model;

import javafx.scene.paint.Color;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.prefs.Preferences;

public class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private static final String CHARSET_KEY = "charset";
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String PARSING_MODE_KEY = "parsingMode";
    private static final String BG_MODE_KEY = "backgroundMode";
    private static final String BG_COLOR_KEY = "backgroundColor";
    private static final String AUTO_PLAY_KEY = "autoPlayAnimations";
    private static final String IMAGE_SEARCH_PATHS_KEY = "imageSearchPaths";
    private static final String ORIGIN_MODE_KEY = "originMode";
    private static final String ENV_IMAGE_PATHS = "SPM_IMAGE_PATHS";
    private static final String DEFAULT_CHARSET = "windows-31j";

    /**
     * 定义SPM文件的解析模式（方言）。
     * 版本自动按文件头识别，只需选择引擎类型。
     */
    public enum ParsingMode {
        BHE("BHE (auto detect version)"),
        BSDX("BSDX (auto detect version)");

        private final String displayName;
        ParsingMode(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum BackgroundMode {
        CHECKERBOARD,
        SOLID_COLOR
    }

    public enum OriginMode {
        CENTER("Center"),
        TOP_LEFT("Top-Left");

        private final String displayName;
        OriginMode(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    private static String currentCharsetName = DEFAULT_CHARSET;
    private static ParsingMode currentParsingMode = ParsingMode.BHE;
    private static BackgroundMode currentBackgroundMode = BackgroundMode.CHECKERBOARD;
    private static Color currentBackgroundColor = Color.rgb(30, 30, 30);
    private static boolean currentAutoPlayEnabled = true;
    private static List<Path> currentImageSearchRoots = new ArrayList<>();
    private static OriginMode currentOriginMode = OriginMode.CENTER;

    static {
        currentCharsetName = PREFS.get(CHARSET_KEY, DEFAULT_CHARSET);

        String modeName = PREFS.get(PARSING_MODE_KEY, ParsingMode.BHE.name());
        try {
            currentParsingMode = ParsingMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            currentParsingMode = ParsingMode.BHE;
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

        currentImageSearchRoots = parseSearchPaths(PREFS.get(IMAGE_SEARCH_PATHS_KEY, ""));
        String envPaths = System.getenv(ENV_IMAGE_PATHS);
        if (envPaths != null && !envPaths.isBlank()) {
            List<Path> merged = new ArrayList<>(new LinkedHashSet<>(currentImageSearchRoots));
            merged.addAll(parseSearchPaths(envPaths));
            currentImageSearchRoots = normalizePaths(merged);
        } else {
            currentImageSearchRoots = normalizePaths(currentImageSearchRoots);
        }

        String originName = PREFS.get(ORIGIN_MODE_KEY, OriginMode.CENTER.name());
        try {
            currentOriginMode = OriginMode.valueOf(originName);
        } catch (IllegalArgumentException e) {
            currentOriginMode = OriginMode.CENTER;
        }
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

    public static List<Path> getImageSearchRoots() {
        return currentImageSearchRoots;
    }

    public static void setImageSearchRoots(List<Path> roots) {
        currentImageSearchRoots = normalizePaths(roots);
        PREFS.put(IMAGE_SEARCH_PATHS_KEY, formatSearchPaths(currentImageSearchRoots));
    }

    public static void addImageSearchRoot(Path root) {
        if (root == null) {
            return;
        }
        List<Path> merged = new ArrayList<>(currentImageSearchRoots);
        merged.add(root);
        setImageSearchRoots(merged);
    }

    public static OriginMode getOriginMode() {
        return currentOriginMode;
    }

    public static void setOriginMode(OriginMode mode) {
        if (mode != null) {
            currentOriginMode = mode;
            PREFS.put(ORIGIN_MODE_KEY, mode.name());
        }
    }

    public static boolean isAutoPlayEnabled() {
        return currentAutoPlayEnabled;
    }

    private static List<Path> parseSearchPaths(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split("[;\n]+");
        List<Path> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Paths.get(trimmed));
            }
        }
        return result;
    }

    private static List<Path> normalizePaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Path> unique = new LinkedHashSet<>();
        for (Path path : paths) {
            if (path != null) {
                unique.add(path.toAbsolutePath().normalize());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(unique));
    }

    private static String formatSearchPaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Path path : paths) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(path);
        }
        return builder.toString();
    }    public static void setAutoPlayEnabled(boolean enabled) {
        currentAutoPlayEnabled = enabled;
        PREFS.putBoolean(AUTO_PLAY_KEY, enabled);
    }
}
