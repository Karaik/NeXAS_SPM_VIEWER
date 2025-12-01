package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.io.BinaryWriter;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.c.*;
import com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/**
 * 简单的 SPM 写回器：按当前内存模型导出为二进制，匹配现有解析顺序。
 * 仅用于“另存为”，原始文件覆盖需谨慎。
 */
@Slf4j
public class SpmBinaryWriter {

    public static void write(Spm spm, OutputStream os, Settings.ParsingMode mode) throws IOException {
        Objects.requireNonNull(spm, "spm");
        try (BinaryWriter w = new BinaryWriter(os, Settings.getCharsetName())) {
            String ver = spm.getSpmVersion();
            boolean is202 = ver != null && ver.contains("2.02");
            w.writeNullTerminatedString(ver == null ? "SPM VER-2.00" : ver);
            switch (mode) {
                case BHE -> writeBhe(spm, w, is202);
                case CLARIAS -> writeClarias(spm, w, is202);
                case BSDX -> writeBsdx(spm, w, is202);
            }
        }
    }

    private static void writePagesCommon(Spm spm, BinaryWriter w, boolean is202, boolean clarias) throws IOException {
        List<Spm.SPMPageData> pages = defaultList(spm.getPageData());
        w.writeInt(pages.size());
        for (Spm.SPMPageData p : pages) {
            w.writeInt(defaultInt(p.getNumChipData()));
            w.writeInt(defaultInt(p.getPageWidth()));
            w.writeInt(defaultInt(p.getPageHeight()));
            writeRect(w, p.getPageRect());
            w.writeInt((int) defaultLong(p.getPageOption()));
            w.writeInt(defaultInt(p.getRotateCenterX()));
            w.writeInt(defaultInt(p.getRotateCenterY()));
            if (clarias && is202) {
                w.writeByte(p.getUnk3());
            }
            w.writeInt((int) defaultLong(p.getHitFlag()));

            long flag = defaultLong(p.getHitFlag());
            List<Spm.SPMHitArea> hits = defaultList(p.getHitRects());
            int hitIndex = 0;
            for (int i = 0; i < 32; i++) {
                if (((1L << i) & flag) != 0) {
                    if (hitIndex < hits.size()) {
                        writeHit(w, hits.get(hitIndex++), clarias);
                    } else {
                        // 占位填零
                        w.writeShort((short) 0);
                        w.writeShort((short) 0);
                    }
                }
            }

            List<Spm.SPMChipData> chips = defaultList(p.getChipData());
            for (Spm.SPMChipData c : chips) {
                w.writeInt(defaultInt(c.getImageNo()));
                writeRect(w, c.getDstRect());
                w.writeInt(defaultInt(c.getChipWidth()));
                w.writeInt(defaultInt(c.getChipHeight()));
                writeRect(w, c.getSrcRect());
                w.writeInt((int) defaultLong(c.getDrawOption()));
                if (clarias && is202 && c.getUnk5() != 0) {
                    w.writeByte(c.getUnk5());
                }
                w.writeInt((int) defaultLong(c.getDrawOptionValue()));
                w.writeInt(defaultInt(c.getOption()));
            }
        }
    }

    private static void writeBhe(Spm spm, BinaryWriter w, boolean is202) throws IOException {
        writePagesCommon(spm, w, is202, false);
        writeImagesAndAnim(spm, w);
    }

    private static void writeClarias(Spm spm, BinaryWriter w, boolean is202) throws IOException {
        writePagesCommon(spm, w, is202, true);
        writeImagesAndAnim(spm, w);
    }

    private static void writeBsdx(Spm spm, BinaryWriter w, boolean is202) throws IOException {
        List<Spm.SPMPageData> pages = defaultList(spm.getPageData());
        w.writeInt(pages.size());
        for (Spm.SPMPageData p : pages) {
            w.writeInt(defaultInt(p.getNumChipData()));
            w.writeInt(defaultInt(p.getPageWidth()));
            w.writeInt(defaultInt(p.getPageHeight()));
            writeRect(w, p.getPageRect());
            w.writeInt((int) defaultLong(p.getPageOption()));
            w.writeInt(defaultInt(p.getRotateCenterX()));
            w.writeInt(defaultInt(p.getRotateCenterY()));
            if (is202) {
                w.writeByte(p.getUnk3());
            }
            w.writeInt((int) defaultLong(p.getHitFlag()));
            long flag = defaultLong(p.getHitFlag());
            List<Spm.SPMHitArea> hits = defaultList(p.getHitRects());
            int hitIndex = 0;
            for (int i = 0; i < 32; i++) {
                if (((1L << i) & flag) != 0) {
                    if (hitIndex < hits.size()) {
                        writeBsdxHit(w, hits.get(hitIndex++));
                    } else {
                        w.writeInt(0);
                        writeRect(w, null);
                        w.writeInt(0);
                        w.writeInt(0);
                    }
                }
            }
            List<Spm.SPMChipData> chips = defaultList(p.getChipData());
            for (Spm.SPMChipData c : chips) {
                w.writeInt(defaultInt(c.getImageNo()));
                writeRect(w, c.getDstRect());
                w.writeInt(defaultInt(c.getChipWidth()));
                w.writeInt(defaultInt(c.getChipHeight()));
                writeRect(w, c.getSrcRect());
                w.writeInt((int) defaultLong(c.getDrawOption()));
                if (is202) {
                    w.writeByte(c.getUnk5());
                }
                w.writeInt((int) defaultLong(c.getDrawOptionValue()));
                w.writeInt(defaultInt(c.getOption()));
            }
        }
        writeImagesAndAnim(spm, w);
    }

    private static void writeImagesAndAnim(Spm spm, BinaryWriter w) throws IOException {
        List<Spm.SPMImageData> images = defaultList(spm.getImageData());
        w.writeInt(images.size());
        for (Spm.SPMImageData img : images) {
            w.writeNullTerminatedString(img == null ? "" : defaultStr(img.getImageName()));
        }

        int patPageNum = defaultInt(spm.getPatPageNum());
        w.writeInt(patPageNum);

        List<Spm.SPMAnimData> anims = defaultList(spm.getAnimData());
        w.writeInt(anims.size());
        for (Spm.SPMAnimData a : anims) {
            w.writeNullTerminatedString(defaultStr(a.getAnimName()));
            w.writeInt(defaultInt(a.getNumPat()));
            w.writeInt(defaultInt(a.getAnimRotateDirection()));
            w.writeInt(defaultInt(a.getAnimReverseDirection()));
            List<Spm.SPMPatData> pats = defaultList(a.getPatData());
            for (Spm.SPMPatData p : pats) {
                w.writeInt(defaultInt(p.getWaitFrame()));
                List<Integer> pages = defaultList(p.getPageNo());
                for (int i = 0; i < patPageNum; i++) {
                    int val = i < pages.size() && pages.get(i) != null ? pages.get(i) : 0;
                    w.writeInt(val);
                }
            }
        }
    }

    private static void writeRect(BinaryWriter w, Spm.SPMRect rect) throws IOException {
        if (rect == null) {
            w.writeInt(0);
            w.writeInt(0);
            w.writeInt(0);
            w.writeInt(0);
            return;
        }
        w.writeInt(defaultInt(rect.getLeft()));
        w.writeInt(defaultInt(rect.getTop()));
        w.writeInt(defaultInt(rect.getRight()));
        w.writeInt(defaultInt(rect.getBottom()));
    }

    private static void writeHit(BinaryWriter w, Spm.SPMHitArea hit, boolean clarias) throws IOException {
        short id = hit.getId();
        short shape = hit.getShapeType();
        w.writeShort(id);
        w.writeShort(shape);
        if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r) {
            writeRect(w, r.getRect());
            w.writeLong(r.getSkipped() == null ? 0L : r.getSkipped());
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea h) {
            w.writeInt(defaultInt(h.getXMin()));
            w.writeInt(defaultInt(h.getXMax()));
            w.writeInt(defaultInt(h.getYMin()));
            w.writeInt(defaultInt(h.getYMax()));
            w.writeInt(defaultInt(h.getZMin()));
            w.writeInt(defaultInt(h.getZMax()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect r) {
            w.writeInt(defaultInt(r.getCenterX()));
            w.writeInt(defaultInt(r.getCenterY()));
            w.writeBytes(defaultBytes(r.getSkippedBytes1(), 4));
            w.writeInt(defaultInt(r.getWidth()));
            w.writeInt(defaultInt(r.getHeight()));
            w.writeBytes(defaultBytes(r.getSkippedBytes2(), 4));
            w.writeInt(defaultInt(r.getAttrU16()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle c) {
            w.writeInt(defaultInt(c.getCenterX()));
            w.writeInt(defaultInt(c.getCenterY()));
            if (clarias) {
                w.writeBytes(defaultBytes(c.getSkippedBytes(), 4));
            } else {
                w.writeBytes(defaultBytes(c.getSkippedBytes(), 8));
            }
            w.writeInt(defaultInt(c.getRadius()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DLineSegment l) {
            w.writeInt(defaultInt(l.getX1()));
            w.writeInt(defaultInt(l.getY1()));
            w.writeInt(defaultInt(l.getX2()));
            w.writeInt(defaultInt(l.getY2()));
            w.writeBytes(defaultBytes(l.getSkippedBytes(), 8));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DDot d) {
            w.writeInt(defaultInt(d.getX()));
            w.writeInt(defaultInt(d.getY()));
            w.writeBytes(defaultBytes(d.getSkippedBytes(), 16));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CBox b) {
            w.writeInt(defaultInt(b.getMinX()));
            w.writeInt(defaultInt(b.getMinY()));
            w.writeInt(defaultInt(b.getMinZ()));
            w.writeInt(defaultInt(b.getMaxX()));
            w.writeInt(defaultInt(b.getMaxY()));
            w.writeInt(defaultInt(b.getMaxZ()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableBox b) {
            w.writeInt(defaultInt(b.getCenterX()));
            w.writeInt(defaultInt(b.getCenterY()));
            w.writeInt(defaultInt(b.getCenterZ()));
            w.writeInt(defaultInt(b.getSizeX()));
            w.writeInt(defaultInt(b.getSizeY()));
            w.writeInt(defaultInt(b.getSizeZ()));
            w.writeInt(defaultInt(b.getReserved0()));
            w.writeInt(defaultInt(b.getReserved1()));
            w.writeInt(defaultInt(b.getPropertyFlags()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CSphere s) {
            w.writeInt(defaultInt(s.getCenterX()));
            w.writeInt(defaultInt(s.getCenterY()));
            w.writeInt(defaultInt(s.getCenterZ()));
            w.writeInt(defaultInt(s.getRadius()));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
            writeBsdxHit(w, lr);
        } else if (hit instanceof LegacyRectHitArea lr2) {
            writeBsdxHit(w, lr2);
        } else {
            log.warn("Unknown hit type when writing: {}", hit.getClass());
        }
    }

    private static void writeBsdxHit(BinaryWriter w, Spm.SPMHitArea hit) throws IOException {
        int unk0 = 0, unk1 = 0, unk2 = 0;
        Spm.SPMRect rect = null;
        if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
            rect = lr.getHitRect();
            unk0 = defaultInt(lr.getUnk0());
            unk1 = defaultInt(lr.getUnk1());
            unk2 = defaultInt(lr.getUnk2());
        } else if (hit instanceof LegacyRectHitArea lr2) {
            rect = lr2.getHitRect();
            unk0 = defaultInt(lr2.getUnk0());
            unk1 = defaultInt(lr2.getUnk1());
            unk2 = defaultInt(lr2.getUnk2());
        }
        w.writeInt(unk0);
        writeRect(w, rect);
        w.writeInt(unk1);
        w.writeInt(unk2);
    }

    private static int defaultInt(Integer v) {
        return v == null ? 0 : v;
    }

    private static long defaultLong(Long v) {
        return v == null ? 0L : v;
    }

    private static String defaultStr(String v) {
        return v == null ? "" : v;
    }

    private static byte[] defaultBytes(byte[] arr, int expectedLen) {
        byte[] out = new byte[expectedLen];
        if (arr != null && arr.length > 0) {
            System.arraycopy(arr, 0, out, 0, Math.min(arr.length, expectedLen));
        }
        return out;
    }

    private static <T> List<T> defaultList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
