package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;

/**
 * SPM 版本解析器接口 ("方言"解析策略)。
 * 定义了针对特定SPM“方言”的文件结构块的解析方法。
 */
public interface SpmVersionParser {

    Spm.SPMPageData parsePageData(BinaryReader reader);
    Spm.SPMChipData parseChipData(BinaryReader reader);
    Spm.SPMRect parseRect(BinaryReader reader);
    Spm.SPMHitArea parseHitArea(BinaryReader reader);
}