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

public class CanvasController {

    private static final double FIXED_CANVAS_SIZE = 4096; // 一个足够大的固定尺寸

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
        // 不再需要调整Canvas大小
        renderer.render(canvas, currentSpm, currentPageIndex, loadedImages, showCoords, showHitboxes);
    }

    public void previewImage(int imageIndex, String imageName) {
        this.currentPageIndex = -1;
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 绘制背景
        g.setFill(Color.rgb(30, 30, 30));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawCheckerBackground(g, canvas.getWidth(), canvas.getHeight());

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

    public void clearCanvas() {
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