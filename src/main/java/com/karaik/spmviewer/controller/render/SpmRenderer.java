package com.karaik.spmviewer.controller.render;

import com.karaik.spmviewer.controller.CanvasController;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class SpmRenderer {

    /**
     * 根据设置绘制画布背景。
     */
    public void drawBackground(GraphicsContext g, double w, double h, Settings.BackgroundMode bgMode, Color bgColor) {
        switch (bgMode) {
            case CHECKERBOARD -> drawChecker(g, (int) w, (int) h);
            case SOLID_COLOR -> {
                g.setFill(bgColor);
                g.fillRect(0, 0, w, h);
            }
        }
    }

    public void render(Canvas canvas,
                       Spm spm,
                       int pageIndex,
                       List<Image> images,
                       boolean showCoords,
                       boolean showChipBounds,
                       boolean showHitboxes,
                       boolean showPageBounds,
                       Settings.BackgroundMode bgMode,
                       Color bgColor,
                       Settings.OriginMode originMode,
                       CanvasController.PageExtents extents) {

        GraphicsContext g = canvas.getGraphicsContext2D();
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();

        drawBackground(g, canvasWidth, canvasHeight, bgMode, bgColor);

        Spm.SPMPageData page = spm.getPageData().get(pageIndex);

        PageContext ctx = computePageContext(page, originMode, extents, canvasWidth, canvasHeight);
        double translateX = ctx.translateX();
        double translateY = ctx.translateY();
        double pageOriginX = ctx.pageOriginX();
        double pageOriginY = ctx.pageOriginY();

        var chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
        for (Spm.SPMChipData c : chips) {
            Image img = getImageByNo(images, c.getImageNo());
            if (img == null) continue;

            Rect dst = Rect.from(c.getDstRect());
            Rect src = Rect.from(c.getSrcRect());

            double dx = translateX + dst.x;
            double dy = translateY + dst.y;
            g.drawImage(img, src.x, src.y, src.w, src.h, dx, dy, dst.w, dst.h);

            if (showChipBounds) {
                g.setStroke(Color.color(0.2, 0.7, 1.0, 0.6));
                g.setLineWidth(1.0);
                g.strokeRect(dx, dy, dst.w, dst.h);
            }
        }

        if (showPageBounds) {
            drawPageBounds(g, page, translateX, translateY, extents);
        }

        if (showCoords) {
            drawCoordinateSystem(g, pageOriginX, pageOriginY, originMode, extents);
        }

        if (showHitboxes) {
            var hitboxes = Optional.ofNullable(page.getHitRects()).orElse(List.of());
            for (Spm.SPMHitArea hitbox : hitboxes) {
                hitbox.drawSelf(g, pageOriginX, pageOriginY);
            }
        }
    }

    private void drawCoordinateSystem(GraphicsContext g, double originX, double originY,
                                      Settings.OriginMode originMode,
                                      CanvasController.PageExtents extents) {
        g.setStroke(Color.YELLOW);
        g.setLineWidth(1.0);
        if (originMode == Settings.OriginMode.CENTER) {
            g.strokeLine(0, originY, g.getCanvas().getWidth(), originY);
            g.strokeLine(originX, 0, originX, g.getCanvas().getHeight());
            g.setFill(Color.YELLOW);
            g.fillOval(originX - 2, originY - 2, 4, 4);
        } else {
            double guideLength = Math.min(80, Math.max(extents == null ? 80 : extents.getWidth(), 48));
            g.strokeLine(originX, originY, originX + guideLength, originY);
            g.strokeLine(originX, originY, originX, originY + guideLength);
            g.setFill(Color.YELLOW);
            g.fillOval(originX - 2, originY - 2, 4, 4);
        }
    }

    private void drawPageBounds(GraphicsContext g, Spm.SPMPageData page, double translateX, double translateY,
                                CanvasController.PageExtents extents) {
        g.setStroke(Color.ORANGE);
        g.setLineWidth(1.2);

        Spm.SPMRect rect = page.getPageRect();
        if (rect != null) {
            double x = translateX + safe(rect.getLeft());
            double y = translateY + safe(rect.getTop());
            double w = safe(rect.getRight()) - safe(rect.getLeft());
            double h = safe(rect.getBottom()) - safe(rect.getTop());
            g.strokeRect(x, y, w, h);
            return;
        }

        double pageWidth = safe(page.getPageWidth());
        double pageHeight = safe(page.getPageHeight());
        if (pageWidth > 0 && pageHeight > 0) {
            double left = translateX + safe(page.getRotateCenterX()) - pageWidth / 2.0;
            double top = translateY + safe(page.getRotateCenterY()) - pageHeight / 2.0;
            g.strokeRect(left, top, pageWidth, pageHeight);
        } else if (extents != null) {
            double left = translateX + extents.minX();
            double top = translateY + extents.minY();
            g.strokeRect(left, top, extents.getWidth(), extents.getHeight());
        }
    }

    private void drawChecker(GraphicsContext g, int w, int h) {
        int s = 16;
        for (int y = 0; y < h; y += s) {
            for (int x = 0; x < w; x += s) {
                boolean odd = ((x / s) + (y / s)) % 2 == 1;
                g.setFill(odd ? Color.gray(0.85) : Color.gray(0.92));
                g.fillRect(x, y, s, s);
            }
        }
    }

    public String rectToStr(Spm.SPMRect r) {
        if (r == null) return "-";
        Rect rr = Rect.from(r);
        return "[" + rr.x + "," + rr.y + "," + (rr.x + rr.w) + "," + (rr.y + rr.h) + "] w=" + rr.w + " h=" + rr.h;
    }

    public PageContext computePageContext(Spm.SPMPageData page,
                                          Settings.OriginMode originMode,
                                          CanvasController.PageExtents extents,
                                          double canvasWidth,
                                          double canvasHeight) {
        double translateX;
        double translateY;
        double pageOriginX;
        double pageOriginY;

        if (originMode == Settings.OriginMode.TOP_LEFT && extents != null) {
            double contentWidth = Math.max(extents.getWidth(), 1.0);
            double contentHeight = Math.max(extents.getHeight(), 1.0);
            double anchorX = (canvasWidth - contentWidth) / 2.0;
            double anchorY = (canvasHeight - contentHeight) / 2.0;
            translateX = anchorX - extents.minX();
            translateY = anchorY - extents.minY();
            pageOriginX = translateX + safe(page.getRotateCenterX());
            pageOriginY = translateY + safe(page.getRotateCenterY());
        } else {
            double worldOriginX = canvasWidth / 2.0;
            double worldOriginY = canvasHeight / 2.0;
            translateX = worldOriginX - safe(page.getRotateCenterX());
            translateY = worldOriginY - safe(page.getRotateCenterY());
            pageOriginX = worldOriginX;
            pageOriginY = worldOriginY;
        }
        return new PageContext(translateX, translateY, pageOriginX, pageOriginY);
    }

    public record PageContext(double translateX, double translateY, double pageOriginX, double pageOriginY) { }

    private Image getImageByNo(List<Image> images, Integer no) {
        if (no == null || no < 0 || no >= images.size()) return null;
        return images.get(no);
    }

    private static double safe(Integer value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static class Rect {
        double x, y, w, h;

        static Rect from(Spm.SPMRect r) {
            Rect rr = new Rect();
            if (r == null) return rr;
            double L = safe(r.getLeft());
            double T = safe(r.getTop());
            double R = safe(r.getRight());
            double B = safe(r.getBottom());
            rr.x = L;
            rr.y = T;
            rr.w = R - L;
            rr.h = B - T;
            return rr;
        }
    }
}
