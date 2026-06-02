package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=9。
 */
public class CBox extends com.karaik.spmviewer.spm.hitarea.bhe.c.CBox {

    @Override
    public double[] getBounds() {
        if (getMinX() == null) return new double[]{0, 0, 0, 0};
        double x1 = Math.min(getMinX(), getMaxX()), x2 = Math.max(getMinX(), getMaxX());
        double y1 = Math.min(getMinY(), getMaxY()), y2 = Math.max(getMinY(), getMaxY());
        return new double[]{x1, y1, x2, y2};
    }
}
