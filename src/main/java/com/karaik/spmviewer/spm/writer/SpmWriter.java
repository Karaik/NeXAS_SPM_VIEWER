package com.karaik.spmviewer.spm.writer;

import com.karaik.spmviewer.io.BinaryWriter;
import com.karaik.spmviewer.spm.Spm;

/**
 * 将 SPM 视图模型序列化为二进制。
 */
public interface SpmWriter {
    void write(Spm spm, BinaryWriter writer) throws Exception;
}
