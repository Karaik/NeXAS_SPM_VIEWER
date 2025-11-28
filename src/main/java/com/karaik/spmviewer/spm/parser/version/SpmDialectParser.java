package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;

/**
 * 每种方言的完整解析器，返回通用的 {@link Spm} 视图模型。
 */
public interface SpmDialectParser {
    Spm parse(BinaryReader reader, String spmVersion);
}
