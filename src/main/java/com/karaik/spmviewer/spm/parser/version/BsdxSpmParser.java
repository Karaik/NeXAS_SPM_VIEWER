package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.LegacyRectHitArea;
import lombok.extern.slf4j.Slf4j;

/**
 * 方言 C 解析器：用于 bsdx 的 SPM VER-2.00 版本。
 * <p>
 * 特点:
 * 1. HitArea 是固定的旧版矩形结构，但字段顺序与 VER-2.02 不同。
 * 结构: unk0, hitRect, unk1, unk2.
 */
@Slf4j
public class BsdxSpmParser extends BheSpmParser {

    /**
     * 重写 HitArea 解析逻辑，以适应 bsdx 的字段顺序。
     * 结构: unk0 (4 bytes), hitRect (16 bytes), unk1 (4 bytes), unk2 (4 bytes).
     */
    @Override
    public Spm.SPMHitArea parseHitArea(BinaryReader reader) {
        try {
            var hitArea = new LegacyRectHitArea();
            hitArea.setId((short) -1);
            hitArea.setShapeType((short) -1);

            // 按照 bsdx 的顺序读取
            hitArea.setUnk0(reader.readInt());

            var rect = new Spm.SPMRect();
            rect.setLeft(reader.readInt());
            rect.setTop(reader.readInt());
            rect.setRight(reader.readInt());
            rect.setBottom(reader.readInt());
            hitArea.setHitRect(rect);

            hitArea.setUnk1(reader.readInt());
            hitArea.setUnk2(reader.readInt());

            return hitArea;
        } catch (Exception e) {
            log.error("Failed to parse bsdx HitArea: " + e.getMessage());
            return new LegacyRectHitArea();
        }
    }
}