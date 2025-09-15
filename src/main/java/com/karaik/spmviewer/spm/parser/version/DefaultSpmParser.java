package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.parser.HitboxFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认的 SPM "方言"解析器实现。
 * 提供了对最常见或基础版本 SPM 文件的解析逻辑。
 */
public class DefaultSpmParser implements SpmVersionParser {

    @Override
    public Spm.SPMPageData parsePageData(BinaryReader reader) {
        var page = new Spm.SPMPageData();
        page.setNumChipData(reader.readInt());
        page.setPageWidth(reader.readInt());
        page.setPageHeight(reader.readInt());
        page.setPageRect(parseRect(reader));
        page.setPageOption(reader.readInt() & 0xFFFFFFFFL);
        page.setRotateCenterX(reader.readInt());
        page.setRotateCenterY(reader.readInt());
        page.setHitFlag(reader.readInt() & 0xFFFFFFFFL);

        List<Spm.SPMHitArea> hitRects = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (((1L << i) & page.getHitFlag()) != 0L) {
                hitRects.add(parseHitArea(reader));
            }
        }
        page.setHitRects(hitRects);

        List<Spm.SPMChipData> chipData = new ArrayList<>();
        for (int i = 0; i < page.getNumChipData(); i++) {
            chipData.add(parseChipData(reader));
        }
        page.setChipData(chipData);

        return page;
    }

    @Override
    public Spm.SPMChipData parseChipData(BinaryReader reader) {
        var chipData = new Spm.SPMChipData();
        chipData.setImageNo(reader.readInt());
        chipData.setDstRect(parseRect(reader));
        chipData.setChipWidth(reader.readInt());
        chipData.setChipHeight(reader.readInt());
        chipData.setSrcRect(parseRect(reader));
        chipData.setDrawOption(reader.readInt() & 0xFFFFFFFFL);
        chipData.setDrawOptionValue(reader.readInt() & 0xFFFFFFFFL);
        chipData.setOption(reader.readInt());
        return chipData;
    }

    @Override
    public Spm.SPMRect parseRect(BinaryReader reader) {
        var rect = new Spm.SPMRect();
        rect.setLeft(reader.readInt());
        rect.setTop(reader.readInt());
        rect.setRight(reader.readInt());
        rect.setBottom(reader.readInt());
        return rect;
    }

    /**
     * 解析多态的 HitArea。这是新架构的核心。
     */
    @Override
    public Spm.SPMHitArea parseHitArea(BinaryReader reader) {
        try {
            // 1. 读取所有 HitArea 共有的 id 和 shapeType
            short id = reader.readShort();
            short shapeType = reader.readShort();

            // 2. 使用工厂创建对应的 Hitbox 对象
            Spm.SPMHitArea hitbox = HitboxFactory.createHitbox(shapeType);
            hitbox.setId(id);
            hitbox.setShapeType(shapeType);

            // 3. 让 Hitbox 对象自己读取剩下的数据
            hitbox.readInfo(reader);

            return hitbox;
        } catch (Exception e) {
            System.err.println("parse HitArea failed: " + e.getMessage());
            // 返回一个安全的空对象，避免程序崩溃
            return new com.karaik.spmviewer.spm.hitarea.CRect();
        }
    }
}