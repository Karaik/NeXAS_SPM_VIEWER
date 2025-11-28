package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class C2DDot extends Spm.SPMHitArea {
    // 单点命中区域的 X 坐标。
    private Integer x;
    // 单点命中区域的 Y 坐标。
    private Integer y;
    private byte[] skippedBytes;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        x = reader.readInt();
        y = reader.readInt();
        skippedBytes = reader.readBytes(16);
    }

    @Override
    public String getDisplayInfo() {
        return String.format("2DDot (x: %d, y: %d)", x, y);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (x == null || y == null) return;
        double px = pageOriginX + x;
        double py = pageOriginY + y;
        g.setFill(Color.YELLOW);
        g.fillOval(px - 2, py - 2, 4, 4);
        g.setStroke(Color.GOLD);
        g.strokeOval(px - 2, py - 2, 4, 4);
    }
}
