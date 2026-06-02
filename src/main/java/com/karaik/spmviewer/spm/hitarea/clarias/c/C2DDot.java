package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=8。
 */
public class C2DDot extends com.karaik.spmviewer.spm.hitarea.bhe.c.C2DDot {

    @Override
    public double[] getBounds() {
        if (getX() == null || getY() == null) return new double[]{0, 0, 0, 0};
        return new double[]{getX() - 4, getY() - 4, getX() + 4, getY() + 4};
    }
}
