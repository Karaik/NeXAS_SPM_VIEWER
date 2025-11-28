package com.karaik.spmviewer.spm.hitarea.clarias.c;

import com.karaik.spmviewer.io.BinaryReader;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=2，但占位字节仅 4 个。
 */
public class CCircle extends com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle {

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        setCenterX(reader.readInt());
        setCenterY(reader.readInt());
        // 逆向显示仅有 4 字节 padding
        setSkippedBytes(reader.readBytes(4));
        setRadius(reader.readInt());
    }
}
