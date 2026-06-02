package com.karaik.spmviewer.spm.writer.version;

import com.karaik.spmviewer.io.BinaryWriter;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.c.*;
import com.karaik.spmviewer.spm.writer.SpmWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class BheV200Writer implements SpmWriter {

    @Override
    public void write(Spm spm, BinaryWriter writer) throws Exception {
        writer.writeNullTerminatedString(spm.getSpmVersion());
        writer.writeInt(val(spm.getNumPageData()));
        for (Spm.SPMPageData page : nvl(spm.getPageData())) {
            writePage(writer, page);
        }
        writer.writeInt(val(spm.getNumImageData()));
        for (Spm.SPMImageData img : nvl(spm.getImageData())) {
            writer.writeNullTerminatedString(img.getImageName());
        }
        writer.writeInt(val(spm.getPatPageNum()));
        writer.writeInt(val(spm.getNumAnimData()));
        for (Spm.SPMAnimData anim : nvl(spm.getAnimData())) {
            writeAnim(writer, anim, val(spm.getPatPageNum()));
        }
    }

    protected void writePage(BinaryWriter writer, Spm.SPMPageData page) throws Exception {
        writer.writeInt(val(page.getNumChipData()));
        writer.writeInt(val(page.getPageWidth()));
        writer.writeInt(val(page.getPageHeight()));
        writeRect(writer, page.getPageRect());
        writer.writeInt((int) (valL(page.getPageOption()) & 0xFFFFFFFFL));
        writer.writeInt(val(page.getRotateCenterX()));
        writer.writeInt(val(page.getRotateCenterY()));
        long hitFlag = valL(page.getHitFlag());
        writer.writeInt((int) (hitFlag & 0xFFFFFFFFL));

        List<Spm.SPMHitArea> hits = nvl(page.getHitRects());
        for (int i = 0; i < 32; i++) {
            if (((hitFlag >> i) & 1L) != 0L) {
                if (i < hits.size()) {
                    writeHit(writer, hits.get(i));
                }
            }
        }

        for (Spm.SPMChipData chip : nvl(page.getChipData())) {
            writeChip(writer, chip);
        }
    }

    protected void writeHit(BinaryWriter writer, Spm.SPMHitArea hit) throws Exception {
        writer.writeShort(hit.getId());
        writer.writeShort(hit.getShapeType());

        if (hit instanceof DefaultHitArea d) {
            writer.writeInt(val(d.getXMin())); writer.writeInt(val(d.getXMax()));
            writer.writeInt(val(d.getYMin())); writer.writeInt(val(d.getYMax()));
            writer.writeInt(val(d.getZMin())); writer.writeInt(val(d.getZMax()));
        } else if (hit instanceof CRect r) {
            writeRect(writer, r.getRect());
            writer.writeLong(r.getSkipped() != null ? r.getSkipped() : 0L);
        } else if (hit instanceof CCircle c) {
            writer.writeInt(val(c.getCenterX())); writer.writeInt(val(c.getCenterY()));
            writer.writeLong(0L);
            writer.writeInt(val(c.getRadius()));
        } else if (hit instanceof CBox b) {
            writer.writeInt(val(b.getMinX())); writer.writeInt(val(b.getMinY()));
            writer.writeInt(val(b.getMinZ())); writer.writeInt(val(b.getMaxX()));
            writer.writeInt(val(b.getMaxY())); writer.writeInt(val(b.getMaxZ()));
        } else if (hit instanceof CRotatableRect rr) {
            writer.writeInt(val(rr.getCenterX())); writer.writeInt(val(rr.getCenterY()));
            writer.writeInt(0);
            writer.writeInt(val(rr.getWidth())); writer.writeInt(val(rr.getHeight()));
            writer.writeInt(0);
            writer.writeInt(val(rr.getAttrU16()));
        } else if (hit instanceof CRotatableBox rb) {
            writer.writeInt(val(rb.getCenterX())); writer.writeInt(val(rb.getCenterY()));
            writer.writeInt(val(rb.getCenterZ()));
            writer.writeInt(val(rb.getSizeX())); writer.writeInt(val(rb.getSizeY()));
            writer.writeInt(val(rb.getSizeZ()));
            writer.writeInt(val(rb.getReserved0())); writer.writeInt(val(rb.getReserved1()));
            writer.writeInt(val(rb.getPropertyFlags()));
        } else if (hit instanceof C2DDot dot) {
            writer.writeInt(val(dot.getX())); writer.writeInt(val(dot.getY()));
            writer.writeLong(0L); writer.writeLong(0L);
        } else if (hit instanceof C2DLineSegment ls) {
            writer.writeInt(val(ls.getX1())); writer.writeInt(val(ls.getY1()));
            writer.writeInt(val(ls.getX2())); writer.writeInt(val(ls.getY2()));
            writer.writeLong(0L);
        } else if (hit instanceof CSphere s) {
            writer.writeInt(val(s.getCenterX())); writer.writeInt(val(s.getCenterY()));
            writer.writeInt(val(s.getCenterZ())); writer.writeInt(val(s.getRadius()));
        } else {
            writeRect(writer, null);
            writer.writeLong(0L);
        }
    }

    protected void writeChip(BinaryWriter writer, Spm.SPMChipData chip) throws Exception {
        writer.writeInt(val(chip.getImageNo()));
        writeRect(writer, chip.getDstRect());
        writer.writeInt(val(chip.getChipWidth()));
        writer.writeInt(val(chip.getChipHeight()));
        writeRect(writer, chip.getSrcRect());
        writer.writeInt((int) (valL(chip.getDrawOption()) & 0xFFFFFFFFL));
        writer.writeInt((int) (valL(chip.getDrawOptionValue()) & 0xFFFFFFFFL));
        writer.writeInt(val(chip.getOption()));
    }

    protected void writeAnim(BinaryWriter writer, Spm.SPMAnimData anim, int patPageNum) throws Exception {
        writer.writeNullTerminatedString(anim.getAnimName());
        writer.writeInt(val(anim.getNumPat()));
        writer.writeInt(val(anim.getAnimRotateDirection()));
        writer.writeInt(val(anim.getAnimReverseDirection()));

        for (Spm.SPMPatData pat : nvl(anim.getPatData())) {
            writer.writeInt(val(pat.getWaitFrame()));
            List<Integer> pages = nvl(pat.getPageNo());
            for (int i = 0; i < patPageNum; i++) {
                writer.writeInt(i < pages.size() ? val(pages.get(i)) : 0);
            }
        }
    }

    protected void writeRect(BinaryWriter writer, Spm.SPMRect rect) throws Exception {
        if (rect == null) {
            writer.writeInt(0); writer.writeInt(0);
            writer.writeInt(0); writer.writeInt(0);
        } else {
            writer.writeInt(val(rect.getLeft())); writer.writeInt(val(rect.getTop()));
            writer.writeInt(val(rect.getRight())); writer.writeInt(val(rect.getBottom()));
        }
    }

    protected static int val(Integer v) { return v == null ? 0 : v; }
    protected static long valL(Long v) { return v == null ? 0L : v; }

    @SuppressWarnings("unchecked")
    protected static <T> List<T> nvl(List<T> list) {
        return Optional.ofNullable(list).orElse(List.of());
    }
}
