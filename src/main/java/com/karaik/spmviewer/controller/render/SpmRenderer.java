package com.karaik.spmviewer.controller.render;

import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Optional;

public class SpmRenderer {

    // render
    public void render(Canvas canvas,
                       Spm spm,
                       int pageIndex,
                       List<Image> images,
                       boolean showPageRect,
                       boolean showHit,
                       boolean showChipBounds) {

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.rgb(30, 30, 30));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Spm.SPMPageData page = spm.getPageData().get(pageIndex);

        int offX = 0;
        int offY = 0;
        if (page.getPageRect() != null) {
            offX = -safe(page.getPageRect().getLeft());
            offY = -safe(page.getPageRect().getTop());
        }

        drawChecker(g, safe(page.getPageWidth()), safe(page.getPageHeight()), offX, offY);

        var chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
        for (Spm.SPMChipData c : chips) {
            Image img = getImageByNo(images, c.getImageNo());
            if (img == null) continue;

            Rect dst = Rect.from(c.getDstRect());
            Rect src = Rect.from(c.getSrcRect());

            g.drawImage(img, src.x, src.y, src.w, src.h, offX + dst.x, offY + dst.y, dst.w, dst.h);

            if (showChipBounds) {
                g.setStroke(Color.ORANGE);
                g.setLineDashes(6);
                g.strokeRect(offX + dst.x, offY + dst.y, dst.w, dst.h);
                g.setLineDashes();
            }
        }

        if (showPageRect && page.getPageRect() != null) {
            Rect r = Rect.from(page.getPageRect());
            g.setStroke(Color.LIME);
            g.strokeRect(offX + r.x, offY + r.y, r.w, r.h);
        }

        if (showHit) {
            var hits = Optional.ofNullable(page.getHitRects()).orElse(List.of());
            g.setStroke(Color.RED);
            for (Spm.SPMHitArea ha : hits) {
                Rect r = Rect.from(ha.getHitRect());
                g.strokeRect(offX + r.x, offY + r.y, r.w, r.h);
            }
        }
    }

    // checker background
    private void drawChecker(GraphicsContext g, int w, int h, int offX, int offY) {
        int s = 16;
        for (int y = 0; y < h; y += s) {
            for (int x = 0; x < w; x += s) {
                boolean odd = ((x / s) + (y / s)) % 2 == 1;
                g.setFill(odd ? Color.gray(0.85) : Color.gray(0.92));
                g.fillRect(offX + x, offY + y, s, s);
            }
        }
    }

    // rect -> string
    public String rectToStr(Spm.SPMRect r) {
        if (r == null) return "-";
        Rect rr = Rect.from(r);
        return "[" + rr.x + "," + rr.y + "," + (rr.x + rr.w) + "," + (rr.y + rr.h) + "] w=" + rr.w + " h=" + rr.h;
    }

    // image by index
    private Image getImageByNo(List<Image> images, Integer no) {
        if (no == null || no < 0 || no >= images.size()) return null;
        return images.get(no);
    }

    // int safe
    private static int safe(Integer i) { return i == null ? 0 : i; }

    // rect helper
    private static class Rect {
        int x, y, w, h;

        static Rect from(Spm.SPMRect r) {
            Rect rr = new Rect();
            if (r == null) return rr;
            int L = r.getLeft() == null ? 0 : r.getLeft();
            int T = r.getTop() == null ? 0 : r.getTop();
            int R = r.getRight() == null ? L : r.getRight();
            int B = r.getBottom() == null ? T : r.getBottom();
            rr.x = L;
            rr.y = T;
            rr.w = R - L;
            rr.h = B - T;
            return rr;
        }
    }
}
