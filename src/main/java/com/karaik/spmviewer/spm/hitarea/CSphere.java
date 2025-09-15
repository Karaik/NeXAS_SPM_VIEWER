package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
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
}