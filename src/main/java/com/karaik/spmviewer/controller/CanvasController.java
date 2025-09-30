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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Data
public class CanvasController {

    private static final double MIN_CANVAS_SIZE = 256;
    private static final double MAX_CANVAS_SIZE = 4096;
    private static final double CONTENT_PADDING = 64;
    private static final double MIN_CONTENT_SIZE = 16;
    private static final double MESSAGE_MAX_WIDTH_RATIO = 0.8;

    private final Canvas canvas;
    private final SpmRenderer renderer = new SpmRenderer();
    private final List<Image> loadedImages = new ArrayList<>();

    private Spm currentSpm;
    private int currentPageIndex = -1;
    private double preferredContentWidth = MIN_CONTENT_SIZE;
    private double preferredContentHeight = MIN_CONTENT_SIZE;

    public CanvasController(Canvas canvas, Group canvasGroup, Scale scale) {
        this.canvas = canvas;
        resizeCanvas(MIN_CANVAS_SIZE - CONTENT_PADDING * 2, MIN_CANVAS_SIZE - CONTENT_PADDING * 2);
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
        recomputePreferredContentSize(spm);
        resizeCanvas(preferredContentWidth, preferredContentHeight);
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
        adjustCanvasForPage(currentSpm, currentPageIndex);
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
        Image img = (imageIndex >= 0 && imageIndex < loadedImages.size()) ? loadedImages.get(imageIndex) : null;
        adjustCanvasForImage(img);

        GraphicsContext g = canvas.getGraphicsContext2D();
        renderer.drawBackground(g, canvas.getWidth(), canvas.getHeight(), bgMode, bgColor);

        if (img == null) {
            drawMissingImageMessage(g, imageName, String.format("Image index: %d", imageIndex));
            return;
        }

        double x = (canvas.getWidth() - img.getWidth()) / 2.0;
        double y = (canvas.getHeight() - img.getHeight()) / 2.0;
        g.drawImage(img, x, y);
    }

    public void clearCanvas(Settings.BackgroundMode bgMode, Color bgColor) {
        if (currentSpm == null) {
            resizeCanvas(MIN_CONTENT_SIZE, MIN_CONTENT_SIZE);
        } else if (currentPageIndex < 0) {
            resizeCanvas(preferredContentWidth, preferredContentHeight);
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        renderer.drawBackground(g, canvas.getWidth(), canvas.getHeight(), bgMode, bgColor);
    }

    private void recomputePreferredContentSize(Spm spm) {
        preferredContentWidth = MIN_CONTENT_SIZE;
        preferredContentHeight = MIN_CONTENT_SIZE;
        if (spm == null) {
            return;
        }
        for (Spm.SPMPageData page : Optional.ofNullable(spm.getPageData()).orElse(List.of())) {
            double[] size = computePageExtents(page);
            preferredContentWidth = Math.max(preferredContentWidth, size[0]);
            preferredContentHeight = Math.max(preferredContentHeight, size[1]);
        }
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

    private void adjustCanvasForImage(Image image) {
        if (image == null) {
            resizeCanvas(MIN_CONTENT_SIZE, MIN_CONTENT_SIZE);
            return;
        }
        resizeCanvas(Math.max(image.getWidth(), MIN_CONTENT_SIZE), Math.max(image.getHeight(), MIN_CONTENT_SIZE));
    }

    private void adjustCanvasForPage(Spm spm, int pageIndex) {
        if (spm == null || pageIndex < 0 || spm.getPageData() == null || pageIndex >= spm.getPageData().size()) {
            return;
        }
        double[] size = computePageExtents(spm.getPageData().get(pageIndex));
        double contentWidth = Math.max(preferredContentWidth, size[0]);
        double contentHeight = Math.max(preferredContentHeight, size[1]);
        resizeCanvas(contentWidth, contentHeight);
    }

    private void resizeCanvas(double contentWidth, double contentHeight) {
        double targetWidth = clamp(contentWidth + CONTENT_PADDING * 2, MIN_CANVAS_SIZE, MAX_CANVAS_SIZE);
        double targetHeight = clamp(contentHeight + CONTENT_PADDING * 2, MIN_CANVAS_SIZE, MAX_CANVAS_SIZE);
        if (canvas.getWidth() != targetWidth) {
            canvas.setWidth(targetWidth);
        }
        if (canvas.getHeight() != targetHeight) {
            canvas.setHeight(targetHeight);
        }
    }

    private double[] computePageExtents(Spm.SPMPageData page) {
        double originX = safe(page.getRotateCenterX());
        double originY = safe(page.getRotateCenterY());
        double minX = originX;
        double maxX = originX;
        double minY = originY;
        double maxY = originY;

        Spm.SPMRect pageRect = page.getPageRect();
        if (pageRect != null) {
            minX = Math.min(minX, Math.min(safe(pageRect.getLeft()), safe(pageRect.getRight())));
            maxX = Math.max(maxX, Math.max(safe(pageRect.getLeft()), safe(pageRect.getRight())));
            minY = Math.min(minY, Math.min(safe(pageRect.getTop()), safe(pageRect.getBottom())));
            maxY = Math.max(maxY, Math.max(safe(pageRect.getTop()), safe(pageRect.getBottom())));
        }

        for (Spm.SPMChipData chip : Optional.ofNullable(page.getChipData()).orElse(List.of())) {
            Spm.SPMRect dst = chip.getDstRect();
            if (dst == null) continue;
            double left = safe(dst.getLeft());
            double right = safe(dst.getRight());
            double top = safe(dst.getTop());
            double bottom = safe(dst.getBottom());
            minX = Math.min(minX, Math.min(left, right));
            maxX = Math.max(maxX, Math.max(left, right));
            minY = Math.min(minY, Math.min(top, bottom));
            maxY = Math.max(maxY, Math.max(top, bottom));
        }

        double pageWidth = safe(page.getPageWidth());
        if (pageWidth > 0) {
            double half = pageWidth / 2.0;
            minX = Math.min(minX, originX - half);
            maxX = Math.max(maxX, originX + half);
        }
        double pageHeight = safe(page.getPageHeight());
        if (pageHeight > 0) {
            double half = pageHeight / 2.0;
            minY = Math.min(minY, originY - half);
            maxY = Math.max(maxY, originY + half);
        }

        double width = Math.max(MIN_CONTENT_SIZE, maxX - minX);
        double height = Math.max(MIN_CONTENT_SIZE, maxY - minY);
        return new double[]{width, height};
    }

    private void drawMissingImageMessage(GraphicsContext g, String imageName) {
        drawMissingImageMessage(g, imageName, (String[]) null);
    }

    private void drawMissingImageMessage(GraphicsContext g, String imageName, String... extraLines) {
        double padding = 24;
        double maxBoxWidth = Math.max(canvas.getWidth() * MESSAGE_MAX_WIDTH_RATIO, MIN_CANVAS_SIZE);
        Font font = Font.font("System", FontWeight.BOLD, 28);

        List<String> lines = new ArrayList<>();
        String base = "Image not found";
        if (imageName != null && !imageName.isBlank()) {
            base += ": " + imageName;
        }
        lines.addAll(wrapLine(base, font, maxBoxWidth - padding * 2));
        if (lines.isEmpty()) {
            lines.add(base);
        }

        if (extraLines != null) {
            for (String detail : extraLines) {
                if (detail == null || detail.isBlank()) {
                    continue;
                }
                lines.addAll(wrapLine(detail.trim(), font, maxBoxWidth - padding * 2));
            }
        }

        if (lines.isEmpty()) {
            lines.add("Image not found");
        }

        Text metrics = new Text("Ag");
        metrics.setFont(font);
        double lineHeight = metrics.getLayoutBounds().getHeight();
        double baseline = metrics.getBaselineOffset();

        double maxLineWidth = 0;
        Text widthProbe = new Text();
        widthProbe.setFont(font);
        for (String line : lines) {
            widthProbe.setText(line);
            maxLineWidth = Math.max(maxLineWidth, widthProbe.getLayoutBounds().getWidth());
        }

        double boxWidth = Math.min(maxBoxWidth, maxLineWidth + padding * 2);
        double boxHeight = lineHeight * lines.size() + padding * 2;
        double x = (canvas.getWidth() - boxWidth) / 2.0;
        double y = (canvas.getHeight() - boxHeight) / 2.0;

        g.setFill(Color.color(0.8, 0.0, 0.0, 0.85));
        g.fillRoundRect(x, y, boxWidth, boxHeight, 16, 16);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.strokeRoundRect(x, y, boxWidth, boxHeight, 16, 16);

        g.setFill(Color.WHITE);
        g.setFont(font);

        double textY = y + padding + baseline;
        Text drawProbe = new Text();
        drawProbe.setFont(font);
        for (String line : lines) {
            drawProbe.setText(line);
            double lineWidth = drawProbe.getLayoutBounds().getWidth();
            double textX = x + padding + Math.max(0, (boxWidth - padding * 2 - lineWidth) / 2.0);
            g.fillText(line, textX, textY);
            textY += lineHeight;
        }
    }

    private List<String> wrapLine(String text, Font font, double maxWidth) {
        List<String> wrapped = new ArrayList<>();
        if (text == null) {
            return wrapped;
        }
        String remaining = text.trim();
        if (remaining.isEmpty()) {
            return wrapped;
        }
        if (maxWidth <= 0) {
            wrapped.add(remaining);
            return wrapped;
        }

        Text measure = new Text();
        measure.setFont(font);

        while (!remaining.isEmpty()) {
            int breakIndex = -1;
            int i = 0;
            for (; i < remaining.length(); i++) {
                String candidate = remaining.substring(0, i + 1);
                measure.setText(candidate);
                if (measure.getLayoutBounds().getWidth() > maxWidth) {
                    break;
                }
                if (Character.isWhitespace(remaining.charAt(i))) {
                    breakIndex = i + 1;
                }
            }

            if (i == remaining.length()) {
                wrapped.add(remaining.trim());
                break;
            }

            if (breakIndex <= 0) {
                breakIndex = Math.max(1, i);
            }

            String line = remaining.substring(0, breakIndex).trim();
            if (!line.isEmpty()) {
                wrapped.add(line);
            }
            remaining = remaining.substring(breakIndex).trim();
        }

        if (wrapped.isEmpty() && !remaining.isEmpty()) {
            wrapped.add(remaining);
        }

        return wrapped;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double safe(Integer value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}

