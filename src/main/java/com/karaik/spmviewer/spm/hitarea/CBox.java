package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
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
}