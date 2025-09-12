package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.parser.SpmParser;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SpmFileHandler {

    private final ListView<SpmEntry> spmListView;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private Path currentDirectory;

    public SpmFileHandler(ListView<SpmEntry> spmListView, ProgressBar progressBar, Label statusLabel) {
        this.spmListView = spmListView;
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void openSpmDirectory(Window owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Open Directory with SPM files");
        File dir = dc.showDialog(owner);
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        currentDirectory = dir.toPath();
        Task<Void> loadTask = createLoadTask(currentDirectory);

        progressBar.progressProperty().bind(loadTask.progressProperty());
        progressBar.setVisible(true);

        loadTask.setOnSucceeded(e -> {
            statusLabel.setText("Loaded " + spmListView.getItems().size() + " SPM files from " + currentDirectory);
            progressBar.setVisible(false);
        });

        loadTask.setOnFailed(e -> {
            log.error("Failed to load SPM directory", loadTask.getException());
            statusLabel.setText("Error loading directory.");
            progressBar.setVisible(false);
        });

        new Thread(loadTask).start();
    }

    private Task<Void> createLoadTask(Path directory) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Path> spmFiles;
                try (var stream = Files.walk(directory, 1)) {
                    spmFiles = stream
                            .filter(p -> !Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".spm"))
                            .collect(Collectors.toList());
                }

                if (spmFiles.isEmpty()) {
                    updateMessage("No .spm files found in this directory.");
                    return null;
                }

                final int total = spmFiles.size();
                updateProgress(0, total);

                javafx.application.Platform.runLater(() -> spmListView.getItems().clear());

                for (int i = 0; i < total; i++) {
                    Path spmPath = spmFiles.get(i);
                    SpmEntry entry = new SpmEntry(spmPath);
                    SpmParser parser = new SpmParser();
                    try {
                        byte[] data = Files.readAllBytes(spmPath);
                        entry.setSpm(parser.parse(data, spmPath.getFileName().toString(), "windows-31j"));
                        entry.setStatus(SpmEntry.Status.SUCCESS);
                    } catch (Exception ex) {
                        log.warn("Failed to parse SPM file: {}", spmPath, ex);
                        entry.setStatus(SpmEntry.Status.FAILED);
                        entry.setErrorMessage(ex.getMessage());
                    }

                    final SpmEntry finalEntry = entry;
                    javafx.application.Platform.runLater(() -> spmListView.getItems().add(finalEntry));
                    updateProgress(i + 1, total);
                    updateMessage("Loading: " + spmPath.getFileName());
                }
                return null;
            }
        };
    }
}