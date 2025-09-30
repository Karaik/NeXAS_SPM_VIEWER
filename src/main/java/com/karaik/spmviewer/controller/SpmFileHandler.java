package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.parser.SpmParser;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SpmFileHandler {

    private final ObservableList<SpmEntry> spmList;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private Path currentDirectory;
    private long loadSequence = 0;

    public SpmFileHandler(
            ObservableList<SpmEntry> spmList,
            ProgressBar progressBar,
            Label statusLabel) {
        this.spmList = spmList;
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * 当用户切换解析模式时调用此方法，以重新加载当前目录。
     */
    public void reloadDirectory() {
        if (currentDirectory != null && Files.isDirectory(currentDirectory)) {
            loadDirectory(currentDirectory);
        }
    }

    public void openSpmDirectory(Window owner) {
        var dc = new DirectoryChooser();
        dc.setTitle("Open Directory with SPM files");
        String lastDir = Settings.getLastDirectory();
        if (lastDir != null) {
            File initialDir = new File(lastDir);
            if (initialDir.isDirectory()) {
                dc.setInitialDirectory(initialDir);
            }
        }
        File dir = dc.showDialog(owner);
        if (dir != null && dir.isDirectory()) {
            loadDirectory(dir.toPath());
        }
    }

    public void loadDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) return;

        final long mySeq = ++loadSequence;
        this.currentDirectory = directory;
        Settings.setLastDirectory(currentDirectory.toAbsolutePath().toString());

        final Settings.ParsingMode selectedMode = Settings.getParsingMode();
        Task<List<SpmEntry>> loadTask = createLoadTask(directory, selectedMode);

        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(loadTask.progressProperty());
        progressBar.setVisible(true);

        loadTask.setOnSucceeded(e -> {
            if (mySeq != loadSequence) {
                log.debug("Discarded stale directory load for {}", directory.getFileName());
                return;
            }
            spmList.setAll(loadTask.getValue());
            statusLabel.setText("Loaded " + spmList.size() + " SPM files from " + currentDirectory.getFileName());
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });

        loadTask.setOnFailed(e -> {
            if (mySeq != loadSequence) {
                log.debug("Discarded stale directory load failure for {}", directory.getFileName());
                return;
            }
            log.error("Failed to load SPM directory", loadTask.getException());
            statusLabel.setText("Error loading directory.");
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });

        Thread loaderThread = new Thread(loadTask, "spm-loader-" + mySeq);
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private Task<List<SpmEntry>> createLoadTask(Path directory, Settings.ParsingMode mode) {
        return new Task<>() {
            @Override
            protected List<SpmEntry> call() throws Exception {
                List<Path> spmFiles;
                try (Stream<Path> stream = Files.walk(directory, 1)) {
                    spmFiles = stream
                            .filter(p -> !Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".spm"))
                            .collect(Collectors.toList());
                }
                if (spmFiles.isEmpty()) {
                    updateMessage("No .spm files found in this directory.");
                    return Collections.emptyList();
                }

                final int total = spmFiles.size();
                List<SpmEntry> results = new ArrayList<>(total);
                var parser = new SpmParser();

                for (int i = 0; i < total; i++) {
                    Path spmPath = spmFiles.get(i);
                    updateProgress(i + 1.0, total);
                    updateMessage("Loading: " + spmPath.getFileName());

                    SpmEntry entry = new SpmEntry(spmPath);
                    try {
                        byte[] data = Files.readAllBytes(spmPath);
                        // 将解析模式传递给解析器
                        entry.setSpm(parser.parse(data, spmPath.getFileName().toString(), Settings.getCharsetName(), mode));
                        entry.setStatus(SpmEntry.Status.SUCCESS);
                    } catch (Exception ex) {
                        log.warn("Failed to parse SPM file: {}", spmPath, ex);
                        entry.setStatus(SpmEntry.Status.FAILED);
                        entry.setErrorMessage(ex.getMessage());
                    }
                    results.add(entry);
                }
                return results;
            }
        };
    }
}
