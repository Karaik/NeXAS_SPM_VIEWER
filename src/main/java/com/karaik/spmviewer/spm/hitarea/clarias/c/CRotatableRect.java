package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=1。
 */
public class CRotatableRect extends com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect {

    @Override
    public double[] getBounds() {
        if (getCenterX() == null || getCenterY() == null || getWidth() == null || getHeight() == null) return new double[]{0, 0, 0, 0};
        double hw = getWidth() / 2.0, hh = getHeight() / 2.0;
        return new double[]{getCenterX() - hw, getCenterY() - hh, getCenterX() + hw, getCenterY() + hh};
    }
}
