package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultHitArea extends Spm.SPMHitArea {
    private Integer xMin;
    private Integer xMax;
    private Integer yMin;
    private Integer yMax;
    private Integer zMin;
    private Integer zMax;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        xMin = reader.readInt();
        xMax = reader.readInt();
        yMin = reader.readInt();
        yMax = reader.readInt();
        zMin = reader.readInt();
        zMax = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("DefaultHitArea (x: %d-%d, y: %d-%d, z: %d-%d)", xMin, xMax, yMin, yMax, zMin, zMax);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (xMin == null || xMax == null || yMin == null || yMax == null) return;

        double x = pageOriginX + Math.min(xMin, xMax);
        double y = pageOriginY + Math.min(yMin, yMax);
        double w = Math.abs(xMax - xMin);
        double h = Math.abs(yMax - yMin);

        g.setFill(new Color(0, 0.6, 1, 0.25));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.CORNFLOWERBLUE);
        g.strokeRect(x, y, w, h);
    }
}