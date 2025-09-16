package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CCircle extends Spm.SPMHitArea {
    private Integer centerX;
    private Integer centerY;
    private byte[] skippedBytes;
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
        return String.format("Circle (CirclePoint: %d, %d, half: %d)", centerX, centerY, radius);
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