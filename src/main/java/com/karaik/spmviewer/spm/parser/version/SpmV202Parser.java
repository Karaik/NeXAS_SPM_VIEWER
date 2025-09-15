package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.LegacyRectHitArea;
import lombok.extern.slf4j.Slf4j;

/**
 * 方言 B 解析器：用于 SPM VER-2.02 版本。
 * <p>
 * 特点:
 * 1. HitArea 是固定的旧版矩形结构，字段顺序为: hitRect, unk0, unk1, unk2。
 * 2. PageData 和 ChipData 在结构末尾可能包含额外的字节。
 */
@Slf4j
public class SpmV202Parser extends BheSpmParser {

    private final String spmVersion;

    public SpmV202Parser(String spmVersion) {
        this.spmVersion = spmVersion;
    }

    /**
     * 重写 HitArea 解析逻辑，以适应 VER-2.02 的固定矩形结构。
     * 结构: hitRect (16 bytes), unk0 (4 bytes), unk1 (4 bytes), unk2 (4 bytes).
     */
    @Override
    public Spm.SPMHitArea parseHitArea(BinaryReader reader) {
        try {
            var hitArea = new LegacyRectHitArea();
            // 2.02 版没有 id 和 shapeType，我们将它们设为默认值
            hitArea.setId((short) -1);
            hitArea.setShapeType((short) -1);

            // 按照 gilberto.spm.json 的顺序读取
            var rect = new Spm.SPMRect();
            rect.setLeft(reader.readInt());
            rect.setTop(reader.readInt());
            rect.setRight(reader.readInt());
            rect.setBottom(reader.readInt());
            hitArea.setHitRect(rect);

            hitArea.setUnk0(reader.readInt());
            hitArea.setUnk1(reader.readInt());
            hitArea.setUnk2(reader.readInt());

            return hitArea;
        } catch (Exception e) {
            log.error("Failed to parse VER-2.02 HitArea: " + e.getMessage());
            return new LegacyRectHitArea();
        }
    }

    @Override
    public Spm.SPMPageData parsePageData(BinaryReader reader) {
        // 先调用父类方法解析通用部分，但父类的 parseHitArea 会被我们重写的方法覆盖
        Spm.SPMPageData page = super.parsePageData(reader);
        // 读取 2.02 版本特有的额外字节
        if ("SPM VER-2.02".equals(spmVersion)) {
            page.setUnk3(reader.readByte());
        }
        return page;
    }

    @Override
    public Spm.SPMChipData parseChipData(BinaryReader reader) {
        Spm.SPMChipData chipData = super.parseChipData(reader);
        // 读取 2.02 版本特有的额外字节
        if ("SPM VER-2.02".equals(spmVersion)) {
            chipData.setUnk5(reader.readByte());
        }
        return chipData;
    }
}