package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CRotatableRect extends Spm.SPMHitArea {
    private Integer centerX;
    private Integer centerY;
    private byte[] skippedBytes1;
    private Integer width;
    private Integer height;
    private byte[] skippedBytes2;
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

    }
}