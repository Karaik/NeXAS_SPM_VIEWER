package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=7。
 */
public class C2DLineSegment extends com.karaik.spmviewer.spm.hitarea.bhe.c.C2DLineSegment {

    @Override
    public double[] getBounds() {
        if (getX1() == null || getY1() == null || getX2() == null || getY2() == null) return new double[]{0, 0, 0, 0};
        double x1 = Math.min(getX1(), getX2()), x2 = Math.max(getX1(), getX2());
        double y1 = Math.min(getY1(), getY2()), y2 = Math.max(getY1(), getY2());
        return new double[]{x1, y1, x2, y2};
    }
}
