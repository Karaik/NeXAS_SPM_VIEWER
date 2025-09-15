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

    /**
     * 渲染一个SPM页面到Canvas上。
     * @param canvas       目标画布
     * @param spm          当前SPM数据对象
     * @param pageIndex    要渲染的页面索引
     * @param images       已加载的图片列表
     * @param showCoords   是否显示坐标系
     * @param showHitboxes 是否显示Hitbox
     */
    public void render(Canvas canvas,
                       Spm spm,
                       int pageIndex,
                       List<Image> images,
                       boolean showCoords,
                       boolean showHitboxes) {

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.rgb(30, 30, 30));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Spm.SPMPageData page = spm.getPageData().get(pageIndex);

        // 计算页面的偏移量，使得页面的(0,0)点能够正确地映射到画布上
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

        // 计算页面的物理中心点（原点）在画布上的位置
        double originX = offX + safe(page.getRotateCenterX());
        double originY = offY + safe(page.getRotateCenterY());

        // 如果用户要求，绘制坐标系
        if (showCoords) {
            drawCoordinateSystem(g, originX, originY);
        }

        // 如果用户要求，绘制Hitboxes
        if (showHitboxes) {
            var hitboxes = Optional.ofNullable(page.getHitRects()).orElse(List.of());
            for (Spm.SPMHitArea hitbox : hitboxes) {
                // 利用多态，让每个hitbox自己绘制自己
                hitbox.drawSelf(g, originX, originY);
            }
        }
    }

    /**
     * 在指定的原点绘制十字坐标轴。
     */
    private void drawCoordinateSystem(GraphicsContext g, double originX, double originY) {
        g.setStroke(Color.YELLOW);
        g.setLineWidth(1.0);

        // 绘制X轴
        g.strokeLine(originX - 1000, originY, originX + 1000, originY);
        // 绘制Y轴
        g.strokeLine(originX, originY - 1000, originX, originY + 1000);

        // 绘制一个小圆点标记原点
        g.setFill(Color.YELLOW);
        g.fillOval(originX - 2, originY - 2, 4, 4);
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