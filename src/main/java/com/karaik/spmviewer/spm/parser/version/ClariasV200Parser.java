package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.clarias.ClariasHitboxFactory;
import com.karaik.spmviewer.spm.hitarea.clarias.c.CRect;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Clarias 方言 2.00：按逆向伪代码的读取顺序实现，避免错位导致 UI 无响应。
 */
@Slf4j
public class ClariasV200Parser implements SpmDialectParser {

    @Override
    public Spm parse(BinaryReader reader, String spmVersion) {
        boolean is202 = isV202(spmVersion);

        Spm spm = new Spm();
        spm.setSpmVersion(spmVersion);
        try {
            int numPages = safeReadInt(reader);
            spm.setNumPageData(numPages);
            List<Spm.SPMPageData> pages = new ArrayList<>();
            for (int i = 0; i < numPages && reader.hasRemaining(); i++) {
                pages.add(parsePage(reader, is202));
            }
            spm.setPageData(pages);

            int numImages = safeReadInt(reader);
            spm.setNumImageData(numImages);
            List<Spm.SPMImageData> images = new ArrayList<>();
            for (int i = 0; i < numImages && reader.hasRemaining(); i++) {
                Spm.SPMImageData img = new Spm.SPMImageData();
                img.setImageName(reader.readNullTerminatedString());
                images.add(img);
            }
            spm.setImageData(images);

            int patPageNum = safeReadInt(reader);
            spm.setPatPageNum(patPageNum);

            int numAnims = safeReadInt(reader);
            spm.setNumAnimData(numAnims);
            List<Spm.SPMAnimData> anims = new ArrayList<>();
            for (int i = 0; i < numAnims && reader.hasRemaining(); i++) {
                anims.add(parseAnim(reader, patPageNum));
            }
            spm.setAnimData(anims);
        } catch (Exception ex) {
            log.warn("Failed to parse Clarias SPM (partial): {}", ex.getMessage());
        }
        return spm;
    }

    protected boolean isV202(String spmVersion) {
        return spmVersion != null && spmVersion.contains("2.02");
    }

    protected Spm.SPMPageData parsePage(BinaryReader reader, boolean is202) {
        var page = new Spm.SPMPageData();
        try {
            page.setNumChipData(safeReadInt(reader));
            page.setPageWidth(safeReadInt(reader));
            page.setPageHeight(safeReadInt(reader));
            page.setPageRect(parseRect(reader));
            page.setPageOption((long) safeReadInt(reader) & 0xFFFFFFFFL);
            page.setRotateCenterX(safeReadInt(reader));
            page.setRotateCenterY(safeReadInt(reader));

            if (is202 && reader.remaining() > 0) {
                page.setUnk3(reader.readByte());
            }

            long hitFlag = (long) safeReadInt(reader) & 0xFFFFFFFFL;
            page.setHitFlag(hitFlag);

            List<Spm.SPMHitArea> hits = new ArrayList<>();
            for (int i = 0; i < 32 && reader.hasRemaining(); i++) {
                if (((1L << i) & hitFlag) != 0L) {
                    hits.add(parseHit(reader));
                }
            }
            page.setHitRects(hits);

            int chipCount = page.getNumChipData() == null ? 0 : page.getNumChipData();
            List<Spm.SPMChipData> chips = new ArrayList<>(chipCount);
            for (int i = 0; i < chipCount && reader.hasRemaining(); i++) {
                chips.add(parseChip(reader, is202));
            }
            page.setChipData(chips);
        } catch (Exception ex) {
            log.warn("Clarias page parse failed (partial): {}", ex.getMessage());
        }
        return page;
    }

    protected Spm.SPMChipData parseChip(BinaryReader reader, boolean is202) {
        var chip = new Spm.SPMChipData();
        chip.setImageNo(safeReadInt(reader));
        chip.setDstRect(parseRect(reader));
        chip.setChipWidth(safeReadInt(reader));
        chip.setChipHeight(safeReadInt(reader));
        chip.setSrcRect(parseRect(reader));
        chip.setDrawOption((long) safeReadInt(reader) & 0xFFFFFFFFL);
        if (is202 && reader.remaining() > 0) {
            chip.setUnk5(reader.readByte());
        }
        chip.setDrawOptionValue((long) safeReadInt(reader) & 0xFFFFFFFFL);
        chip.setOption(safeReadInt(reader));
        return chip;
    }

    protected Spm.SPMHitArea parseHit(BinaryReader reader) {
        if (reader.remaining() < 4) {
            return new CRect();
        }
        try {
            short id = reader.readShort();
            short shape = reader.readShort();
            Spm.SPMHitArea hit = ClariasHitboxFactory.createHitbox(shape);
            hit.setId(id);
            hit.setShapeType(shape);
            hit.readInfo(reader);
            return hit;
        } catch (Exception ex) {
            log.warn("Clarias hit parse failed: {}", ex.getMessage());
            return new CRect();
        }
    }

    protected Spm.SPMRect parseRect(BinaryReader reader) {
        if (reader.remaining() < 16) {
            return null;
        }
        var rect = new Spm.SPMRect();
        rect.setLeft(reader.readInt());
        rect.setTop(reader.readInt());
        rect.setRight(reader.readInt());
        rect.setBottom(reader.readInt());
        return rect;
    }

    protected Spm.SPMAnimData parseAnim(BinaryReader reader, int patPageNum) {
        var anim = new Spm.SPMAnimData();
        try {
            anim.setAnimName(reader.readNullTerminatedString());
            anim.setNumPat(safeReadInt(reader));
            anim.setAnimRotateDirection(safeReadInt(reader));
            anim.setAnimReverseDirection(safeReadInt(reader));

            int numPat = anim.getNumPat() == null ? 0 : anim.getNumPat();
            List<Spm.SPMPatData> pats = new ArrayList<>(numPat);
            for (int i = 0; i < numPat && reader.hasRemaining(); i++) {
                pats.add(parsePat(reader, patPageNum));
            }
            anim.setPatData(pats);
        } catch (Exception ex) {
            log.warn("Clarias anim parse failed (partial): {}", ex.getMessage());
        }
        return anim;
    }

    protected Spm.SPMPatData parsePat(BinaryReader reader, int patPageNum) {
        var pat = new Spm.SPMPatData();
        pat.setWaitFrame(safeReadInt(reader));

        int pageCount = Math.max(0, patPageNum);
        List<Integer> pageNos = new ArrayList<>(pageCount);
        for (int i = 0; i < pageCount && reader.hasRemaining(); i++) {
            pageNos.add(safeReadInt(reader));
        }
        pat.setPageNo(pageNos);
        return pat;
    }

    protected int safeReadInt(BinaryReader reader) {
        if (reader.remaining() < 4) {
            log.trace("not enough remaining bytes: {} < 4, fill 0", reader.remaining());
            return 0;
        }
        return reader.readInt();
    }
}
