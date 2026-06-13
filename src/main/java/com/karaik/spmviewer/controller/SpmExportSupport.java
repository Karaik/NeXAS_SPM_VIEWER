package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.spm.Spm;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SPM 导出辅助逻辑。
 * 这里不依赖界面状态，只负责根据 SPM 数据计算范围、展开动画槽位并生成导出图像。
 */
final class SpmExportSupport {

    private SpmExportSupport() {
    }

    static List<AnimationSlotRef> collectAnimationSlots(Spm spm) {
        if (spm == null) {
            return List.of();
        }
        List<Spm.SPMAnimData> anims = Optional.ofNullable(spm.getAnimData()).orElse(List.of());
        int patPageLimit = Optional.ofNullable(spm.getPatPageNum()).orElse(0);
        List<AnimationSlotRef> slots = new ArrayList<>();
        for (int animIndex = 0; animIndex < anims.size(); animIndex++) {
            Spm.SPMAnimData anim = anims.get(animIndex);
            List<Spm.SPMPatData> pats = Optional.ofNullable(anim.getPatData()).orElse(List.of());
            for (int patIndex = 0; patIndex < pats.size(); patIndex++) {
                Spm.SPMPatData pat = pats.get(patIndex);
                List<Integer> pageNos = Optional.ofNullable(pat.getPageNo()).orElse(List.of());
                int limit = patPageLimit > 0 ? Math.min(patPageLimit, pageNos.size()) : pageNos.size();
                for (int slotIndex = 0; slotIndex < limit; slotIndex++) {
                    Integer pageIndex = pageNos.get(slotIndex);
                    if (pageIndex == null) {
                        continue;
                    }
                    slots.add(new AnimationSlotRef(animIndex, patIndex, slotIndex, pageIndex));
                }
            }
        }
        return slots;
    }

    static IntRect computePageBounds(Spm.SPMPageData page) {
        if (page == null) {
            return null;
        }
        Spm.SPMRect pageRect = page.getPageRect();
        if (pageRect != null) {
            int left = Math.min(safe(pageRect.getLeft()), safe(pageRect.getRight()));
            int top = Math.min(safe(pageRect.getTop()), safe(pageRect.getBottom()));
            int right = Math.max(safe(pageRect.getLeft()), safe(pageRect.getRight()));
            int bottom = Math.max(safe(pageRect.getTop()), safe(pageRect.getBottom()));
            IntRect rect = IntRect.fromCorners(left, top, right, bottom);
            if (!rect.isEmpty()) {
                return rect;
            }
        }

        int pageWidth = safe(page.getPageWidth());
        int pageHeight = safe(page.getPageHeight());
        if (pageWidth > 0 && pageHeight > 0) {
            int pivotX = safe(page.getRotateCenterX());
            int pivotY = safe(page.getRotateCenterY());
            int left = pivotX - pageWidth / 2;
            int top = pivotY - pageHeight / 2;
            return new IntRect(left, top, pageWidth, pageHeight);
        }

        return computePageChipUnion(page);
    }

    static IntRect computeChipBounds(Spm.SPMChipData chip) {
        if (chip == null || chip.getDstRect() == null) {
            return null;
        }
        Spm.SPMRect dst = chip.getDstRect();
        int left = Math.min(safe(dst.getLeft()), safe(dst.getRight()));
        int top = Math.min(safe(dst.getTop()), safe(dst.getBottom()));
        int right = Math.max(safe(dst.getLeft()), safe(dst.getRight()));
        int bottom = Math.max(safe(dst.getTop()), safe(dst.getBottom()));
        IntRect rect = IntRect.fromCorners(left, top, right, bottom);
        return rect.isEmpty() ? null : rect;
    }

    static WritableImage renderPageImage(Spm.SPMPageData page, List<Image> images, IntRect bounds) {
        if (page == null || bounds == null || bounds.isEmpty()) {
            return null;
        }
        Canvas canvas = new Canvas(bounds.width(), bounds.height());
        GraphicsContext g = canvas.getGraphicsContext2D();
        drawPage(g, page, images, bounds);
        return snapshotTransparent(canvas);
    }

    static WritableImage renderChipImage(Spm.SPMChipData chip, List<Image> images) {
        IntRect bounds = computeChipBounds(chip);
        if (chip == null || bounds == null) {
            return null;
        }
        Canvas canvas = new Canvas(bounds.width(), bounds.height());
        GraphicsContext g = canvas.getGraphicsContext2D();
        drawChip(g, chip, images, -bounds.x(), -bounds.y());
        return snapshotTransparent(canvas);
    }

    static WritableImage renderImageBoundsOverlay(Spm spm, List<Image> images, int imageIndex, boolean chipMode) {
        if (spm == null || imageIndex < 0 || imageIndex >= images.size()) {
            return null;
        }
        Image base = images.get(imageIndex);
        if (base == null) {
            return null;
        }
        Canvas canvas = new Canvas(base.getWidth(), base.getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        boolean hasOverlay = false;

        List<Spm.SPMPageData> pages = Optional.ofNullable(spm.getPageData()).orElse(List.of());
        if (chipMode) {
            g.setStroke(javafx.scene.paint.Color.CYAN);
            g.setLineWidth(2.0);
            g.setFill(javafx.scene.paint.Color.color(0, 1, 1, 0.2));
            for (Spm.SPMPageData page : pages) {
                for (Spm.SPMChipData chip : Optional.ofNullable(page.getChipData()).orElse(List.of())) {
                    if (chip.getImageNo() == null || !chip.getImageNo().equals(imageIndex)) {
                        continue;
                    }
                    IntRect srcBounds = rectFrom(chip.getSrcRect());
                    if (srcBounds == null) {
                        continue;
                    }
                    g.fillRect(srcBounds.x(), srcBounds.y(), srcBounds.width(), srcBounds.height());
                    g.strokeRect(srcBounds.x(), srcBounds.y(), srcBounds.width(), srcBounds.height());
                    drawCenterMarker(g, srcBounds);
                    hasOverlay = true;
                }
            }
        } else {
            g.setStroke(javafx.scene.paint.Color.ORANGE);
            g.setLineWidth(1.5);
            for (Spm.SPMPageData page : pages) {
                IntRect bounds = computeImagePageBounds(page, imageIndex);
                if (bounds != null) {
                    g.strokeRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
                    drawCenterMarker(g, bounds);
                    hasOverlay = true;
                }
            }
        }
        return hasOverlay ? snapshotTransparent(canvas) : null;
    }

    static String stripExtension(String name) {
        if (name == null || name.isBlank()) {
            return "spm";
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    static String sanitizeFileComponent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "item";
        }
        String sanitized = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        sanitized = sanitized.replaceAll("\\s+", "_");
        while (sanitized.contains("__")) {
            sanitized = sanitized.replace("__", "_");
        }
        return sanitized.isBlank() ? "item" : sanitized;
    }

    private static IntRect computePageChipUnion(Spm.SPMPageData page) {
        List<Spm.SPMChipData> chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;
        for (Spm.SPMChipData chip : chips) {
            IntRect chipBounds = computeChipBounds(chip);
            if (chipBounds == null) {
                continue;
            }
            minX = Math.min(minX, chipBounds.x());
            minY = Math.min(minY, chipBounds.y());
            maxX = Math.max(maxX, chipBounds.right());
            maxY = Math.max(maxY, chipBounds.bottom());
            found = true;
        }
        return found ? IntRect.fromCorners(minX, minY, maxX, maxY) : null;
    }

    private static IntRect computeImagePageBounds(Spm.SPMPageData page, int imageIndex) {
        List<Spm.SPMChipData> chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;
        for (Spm.SPMChipData chip : chips) {
            if (chip.getImageNo() == null || !chip.getImageNo().equals(imageIndex)) {
                continue;
            }
            IntRect srcBounds = rectFrom(chip.getSrcRect());
            if (srcBounds == null) {
                continue;
            }
            minX = Math.min(minX, srcBounds.x());
            minY = Math.min(minY, srcBounds.y());
            maxX = Math.max(maxX, srcBounds.right());
            maxY = Math.max(maxY, srcBounds.bottom());
            found = true;
        }
        return found ? IntRect.fromCorners(minX, minY, maxX, maxY) : null;
    }

    private static void drawPage(GraphicsContext g, Spm.SPMPageData page, List<Image> images, IntRect bounds) {
        for (Spm.SPMChipData chip : Optional.ofNullable(page.getChipData()).orElse(List.of())) {
            drawChip(g, chip, images, -bounds.x(), -bounds.y());
        }
    }

    private static void drawChip(GraphicsContext g, Spm.SPMChipData chip, List<Image> images, double offsetX, double offsetY) {
        if (chip == null || chip.getDstRect() == null || chip.getSrcRect() == null) {
            return;
        }
        Integer imageNo = chip.getImageNo();
        if (imageNo == null || imageNo < 0 || imageNo >= images.size()) {
            return;
        }
        Image image = images.get(imageNo);
        if (image == null) {
            return;
        }

        IntRect dst = computeChipBounds(chip);
        if (dst == null) {
            return;
        }
        IntRect src = rectFrom(chip.getSrcRect());
        if (src == null || src.isEmpty()) {
            return;
        }

        g.drawImage(image,
                src.x(), src.y(), src.width(), src.height(),
                dst.x() + offsetX, dst.y() + offsetY, dst.width(), dst.height());
    }

    private static IntRect rectFrom(Spm.SPMRect rect) {
        if (rect == null) {
            return null;
        }
        int left = Math.min(safe(rect.getLeft()), safe(rect.getRight()));
        int top = Math.min(safe(rect.getTop()), safe(rect.getBottom()));
        int right = Math.max(safe(rect.getLeft()), safe(rect.getRight()));
        int bottom = Math.max(safe(rect.getTop()), safe(rect.getBottom()));
        IntRect out = IntRect.fromCorners(left, top, right, bottom);
        return out.isEmpty() ? null : out;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private static void drawCenterMarker(GraphicsContext g, IntRect bounds) {
        IntRect marker = computeCenterMarker(bounds);
        g.setFill(Color.color(1.0, 0.0, 0.0, 0.95));
        g.fillRect(marker.x(), marker.y(), marker.width(), marker.height());
    }

    /**
     * bounds 使用的是像素边界坐标，因此中心可能落在像素中点，也可能落在像素缝。
     * 宽高为奇数时中心是一个像素点；宽高为偶数时中心会跨两个像素。
     */
    private static IntRect computeCenterMarker(IntRect bounds) {
        int markerWidth = (bounds.width() % 2 == 0) ? 2 : 1;
        int markerHeight = (bounds.height() % 2 == 0) ? 2 : 1;
        int markerX = bounds.x() + bounds.width() / 2 - (markerWidth == 2 ? 1 : 0);
        int markerY = bounds.y() + bounds.height() / 2 - (markerHeight == 2 ? 1 : 0);
        return new IntRect(markerX, markerY, markerWidth, markerHeight);
    }

    private static WritableImage snapshotTransparent(Canvas canvas) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    record AnimationSlotRef(int animIndex, int patIndex, int slotIndex, int pageIndex) {
    }

    record IntRect(int x, int y, int width, int height) {
        static IntRect fromCorners(int left, int top, int right, int bottom) {
            return new IntRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
        }

        boolean isEmpty() {
            return width <= 0 || height <= 0;
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }
}
