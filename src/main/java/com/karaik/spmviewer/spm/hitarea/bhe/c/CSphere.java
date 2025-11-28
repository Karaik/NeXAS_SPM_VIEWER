package com.karaik.spmviewer.spm.hitarea.bhe.c;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CSphere extends Spm.SPMHitArea {
    // 球心在页面坐标系中的 X。
    private Integer centerX;
    // 球心在页面坐标系中的 Y。
    private Integer centerY;
    // 球心在页面坐标系中的 Z。
    private Integer centerZ;
    // 球的半径（游戏单位）。
    private Integer radius;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        centerX = reader.readInt();
        centerY = reader.readInt();
        centerZ = reader.readInt();
        radius = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format(
                "Sphere (center: %d,%d,%d r=%d)",
                centerX, centerY, centerZ, radius
        );
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (centerX == null || centerY == null || radius == null) {
            return;
        }
        double cx = pageOriginX + centerX;
        double cy = pageOriginY + centerY;
        double r = Math.max(0, radius);

        g.setFill(new Color(0.2, 0.8, 1, 0.2));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(Color.CYAN);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }
}
