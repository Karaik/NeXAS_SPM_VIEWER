package com.karaik.spmviewer.spm.hitarea.bsdx;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BSDX 专用的旧版矩形 HitArea（方言 C），独立于 BHE/Clarias。
 * 字段由 BSDX 解析器直接写入：unk0 → hitRect → unk1 → unk2。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LegacyRectHitArea extends Spm.SPMHitArea {
    private Spm.SPMRect hitRect;
    private Integer unk0;
    private Integer unk1;
    private Integer unk2;

    @Override
    public void readInfo(BinaryReader reader) {
        // BSDX 解析器直接设置字段，这里无需读取。
    }

    @Override
    public String getDisplayInfo() {
        if (hitRect == null) {
            return "Legacy Rect (data incomplete)";
        }
        return String.format("Legacy Rect [%d, %d, %d, %d], unk: %d,%d,%d",
                hitRect.getLeft(), hitRect.getTop(), hitRect.getRight(), hitRect.getBottom(),
                unk0, unk1, unk2);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (hitRect == null) return;

        double x = pageOriginX + hitRect.getLeft();
        double y = pageOriginY + hitRect.getTop();
        double w = hitRect.getRight() - hitRect.getLeft();
        double h = hitRect.getBottom() - hitRect.getTop();

        g.setFill(new Color(0.5, 0, 1, 0.4));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.PURPLE);
        g.strokeRect(x, y, w, h);
    }

    @Override
    public double[] getBounds() {
        if (hitRect == null) return new double[]{0, 0, 0, 0};
        double l = hitRect.getLeft() == null ? 0 : hitRect.getLeft();
        double t = hitRect.getTop() == null ? 0 : hitRect.getTop();
        double r = hitRect.getRight() == null ? 0 : hitRect.getRight();
        double b = hitRect.getBottom() == null ? 0 : hitRect.getBottom();
        return new double[]{Math.min(l, r), Math.min(t, b), Math.max(l, r), Math.max(t, b)};
    }
}
