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
    // ... 构造函数和 getter 不变 ...

    private final ObservableList<SpmEntry> spmList;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private Path currentDirectory;

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

    public void openSpmDirectory(Window owner) {
        var dc = new DirectoryChooser();
        dc.setTitle("打开包含SPM文件的目录");
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
        this.currentDirectory = directory;
        Settings.setLastDirectory(currentDirectory.toAbsolutePath().toString());

        Task<List<SpmEntry>> loadTask = createLoadTask(currentDirectory);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        progressBar.setVisible(true);

        loadTask.setOnSucceeded(e -> {
            spmList.setAll(loadTask.getValue());
            statusLabel.setText("从 " + currentDirectory.getFileName() + " 加载了 " + spmList.size() + " 个SPM文件");
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });
        loadTask.setOnFailed(e -> {
            log.error("加载SPM目录失败", loadTask.getException());
            statusLabel.setText("加载目录时出错。");
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
        });
        new Thread(loadTask).start();
    }

    private Task<List<SpmEntry>> createLoadTask(Path directory) {
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
                    updateMessage("在此目录中未找到 .spm 文件。");
                    return Collections.emptyList();
                }
                final int total = spmFiles.size();
                List<SpmEntry> results = new ArrayList<>(total);
                var parser = new SpmParser();

                for (int i = 0; i < total; i++) {
                    Path spmPath = spmFiles.get(i);
                    updateProgress(i + 1.0, total);
                    updateMessage("正在加载: " + spmPath.getFileName());

                    SpmEntry entry = new SpmEntry(spmPath);
                    try {
                        byte[] data = Files.readAllBytes(spmPath);
                        entry.setSpm(parser.parse(data, spmPath.getFileName().toString(), Settings.getCharsetName()));
                        entry.setStatus(SpmEntry.Status.SUCCESS);
                    } catch (Exception ex) {
                        log.warn("解析SPM文件失败: {}", spmPath, ex);
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