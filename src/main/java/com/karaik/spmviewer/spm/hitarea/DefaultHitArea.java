package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
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
}