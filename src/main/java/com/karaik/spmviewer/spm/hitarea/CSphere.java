package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CSphere extends Spm.SPMHitArea {
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        int1 = reader.readInt();
        int2 = reader.readInt();
        int3 = reader.readInt();
        int4 = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("Sphere (unk: %d, %d, %d, %d)", int1, int2, int3, int4);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        // 暂时将3D球体投影为圆：int1,int2为中心；int3为半径；int4为备用
        if (int1 == null || int2 == null || int3 == null) return;
        double cx = pageOriginX + int1;
        double cy = pageOriginY + int2;
        double r = Math.max(0, int3);

        g.setFill(new Color(0.2, 0.8, 1, 0.2));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(Color.CYAN);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }
}