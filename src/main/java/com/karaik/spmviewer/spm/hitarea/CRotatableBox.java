package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CRotatableBox extends Spm.SPMHitArea {
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer attrU16;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        int1 = reader.readInt();
        int2 = reader.readInt();
        int3 = reader.readInt();
        int4 = reader.readInt();
        int5 = reader.readInt();
        int6 = reader.readInt();
        int7 = reader.readInt();
        int8 = reader.readInt();
        int raw = reader.readInt();
        attrU16 = raw & 0xFFFF;
    }

    @Override
    public String getDisplayInfo() {
        return String.format("RotatableBox (property: %d)", attrU16);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        // 通过JSON观察：shapeType=10时，int1..int6看似三个宽高对，用以不同方向遮罩；此处画成轴对齐矩形，使用int3,int4为半宽半高？
        if (int1 == null || int2 == null || int3 == null || int4 == null) return;

        double cx = pageOriginX + int1;
        double cy = pageOriginY + int2;
        double halfW = int3 / 2.0;
        double halfH = int4 / 2.0;

        double x = cx - halfW;
        double y = cy - halfH;
        double w = halfW * 2;
        double h = halfH * 2;

        g.setFill(new Color(0.7, 0, 0.7, 0.25));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.MEDIUMPURPLE);
        g.strokeRect(x, y, w, h);
    }
}