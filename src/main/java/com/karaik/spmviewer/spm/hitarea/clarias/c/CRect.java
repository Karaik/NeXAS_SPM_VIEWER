package com.karaik.spmviewer.spm.hitarea.clarias.c;

import com.karaik.spmviewer.spm.Spm;

/**
 * Clarias 命中体：同 BHE，作为未知形状的回退。
 */
public class CRect extends com.karaik.spmviewer.spm.hitarea.bhe.c.CRect {

    @Override
    public double[] getBounds() {
        Spm.SPMRect rect = getRect();
        if (rect == null) return new double[]{0, 0, 0, 0};
        double L = rect.getLeft() == null ? 0 : rect.getLeft();
        double T = rect.getTop() == null ? 0 : rect.getTop();
        double R = rect.getRight() == null ? 0 : rect.getRight();
        double B = rect.getBottom() == null ? 0 : rect.getBottom();
        return new double[]{Math.min(L, R), Math.min(T, B), Math.max(L, R), Math.max(T, B)};
    }
}
