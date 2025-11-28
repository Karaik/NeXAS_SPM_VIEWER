package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefaultHitArea extends Spm.SPMHitArea {
    // 区域在页面坐标系中的最小 X。
    private Integer xMin;
    // 区域在页面坐标系中的最大 X。
    private Integer xMax;
    // 区域在页面坐标系中的最小 Y。
    private Integer yMin;
    // 区域在页面坐标系中的最大 Y。
    private Integer yMax;
    // 区域在页面坐标系中的最小 Z。
    private Integer zMin;
    // 区域在页面坐标系中的最大 Z。
    private Integer zMax;
    // 对齐旧版矩形使用时的显示矩形（可选）
    private Spm.SPMRect fallbackRect;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        xMin = reader.readInt();
        xMax = reader.readInt();
        yMin = reader.readInt();
        yMax = reader.readInt();
        zMin = reader.readInt();
        zMax = reader.readInt();
        // 建一个便于渲染的备用矩形
        fallbackRect = new Spm.SPMRect();
        fallbackRect.setLeft(xMin);
        fallbackRect.setRight(xMax);
        fallbackRect.setTop(yMin);
        fallbackRect.setBottom(yMax);
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
