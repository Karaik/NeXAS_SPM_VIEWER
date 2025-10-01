package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
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

    private <T> T runOnFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private void runOnFxThread(Runnable runnable) throws Exception {
        runOnFxThread(() -> {
            runnable.run();
            return null;
        });
    }

    @Test
    void loadImagesPreservesIndexOrderingAndCaching() throws Exception {
        ImageRepository repository = new ImageRepository();
        CanvasController controller = runOnFxThread(() -> new CanvasController(new Canvas(), new Group(), new Scale()));

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

            List<Image> loaded = repository.loadImages(spm, tempDir);
            assertEquals(2, loaded.size(), "Should produce placeholder entries for each declared image");
            assertNotNull(loaded.get(0), "Existing asset should be loaded");
            assertNull(loaded.get(1), "Missing asset should map to null placeholder");

            List<Image> cached = repository.loadImages(spm, tempDir);
            assertSame(loaded.get(0), cached.get(0), "Loaded image should reuse cache");
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
    void setCurrentSpmCopiesLoadedImages() throws Exception {
        CanvasController controller = runOnFxThread(() -> new CanvasController(new Canvas(), new Group(), new Scale()));
        Spm spm = new Spm();

        List<Image> seed = List.of(new WritableImage(1, 1));
        runOnFxThread(() -> { controller.setCurrentSpm(spm, seed); return null; });

        assertEquals(1, Optional.ofNullable(controller.getLoadedImages()).map(List::size).orElse(0));
        assertNotSame(seed, controller.getLoadedImages(), "Internal cache should not alias caller list");
    }

    @Test
    void canvasKeepsPreferredSizeWhenSwitchingPages() throws Exception {
        CanvasController controller = runOnFxThread(() -> new CanvasController(new Canvas(), new Group(), new Scale()));

        Spm spm = new Spm();
        Spm.SPMPageData small = new Spm.SPMPageData();
        small.setPageWidth(64);
        small.setPageHeight(64);
        Spm.SPMPageData large = new Spm.SPMPageData();
        large.setPageWidth(256);
        large.setPageHeight(256);
        spm.setPageData(List.of(small, large));

        runOnFxThread(() -> controller.setCurrentSpm(spm, List.of()));

        double widthAfterLoad = runOnFxThread(() -> controller.getCanvas().getWidth());
        runOnFxThread(() -> controller.setCurrentPageIndex(1));
        double widthAfterLarge = runOnFxThread(() -> controller.getCanvas().getWidth());
        runOnFxThread(() -> controller.setCurrentPageIndex(0));
        double widthAfterSmall = runOnFxThread(() -> controller.getCanvas().getWidth());

        assertEquals(widthAfterLoad, widthAfterLarge, 0.001, "Loading page 1 should match preferred size");
        assertEquals(widthAfterLarge, widthAfterSmall, 0.001, "Switching to smaller page must not shrink canvas");
    }

    @Test
    void pageExtentsRespectChipBounds() throws Exception {
        CanvasController controller = runOnFxThread(() -> new CanvasController(new Canvas(), new Group(), new Scale()));
        Spm spm = new Spm();
        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setPageRect(rect(-32, -16, 96, 48));
        Spm.SPMChipData chip = new Spm.SPMChipData();
        chip.setDstRect(rect(10, 20, 70, 90));
        page.setChipData(List.of(chip));
        spm.setPageData(List.of(page));

        runOnFxThread(() -> controller.setCurrentSpm(spm, List.of()));
        runOnFxThread(() -> controller.setCurrentPageIndex(0));

        CanvasController.PageExtents extents = controller.getCurrentPageExtents();
        assertNotNull(extents, "Extents should be available after selecting a page");
        assertTrue(extents.getWidth() >= 128, "Extents width should include page rect and chip span");
        assertTrue(extents.getHeight() >= 64, "Extents height should include page rect and chip span");
    }

    @Test
    void wrapLineSplitsLongMessagesWithinBounds() throws Exception {
        WrapCheck check = runOnFxThread(() -> {
            CanvasController controller = new CanvasController(new Canvas(), new Group(), new Scale());
            Method wrapLine = CanvasController.class.getDeclaredMethod("wrapLine", String.class, Font.class, double.class);
            wrapLine.setAccessible(true);
            Font font = Font.font("System", FontWeight.BOLD, 28);
            String text = "ȱʧ��Դ��ʾ��Ҫ���ݻ��������Զ����У���ֹ���ֱ�����" +
                    "�������Ҫ��֤�����߼��������޿�������ȷ���";
            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) wrapLine.invoke(controller, text, font, 150.0);
            Text measure = new Text();
            measure.setFont(font);
            boolean within = true;
            for (String line : lines) {
                measure.setText(line);
                if (measure.getLayoutBounds().getWidth() > 151.0) {
                    within = false;
                    break;
                }
            }
            return new WrapCheck(lines.size(), within);
        });

        assertTrue(check.lineCount() > 1, "���ı�Ӧ����ɶ���");
        assertTrue(check.withinBounds(), "ÿ�п��ȱ������������ֵ");
    }

    @Test
    void missingImageBannerRespectsCanvasRatio() throws Exception {
        BannerMetrics metrics = runOnFxThread(() -> {
            CanvasController controller = new CanvasController(new Canvas(), new Group(), new Scale());
            controller.getCanvas().setWidth(800);
            controller.getCanvas().setHeight(600);
            controller.previewImage(42,
                    "δ�ҵ�����Դ�ļ����Ʒǳ��ǳ�������Ҫ�ڻ�����������չʾ",
                    Settings.BackgroundMode.SOLID_COLOR,
                    Color.rgb(240, 240, 240));

            WritableImage snapshot = controller.getCanvas().snapshot(null, null);
            var reader = snapshot.getPixelReader();
            int width = (int) snapshot.getWidth();
            int height = (int) snapshot.getHeight();
            int minX = width;
            int maxX = -1;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color = reader.getColor(x, y);
                    if (color.getOpacity() > 0.5 && color.getRed() > 0.5 && color.getGreen() < 0.4) {
                        if (x < minX) {
                            minX = x;
                        }
                        if (x > maxX) {
                            maxX = x;
                        }
                    }
                }
            }
            double bannerWidth = maxX >= minX ? (maxX - minX + 1) : 0;
            boolean hasBanner = maxX >= minX;
            return new BannerMetrics(controller.getCanvas().getWidth(), bannerWidth, hasBanner);
        });

        assertTrue(metrics.hasBanner(), "ȱͼ��ʾӦ�����ڻ�����");
        assertTrue(metrics.bannerWidth() <= metrics.canvasWidth() * 0.8 + 2,
                "��ʾ�����Ӧ�����ڻ���80%����");
    }

    private Spm.SPMRect rect(int left, int top, int right, int bottom) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(left);
        rect.setTop(top);
        rect.setRight(right);
        rect.setBottom(bottom);
        return rect;
    }

    private record WrapCheck(int lineCount, boolean withinBounds) {}

    private record BannerMetrics(double canvasWidth, double bannerWidth, boolean hasBanner) {}
}
