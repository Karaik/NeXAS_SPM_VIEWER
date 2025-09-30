package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.spm.Spm;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanvasControllerTest {

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

    @Test
    void loadImagesPreservesIndexOrdering() throws Exception {
        CanvasController controller = new CanvasController(new Canvas(), new Group(), new Scale());

        Path tempDir = Files.createTempDirectory("canvas-controller-test");
        try {
            Path existing = tempDir.resolve("existing.png");
            BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            ImageIO.write(bufferedImage, "png", existing.toFile());

            Spm spm = new Spm();
            Spm.SPMImageData imageA = new Spm.SPMImageData();
            imageA.setImageName("existing.png");
            Spm.SPMImageData imageB = new Spm.SPMImageData();
            imageB.setImageName("missing.png");
            spm.setImageData(List.of(imageA, imageB));

            List<Image> loaded = controller.loadImages(spm, tempDir);
            assertEquals(2, loaded.size(), "Should produce placeholder entries for each declared image");
            assertNotNull(loaded.get(0), "Existing asset should be loaded");
            assertNull(loaded.get(1), "Missing asset should map to null placeholder");
        } finally {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void setCurrentSpmCopiesLoadedImages() {
        CanvasController controller = new CanvasController(new Canvas(), new Group(), new Scale());
        Spm spm = new Spm();

        List<Image> seed = List.of(new WritableImage(1, 1));
        controller.setCurrentSpm(spm, seed);

        assertEquals(1, Optional.ofNullable(controller.getLoadedImages()).map(List::size).orElse(0));
        assertNotSame(seed, controller.getLoadedImages(), "Internal cache should not alias caller list");
    }
}
