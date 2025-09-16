package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CBox extends Spm.SPMHitArea {
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        int1 = reader.readInt();
        int2 = reader.readInt();
        int3 = reader.readInt();
        int4 = reader.readInt();
        int5 = reader.readInt();
        int6 = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("Box (unk: %d, %d, %d, %d, %d, %d)", int1, int2, int3, int4, int5, int6);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        // 猜测为轴对齐盒：int1,int2中心；int3,int4宽高或半宽半高；
        if (int1 == null || int2 == null || int3 == null || int4 == null) return;
        double cx = pageOriginX + int1;
        double cy = pageOriginY + int2;
        double w = Math.abs(int3);
        double h = Math.abs(int4);
        double x = cx - w / 2.0;
        double y = cy - h / 2.0;

        g.setFill(new Color(0, 0.8, 0.6, 0.25));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.TEAL);
        g.strokeRect(x, y, w, h);
    }
}