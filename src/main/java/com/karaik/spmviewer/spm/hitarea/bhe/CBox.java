package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轴对齐的三维包围盒，渲染时仅绘制其在 XY 平面的投影。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CBox extends Spm.SPMHitArea {
    // 包围盒在页面坐标系中的最小 X。
    private Integer minX;
    // 包围盒在页面坐标系中的最小 Y。
    private Integer minY;
    // 包围盒在页面坐标系中的最小 Z。
    private Integer minZ;
    // 包围盒在页面坐标系中的最大 X。
    private Integer maxX;
    // 包围盒在页面坐标系中的最大 Y。
    private Integer maxY;
    // 包围盒在页面坐标系中的最大 Z。
    private Integer maxZ;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        minX = reader.readInt();
        minY = reader.readInt();
        minZ = reader.readInt();
        maxX = reader.readInt();
        maxY = reader.readInt();
        maxZ = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format(
                "Box (x: %d-%d, y: %d-%d, z: %d-%d)",
                minX, maxX, minY, maxY, minZ, maxZ
        );
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (minX == null || minY == null || maxX == null || maxY == null) {
            return;
        }

        double left = pageOriginX + Math.min(minX, maxX);
        double top = pageOriginY + Math.min(minY, maxY);
        double width = Math.abs(maxX - minX);
        double height = Math.abs(maxY - minY);

        g.setFill(new Color(0, 0.8, 0.6, 0.25));
        g.fillRect(left, top, width, height);
        g.setStroke(Color.TEAL);
        g.strokeRect(left, top, width, height);
    }
}
