package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;

/**
 * SPM VER-2.02 版本的特定解析器。
 * <p>
 * 继承自默认解析器，仅重写与基础版本有差异的解析逻辑。
 * 在此版本中，PageData 和 ChipData 结构中额外增加了一个字节的未知数据。
 */
public class SpmV202Parser extends DefaultSpmParser {

    /**
     * 重写 PageData 解析逻辑以处理 2.02 版本的额外字节。
     */
    @Override
    public Spm.SPMPageData parsePageData(BinaryReader reader) {
        // 先调用父类方法解析通用部分
        Spm.SPMPageData page = super.parsePageData(reader);
        // 读取 2.02 版本特有的额外字节
        page.setUnk3(reader.readByte());
        return page;
    }

    /**
     * 重写 ChipData 解析逻辑以处理 2.02 版本的额外字节。
     */
    @Override
    public Spm.SPMChipData parseChipData(BinaryReader reader) {
        // 先调用父类方法解析通用部分
        Spm.SPMChipData chipData = super.parseChipData(reader);
        // 读取 2.02 版本特有的额外字节
        chipData.setUnk5(reader.readByte());
        return chipData;
    }
}