package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=0。
 */
public class DefaultHitArea extends com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea {

    @Override
    public double[] getBounds() {
        if (getXMin() == null) return new double[]{0, 0, 0, 0};
        double x1 = Math.min(getXMin(), getXMax()), x2 = Math.max(getXMin(), getXMax());
        double y1 = Math.min(getYMin(), getYMax()), y2 = Math.max(getYMin(), getYMax());
        return new double[]{x1, y1, x2, y2};
    }
}
