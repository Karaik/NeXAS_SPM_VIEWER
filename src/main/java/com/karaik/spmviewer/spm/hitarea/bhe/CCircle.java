package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CCircle extends Spm.SPMHitArea {
    // 圆心在页面坐标系中的 X 坐标。
    private Integer centerX;
    // 圆心在页面坐标系中的 Y 坐标。
    private Integer centerY;
    private byte[] skippedBytes;
    // 圆在游戏单位下的半径。
    private Integer radius;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        centerX = reader.readInt();
        centerY = reader.readInt();
        skippedBytes = reader.readBytes(8);
        radius = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("Circle (center: %d,%d r=%d)", centerX, centerY, radius);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (centerX == null || centerY == null || radius == null) return;
        double cx = pageOriginX + centerX;
        double cy = pageOriginY + centerY;
        double r = Math.max(0, radius);

        g.setFill(new Color(1, 0.5, 0, 0.25));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(Color.ORANGE);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }
}
