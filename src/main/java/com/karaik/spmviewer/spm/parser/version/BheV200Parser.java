package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.BheHitboxFactory;
import com.karaik.spmviewer.spm.hitarea.bhe.c.CRect;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BheV200Parser implements SpmDialectParser {

    @Override
    public Spm parse(BinaryReader reader, String spmVersion) {
        Spm spm = new Spm();
        spm.setSpmVersion(spmVersion);

        spm.setNumPageData(reader.readInt());
        List<Spm.SPMPageData> pages = new ArrayList<>();
        for (int i = 0; i < spm.getNumPageData(); i++) {
            pages.add(parsePage(reader));
        }
        spm.setPageData(pages);

        spm.setNumImageData(reader.readInt());
        List<Spm.SPMImageData> images = new ArrayList<>();
        for (int i = 0; i < spm.getNumImageData(); i++) {
            var img = new Spm.SPMImageData();
            img.setImageName(reader.readNullTerminatedString());
            images.add(img);
        }
        spm.setImageData(images);

        spm.setPatPageNum(reader.readInt());
        spm.setNumAnimData(reader.readInt());
        List<Spm.SPMAnimData> anims = new ArrayList<>();
        for (int i = 0; i < spm.getNumAnimData(); i++) {
            anims.add(parseAnim(reader, spm.getPatPageNum()));
        }
        spm.setAnimData(anims);
        return spm;
    }

    protected Spm.SPMPageData parsePage(BinaryReader reader) {
        var page = new Spm.SPMPageData();
        page.setNumChipData(reader.readInt());
        page.setPageWidth(reader.readInt());
        page.setPageHeight(reader.readInt());
        page.setPageRect(parseRect(reader));
        page.setPageOption(reader.readInt() & 0xFFFFFFFFL);
        page.setRotateCenterX(reader.readInt());
        page.setRotateCenterY(reader.readInt());
        page.setHitFlag(reader.readInt() & 0xFFFFFFFFL);

        List<Spm.SPMHitArea> hits = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (((1L << i) & page.getHitFlag()) != 0L) {
                hits.add(parseHit(reader));
            }
        }
        page.setHitRects(hits);

        List<Spm.SPMChipData> chips = new ArrayList<>();
        for (int i = 0; i < page.getNumChipData(); i++) {
            chips.add(parseChip(reader));
        }
        page.setChipData(chips);
        return page;
    }

    protected Spm.SPMHitArea parseHit(BinaryReader reader) {
        try {
            short id = reader.readShort();
            short shapeType = reader.readShort();
            Spm.SPMHitArea hitbox = BheHitboxFactory.createHitbox(shapeType);
            hitbox.setId(id);
            hitbox.setShapeType(shapeType);
            hitbox.readInfo(reader);
            return hitbox;
        } catch (Exception e) {
            log.error("Failed to parse BHE hitarea: {}", e.getMessage());
            return new CRect();
        }
    }

    protected Spm.SPMChipData parseChip(BinaryReader reader) {
        var chip = new Spm.SPMChipData();
        chip.setImageNo(reader.readInt());
        chip.setDstRect(parseRect(reader));
        chip.setChipWidth(reader.readInt());
        chip.setChipHeight(reader.readInt());
        chip.setSrcRect(parseRect(reader));
        chip.setDrawOption(reader.readInt() & 0xFFFFFFFFL);
        chip.setDrawOptionValue(reader.readInt() & 0xFFFFFFFFL);
        chip.setOption(reader.readInt());
        return chip;
    }

    protected Spm.SPMRect parseRect(BinaryReader reader) {
        var rect = new Spm.SPMRect();
        rect.setLeft(reader.readInt());
        rect.setTop(reader.readInt());
        rect.setRight(reader.readInt());
        rect.setBottom(reader.readInt());
        return rect;
    }

    protected Spm.SPMAnimData parseAnim(BinaryReader reader, int patPageNum) {
        var anim = new Spm.SPMAnimData();
        anim.setAnimName(reader.readNullTerminatedString());
        anim.setNumPat(reader.readInt());
        anim.setAnimRotateDirection(reader.readInt());
        anim.setAnimReverseDirection(reader.readInt());

        int numPat = anim.getNumPat() == null ? 0 : anim.getNumPat();
        List<Spm.SPMPatData> patData = new ArrayList<>();
        for (int i = 0; i < numPat; i++) {
            patData.add(parsePat(reader, patPageNum));
        }
        anim.setPatData(patData);
        return anim;
    }

    protected Spm.SPMPatData parsePat(BinaryReader reader, int patPageNum) {
        var pat = new Spm.SPMPatData();
        pat.setWaitFrame(reader.readInt());
        List<Integer> pages = new ArrayList<>();
        for (int i = 0; i < patPageNum; i++) {
            pages.add(reader.readInt());
        }
        pat.setPageNo(pages);
        return pat;
    }
}
