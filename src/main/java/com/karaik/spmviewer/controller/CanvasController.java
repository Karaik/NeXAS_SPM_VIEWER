package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.controller.render.SpmRenderer;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CanvasController {

    private final Canvas canvas;
    private final SpmRenderer renderer = new SpmRenderer();
    private final List<Image> loadedImages = new ArrayList<>();

    private Spm currentSpm;
    private int currentPageIndex = -1;

    public CanvasController(Canvas canvas, Group canvasGroup, Scale scale) {
        this.canvas = canvas;
        canvas.getTransforms().add(scale);
        canvasGroup.getChildren().add(canvas);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCurrentSpm(Spm spm, Path imageDirectory) {
        this.currentSpm = spm;
        this.currentPageIndex = -1;
        loadImages(imageDirectory);
    }

    public void setCurrentPageIndex(int index) {
        this.currentPageIndex = index;
    }

    /**
     * 渲染当前选定的页面，并根据传入的选项决定是否绘制额外信息。
     * @param showCoords  是否显示坐标系
     * @param showHitboxes 是否显示Hitbox
     */
    public void renderPage(boolean showCoords, boolean showHitboxes) {
        if (currentSpm == null || currentPageIndex < 0 || currentSpm.getPageData() == null || currentPageIndex >= currentSpm.getPageData().size()) {
            clearCanvas();
            return;
        }
        var p = currentSpm.getPageData().get(currentPageIndex);

        int w = Math.max(1, Optional.ofNullable(p.getPageWidth()).orElse(0));
        int h = Math.max(1, Optional.ofNullable(p.getPageHeight()).orElse(0));

        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }

        renderer.render(canvas, currentSpm, currentPageIndex, loadedImages, showCoords, showHitboxes);
    }

    public void previewImage(int imageIndex, String imageName) {
        this.currentPageIndex = -1;
        if (imageIndex < 0 || imageIndex >= loadedImages.size()) return;

        Image img = loadedImages.get(imageIndex);
        if (img == null) {
            canvas.setWidth(256);
            canvas.setHeight(256);
            GraphicsContext g = canvas.getGraphicsContext2D();
            drawCheckerBackground(g, 256, 256);
            g.setFill(Color.RED);
            g.fillText("Image not loaded: " + imageName, 20, 20);
            return;
        }

        canvas.setWidth(img.getWidth());
        canvas.setHeight(img.getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        drawCheckerBackground(g, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0);
    }

    public void clearCanvas() {
        canvas.setWidth(1024);
        canvas.setHeight(768);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.rgb(30, 30, 30));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawCheckerBackground(g, canvas.getWidth(), canvas.getHeight());
    }

    private void loadImages(Path dir) {
        loadedImages.clear();
        if (currentSpm == null || currentSpm.getImageData() == null || dir == null) return;

        for (Spm.SPMImageData imageData : currentSpm.getImageData()) {
            Image loadedImage = null;
            String name = imageData.getImageName();
            if (name != null && !name.isEmpty()) {
                Path p = dir.resolve(name);
                if (Files.exists(p)) {
                    try {
                        loadedImage = new Image(new FileInputStream(p.toFile()));
                    } catch (Exception ignored) {}
                }
            }
            loadedImages.add(loadedImage);
        }
    }

    private void drawCheckerBackground(GraphicsContext g, double w, double h) {
        int s = 16;
        for (int y = 0; y < h; y += s) {
            for (int x = 0; x < w; x += s) {
                boolean odd = ((x / s) + (y / s)) % 2 == 1;
                g.setFill(odd ? Color.gray(0.85) : Color.gray(0.92));
                g.fillRect(x, y, s, s);
            }
        }
    }
}