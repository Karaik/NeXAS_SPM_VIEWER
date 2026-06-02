package com.karaik.spmviewer.spm.hitarea.clarias.c;

/**
 * Clarias 命中体：同 BHE，对应 shapeType=10。
 */
public class CRotatableBox extends com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableBox {

    @Override
    public double[] getBounds() {
        if (getCenterX() == null || getCenterY() == null || getSizeX() == null || getSizeY() == null) return new double[]{0, 0, 0, 0};
        double hx = getSizeX() / 2.0, hy = getSizeY() / 2.0;
        return new double[]{getCenterX() - hx, getCenterY() - hy, getCenterX() + hx, getCenterY() + hy};
    }
}
