package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=11。
 */
public class CSphere extends com.karaik.spmviewer.spm.hitarea.bhe.c.CSphere {

    @Override
    public double[] getBounds() {
        if (getCenterX() == null || getCenterY() == null || getRadius() == null) return new double[]{0, 0, 0, 0};
        double r = Math.abs(getRadius());
        return new double[]{getCenterX() - r, getCenterY() - r, getCenterX() + r, getCenterY() + r};
    }
}
