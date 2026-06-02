package com.karaik.spmviewer.spm.hitarea.clarias.c;

import com.karaik.spmviewer.spm.Spm;

/**
 * Clarias 兼容旧矩形结构（若需要固定矩形时使用）。
 */
public class LegacyRectHitArea extends com.karaik.spmviewer.spm.hitarea.bhe.c.LegacyRectHitArea {

    @Override
    public double[] getBounds() {
        Spm.SPMRect rect = getHitRect();
        if (rect == null) return new double[]{0, 0, 0, 0};
        double L = rect.getLeft() == null ? 0 : rect.getLeft();
        double T = rect.getTop() == null ? 0 : rect.getTop();
        double R = rect.getRight() == null ? 0 : rect.getRight();
        double B = rect.getBottom() == null ? 0 : rect.getBottom();
        return new double[]{Math.min(L, R), Math.min(T, B), Math.max(L, R), Math.max(T, B)};
    }
}
