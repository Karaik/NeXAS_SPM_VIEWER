package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class C2DLineSegment extends Spm.SPMHitArea {
    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;
    private byte[] skippedBytes;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        x1 = reader.readInt();
        y1 = reader.readInt();
        x2 = reader.readInt();
        y2 = reader.readInt();
        skippedBytes = reader.readBytes(8);
    }

    @Override
    public String getDisplayInfo() {
        return String.format("2DLineSegment (x1: %d, y1: %d) -> (x2: %d, y2: %d)", x1, y1, x2, y2);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (x1 == null || y1 == null || x2 == null || y2 == null) return;
        double sx = pageOriginX + x1;
        double sy = pageOriginY + y1;
        double ex = pageOriginX + x2;
        double ey = pageOriginY + y2;
        g.setStroke(Color.SKYBLUE);
        g.setLineWidth(1.5);
        g.strokeLine(sx, sy, ex, ey);
        // 端点
        g.setFill(Color.SKYBLUE);
        g.fillOval(sx - 2, sy - 2, 4, 4);
        g.fillOval(ex - 2, ey - 2, 4, 4);
    }
}