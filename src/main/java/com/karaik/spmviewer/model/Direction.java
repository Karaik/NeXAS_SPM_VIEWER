package com.karaik.spmviewer.model;

import java.util.List;

/**
 * 八向精灵图方向枚举。
 * 8 个方向覆盖 0°/45°/90°/135°/180°/225°/270°/315°，
 * mode=7 时对当前 page 应用对应角度的 canvas 旋转。
 * mode=3/4 时通过页面偏移区分方向，不应用旋转。
 */
public enum Direction {

    S(0, 0, "↓"),
    SW(1, 45, "↙"),
    W(2, 90, "←"),
    NW(3, 135, "↖"),
    N(4, 180, "↑"),
    NE(5, 225, "↗"),
    E(6, 270, "→"),
    SE(7, 315, "↘");

    private final int index;
    private final double rotateAngle;
    private final String symbol;

    Direction(int index, double rotateAngle, String symbol) {
        this.index = index;
        this.rotateAngle = rotateAngle;
        this.symbol = symbol;
    }

    public int getIndex() { return index; }
    public double getRotateAngle() { return rotateAngle; }
    public String getSymbol() { return symbol; }

    public static Direction fromIndex(int index) {
        for (Direction d : values()) {
            if (d.index == index) return d;
        }
        return S;
    }

    public static List<Direction> all() {
        return List.of(values());
    }

    @Override
    public String toString() {
        return symbol;
    }
}
