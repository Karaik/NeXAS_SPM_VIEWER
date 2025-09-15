package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CCircle extends Spm.SPMHitArea {
    private Integer centerX;
    private Integer centerY;
    private byte[] skippedBytes;
    private Integer radius;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        centerX = reader.readInt();
        centerY = reader.readInt();
        skippedBytes = reader.readBytes(8);
        radius = reader.readInt();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("Circle (CirclePoint: %d, %d, half: %d)", centerX, centerY, radius);
    }
}