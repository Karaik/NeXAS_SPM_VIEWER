package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.model.Direction;
import lombok.extern.slf4j.Slf4j;

/**
 * 根据 animRotateDirection 的模式解析方向对应的页面索引或画布变换。
 *
 * rotateDirection 模式：
 *   0 — 无方向，动画按页顺序播放
 *   3 — 8 方向分页，每姿态 8 页为一组（动画 patNo 间隔 8），页面 0-7 对应各方向，5-7 为 1-3 的镜像
 *   4 — 16 方向分页，每姿态 16 页为一组（动画 patNo 间隔 16），页面 0-7 为 8 方向，8-15 为镜像页
 *   7 — 单页自旋转，对当前页应用 0°/45°/…/315° 的 canvas 旋转变换
 */
@Slf4j
public class DirectionResolver {

    private DirectionResolver() {}

    public static boolean supportsDirection(Integer rotateDirection) {
        if (rotateDirection == null) return false;
        return rotateDirection == 3 || rotateDirection == 4 || rotateDirection == 7;
    }

    public static DirectionMode getMode(Integer rotateDirection) {
        if (rotateDirection == null) return DirectionMode.NONE;
        return switch (rotateDirection) {
            case 3, 4 -> DirectionMode.PAGE_OFFSET;
            case 7 -> DirectionMode.CANVAS_TRANSFORM;
            default -> DirectionMode.NONE;
        };
    }

    /**
     * 动画 patNo 的组跨度（用于将当前页归组）。
     * mode=3: 每组 8 页，mode=4: 每组 16 页。
     */
    public static int getGroupSpan(Integer rotateDirection) {
        if (rotateDirection == null) return 1;
        return switch (rotateDirection) {
            case 3 -> 8;
            case 4 -> 16;
            default -> 1;
        };
    }

    /**
     * PAGE_OFFSET 模式下，方向切换时的页面步幅。
     * 8 方向页面在组内总是连续排列（0-7），因此步幅为 1。
     */
    public static int getDirectionStride(Integer rotateDirection) {
        return 1;
    }

    /**
     * PAGE_OFFSET 模式下组内方向数量。
     * mode=3: 8，mode=4: 16。
     */
    public static int getDirectionCount(Integer rotateDirection) {
        if (rotateDirection == null) return 0;
        return switch (rotateDirection) {
            case 3 -> 8;
            case 4 -> 16;
            default -> 0;
        };
    }

    /**
     * 判断该方向索引是否为镜像方向（页面本身不存在，需对源页水平翻转）。
     * mode=3: idx 5-7 镜像自 1-3
     * mode=4: idx 9-15 镜像自 1-7
     */
    public static boolean isMirrorDirection(Integer rotateDirection, int dirIndex) {
        if (rotateDirection == null) return false;
        return switch (rotateDirection) {
            case 3 -> dirIndex >= 5;
            case 4 -> dirIndex >= 9;
            default -> false;
        };
    }

    /**
     * 返回镜像方向的源方向索引（即被镜像的原始页面）。
     * mode=3: 5→3, 6→2, 7→1  （逆向映射）
     * mode=4: 9→7, 10→6, ..., 15→1  （逆向映射）
     */
    public static int getMirrorSourceIndex(Integer rotateDirection, int dirIndex) {
        if (rotateDirection == null) return dirIndex;
        return switch (rotateDirection) {
            case 3 -> 8 - dirIndex;   // 5→3, 6→2, 7→1
            case 4 -> 16 - dirIndex;  // 9→7, 10→6, ... 15→1
            default -> dirIndex;
        };
    }

    /**
     * 返回方向按钮的显示标签。
     * mode=3/7: 8 方向符号（↓↙←↖↑↗→↘）
     * mode=4: 0°=↓ 45°=↙ 90°=← … 337°=↘，中间角度显示度数
     */
    public static String getDirectionLabel(Integer rotateDirection, int dirIndex) {
        if (rotateDirection == null || rotateDirection == 3 || rotateDirection == 7) {
            return Direction.fromIndex(dirIndex % 8).getSymbol();
        }
        // mode 4: 16 方向，22.5° 间隔
        String[] labels = {"↓","22°","↙","67°","←","112°","↖","157°","↑","202°","↗","247°","→","292°","↘","337°"};
        return dirIndex < labels.length ? labels[dirIndex] : String.valueOf(dirIndex);
    }

    public enum DirectionMode {
        NONE,
        PAGE_OFFSET,
        CANVAS_TRANSFORM
    }
}
