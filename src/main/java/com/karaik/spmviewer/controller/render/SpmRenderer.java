package com.karaik.spmviewer.controller.render;

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

    // MODIFIED: Removed boolean parameters for view options
    public void render(Canvas canvas,
                       Spm spm,
                       int pageIndex,
                       List<Image> images) {

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

        drawChecker(g, safe(page.getPageWidth()), safe(page.getPageHeight()));

        var chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
        for (Spm.SPMChipData c : chips) {
            Image img = getImageByNo(images, c.getImageNo());
            if (img == null) continue;

            Rect dst = Rect.from(c.getDstRect());
            Rect src = Rect.from(c.getSrcRect());

            g.drawImage(img, src.x, src.y, src.w, src.h, offX + dst.x, offY + dst.y, dst.w, dst.h);
        }

        // NOTE: The rendering for page rect, hit areas, and chip bounds has been removed.
        // If you want to re-add them later, they would go here.
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

    private Image getImageByNo(List<Image> images, Integer no) {
        if (no == null || no < 0 || no >= images.size()) return null;
        return images.get(no);
    }

    private static int safe(Integer i) { return i == null ? 0 : i; }

    private static class Rect {
        int x, y, w, h;

        static Rect from(Spm.SPMRect r) {
            Rect rr = new Rect();
            if (r == null) return rr;
            int L = safe(r.getLeft());
            int T = safe(r.getTop());
            int R = safe(r.getRight());
            int B = safe(r.getBottom());
            rr.x = L;
            rr.y = T;
            rr.w = R - L;
            rr.h = B - T;
            return rr;
        }
    }
}