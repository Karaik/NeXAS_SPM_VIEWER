package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支持旋转的长方体命中区域。数据给出了中心点与三轴尺寸，
 * 我们在画布上只渲染其在 XY 平面的投影矩形作为参考。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CRotatableBox extends Spm.SPMHitArea {
    // 命中盒中心点的 X 坐标。
    private Integer centerX;
    // 命中盒中心点的 Y 坐标。
    private Integer centerY;
    // 命中盒中心点的 Z 坐标。
    private Integer centerZ;
    // 沿 X 轴的尺寸（宽度）。
    private Integer sizeX;
    // 沿 Y 轴的尺寸（高度）。
    private Integer sizeY;
    // 沿 Z 轴的尺寸（深度）。
    private Integer sizeZ;
    // 目前样本中始终为 0，保留占位。
    private Integer reserved0;
    // 目前样本中始终为 0，保留占位。
    private Integer reserved1;
    // 低 16 位的属性标志位。
    private Integer propertyFlags;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        centerX = reader.readInt();
        centerY = reader.readInt();
        centerZ = reader.readInt();
        sizeX = reader.readInt();
        sizeY = reader.readInt();
        sizeZ = reader.readInt();
        reserved0 = reader.readInt();
        reserved1 = reader.readInt();
        int raw = reader.readInt();
        propertyFlags = raw & 0xFFFF;
    }

    @Override
    public String getDisplayInfo() {
        return String.format(
                "RotBox center=(%d,%d,%d) size=(%d,%d,%d)",
                centerX, centerY, centerZ, sizeX, sizeY, sizeZ
        );
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (centerX == null || centerY == null || sizeX == null || sizeY == null) {
            return;
        }

        double cx = pageOriginX + centerX;
        double cy = pageOriginY + centerY;
        double halfW = sizeX / 2.0;
        double halfH = sizeY / 2.0;

        double x = cx - halfW;
        double y = cy - halfH;

        g.setFill(new Color(0.7, 0, 0.7, 0.25));
        g.fillRect(x, y, halfW * 2, halfH * 2);
        g.setStroke(Color.MEDIUMPURPLE);
        g.strokeRect(x, y, halfW * 2, halfH * 2);

        if (sizeZ != null && sizeZ > 0) {
            g.setStroke(Color.GRAY);
            g.setLineWidth(0.5);
            g.strokeText("z±" + sizeZ / 2, x, y - 4);
        }
    }
}
