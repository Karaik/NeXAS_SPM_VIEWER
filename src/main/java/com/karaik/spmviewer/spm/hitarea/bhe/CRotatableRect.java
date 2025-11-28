package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CRotatableRect extends Spm.SPMHitArea {
    // 矩形中心点的 X 坐标。
    private Integer centerX;
    // 矩形中心点的 Y 坐标。
    private Integer centerY;
    private byte[] skippedBytes1;
    // 矩形的宽度（像素）。
    private Integer width;
    // 矩形的高度（像素）。
    private Integer height;
    private byte[] skippedBytes2;
    // 低 16 位的状态标志；旋转信息可能存放于此，尚待确认。
    private Integer attrU16;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        centerX = reader.readInt();
        centerY = reader.readInt();
        skippedBytes1 = reader.readBytes(4);
        width = reader.readInt();
        height = reader.readInt();
        skippedBytes2 = reader.readBytes(4);
        int raw = reader.readInt();
        attrU16 = raw & 0xFFFF;
    }

    @Override
    public String getDisplayInfo() {
        return String.format("RotatableRect (center: %d, %d, w: %d, h: %d)", centerX, centerY, width, height);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (centerX == null || centerY == null || width == null || height == null) return;

        double cx = pageOriginX + centerX;
        double cy = pageOriginY + centerY;
        double w = width;
        double h = height;

        // 暂未验证旋转角度，先按轴对齐矩形绘制。
        double x = cx - w / 2.0;
        double y = cy - h / 2.0;

        g.setFill(new Color(0, 1, 0, 0.25));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.LIMEGREEN);
        g.strokeRect(x, y, w, h);
    }
}
