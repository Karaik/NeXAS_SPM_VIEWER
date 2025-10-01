package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.SpmEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnOs(OS.WINDOWS)
class SpmFileHandlerIntegrationTest {

    private static final AtomicBoolean TOOLKIT_INITIALIZED = new AtomicBoolean(false);

    @BeforeAll
    void initToolkit() throws Exception {
        if (TOOLKIT_INITIALIZED.get()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
        latch.await();
        TOOLKIT_INITIALIZED.set(true);
    }

    private <T> T runOnFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(60, TimeUnit.SECONDS);
    }

    private void runOnFxThread(Runnable runnable) throws Exception {
        runOnFxThread(() -> {
            runnable.run();
            return null;
        });
    }

    @Test
    void loadingTwoDirectoriesKeepsLatestResult() throws Exception {
        Path bsdxDir = Path.of("D:/BDY/bsdx_bhe/bsdx_resources");
        Path bheDir = Path.of("D:/BDY/bsdx_bhe/bhe_resources");
        Assumptions.assumeTrue(Files.isDirectory(bsdxDir), "bsdx 资源目录不存在，跳过测试");
        Assumptions.assumeTrue(Files.isDirectory(bheDir), "bhe 资源目录不存在，跳过测试");

        ObservableList<SpmEntry> spmList = FXCollections.observableArrayList();
        ProgressBar progressBar = runOnFxThread(() -> new ProgressBar());
        Label statusLabel = runOnFxThread(() -> new Label());
        SpmFileHandler handler = new SpmFileHandler(spmList, progressBar, statusLabel);

        CountDownLatch finishedLatch = new CountDownLatch(1);
        statusLabel.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && newText.contains(bheDir.getFileName().toString())) {
                finishedLatch.countDown();
            }
        });

        runOnFxThread(() -> {
            handler.loadDirectory(bsdxDir);
            handler.loadDirectory(bheDir);
        });

        assertTrue(finishedLatch.await(120, TimeUnit.SECONDS), "期待 bhe 目录加载完成");

        List<SpmEntry> snapshot = runOnFxThread(() -> List.copyOf(spmList));
        assertFalse(snapshot.isEmpty(), "bhe 目录应包含可解析的 SPM 文件");
        snapshot.forEach(entry -> assertTrue(entry.getPath().startsWith(bheDir),
                () -> "发现旧目录残留: " + entry.getPath()));
        assertFalse(runOnFxThread(progressBar::isVisible), "加载完成后进度条应隐藏");
    }
}
