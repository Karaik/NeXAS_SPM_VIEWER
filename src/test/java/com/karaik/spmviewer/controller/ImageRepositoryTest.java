package com.karaik.spmviewer.controller;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
class ImageRepositoryTest {

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
        return task.get(10, TimeUnit.SECONDS);
    }

    @Test
    void loadImageFallsBackToExtraRoots() throws Exception {
        ImageRepository repository = new ImageRepository();
        Path baseDir = Files.createTempDirectory("img-repo-base");
        Path extraDir = Files.createTempDirectory("img-repo-extra");
        try {
            Path asset = extraDir.resolve("sprite.png");
            writePlaceholderImage(asset);
            repository.setExtraSearchRoots(List.of(extraDir));

            Image image = repository.loadImage(baseDir, "sprite.png");
            assertNotNull(image, "Image should be located via extra search roots");
        } finally {
            deleteDirectory(extraDir);
            deleteDirectory(baseDir);
        }
    }

    @Test
    void loadImagesCachesResults() throws Exception {
        ImageRepository repository = new ImageRepository();
        Path baseDir = Files.createTempDirectory("img-repo-cache");
        try {
            Path asset = baseDir.resolve("cached.png");
            writePlaceholderImage(asset);

            Spm spm = new Spm();
            Spm.SPMImageData imageData = new Spm.SPMImageData();
            imageData.setImageName("cached.png");
            spm.setImageData(List.of(imageData));

            Image first = repository.loadImages(spm, baseDir).get(0);
            Image second = repository.loadImages(spm, baseDir).get(0);
            assertSame(first, second, "Repository should reuse cached images");
        } finally {
            deleteDirectory(baseDir);
        }
    }

    private void writePlaceholderImage(Path path) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", path.toFile());
    }

    private void deleteDirectory(Path dir) throws Exception {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
    }
}
