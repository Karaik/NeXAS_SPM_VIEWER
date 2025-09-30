package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.controller.render.SpmRenderer;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class CanvasController {

    private static final double FIXED_CANVAS_SIZE = 4096;

    private final Canvas canvas;
    private final SpmRenderer renderer = new SpmRenderer();
    private final List<Image> loadedImages = new ArrayList<>();

    private Spm currentSpm;
    private int currentPageIndex = -1;

    public CanvasController(Canvas canvas, Group canvasGroup, Scale scale) {
        this.canvas = canvas;
        canvas.setWidth(FIXED_CANVAS_SIZE);
        canvas.setHeight(FIXED_CANVAS_SIZE);
        canvas.getTransforms().add(scale);
        canvasGroup.getChildren().add(canvas);
    }

    public void setCurrentSpm(Spm spm, List<Image> images) {
        this.currentSpm = spm;
        this.currentPageIndex = -1;
        loadedImages.clear();
        if (images != null) {
            loadedImages.addAll(images);
        }
    }

    public List<Image> loadImages(Spm spm, Path dir) {
        List<Image> images = new ArrayList<>();
        if (spm == null || spm.getImageData() == null) {
            return images;
        }
        for (Spm.SPMImageData imageData : spm.getImageData()) {
            images.add(loadImage(dir, imageData.getImageName()));
        }
        return images;
    }

    public void renderPage(boolean showCoords, boolean showHitboxes, Settings.BackgroundMode bgMode, Color bgColor) {
        if (currentSpm == null || currentPageIndex < 0 || currentSpm.getPageData() == null || currentPageIndex >= currentSpm.getPageData().size()) {
            clearCanvas(bgMode, bgColor);
            return;
        }
        renderer.render(canvas, currentSpm, currentPageIndex, loadedImages, showCoords, showHitboxes, bgMode, bgColor);
    }

    public void previewImage(int imageIndex, String imageName, Settings.BackgroundMode bgMode, Color bgColor) {
        this.currentPageIndex = -1;
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 绘制背景
        renderer.drawBackground(g, canvas.getWidth(), canvas.getHeight(), bgMode, bgColor);

        if (imageIndex < 0 || imageIndex >= loadedImages.size()) return;

        Image img = loadedImages.get(imageIndex);
        if (img == null) {
            g.setFill(Color.RED);
            g.fillText("Image not loaded: " + imageName, 20, 20);
            return;
        }

        // 将图片绘制在画布中心
        double x = (canvas.getWidth() - img.getWidth()) / 2.0;
        double y = (canvas.getHeight() - img.getHeight()) / 2.0;
        g.drawImage(img, x, y);
    }

    public void clearCanvas(Settings.BackgroundMode bgMode, Color bgColor) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        renderer.drawBackground(g, canvas.getWidth(), canvas.getHeight(), bgMode, bgColor);
    }

    private Image loadImage(Path dir, String name) {
        if (dir == null || name == null || name.isBlank()) {
            return null;
        }
        Path path = dir.resolve(name);
        if (!Files.exists(path)) {
            log.debug("Image asset {} not found under {}", name, dir);
            return null;
        }
        try (InputStream input = Files.newInputStream(path)) {
            return new Image(input);
        } catch (Exception ex) {
            log.warn("Failed to load image {}", path.getFileName(), ex);
            return null;
        }
    }
}
