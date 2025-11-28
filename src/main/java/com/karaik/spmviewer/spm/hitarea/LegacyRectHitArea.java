package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用于表示旧版、非多态的矩形 HitArea。
 * 兼容 bsdx (方言C) 和 VER-2.02 (方言B) 的数据结构。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LegacyRectHitArea extends Spm.SPMHitArea {
    private Spm.SPMRect hitRect;
    private Integer unk0;
    private Integer unk1;
    private Integer unk2;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        // 这个方法是空的，因为具体的解析逻辑在不同的“方言”解析器中实现，
        // 它们会直接设置这个对象的字段。
    }

    @Override
    public String getDisplayInfo() {
        if (hitRect == null) {
            return "Legacy Rect (data incomplete)";
        }
        return String.format("Legacy Rect [%d, %d, %d, %d], unk: %d,%d,%d",
                hitRect.getLeft(), hitRect.getTop(), hitRect.getRight(), hitRect.getBottom(),
                unk0, unk1, unk2);
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (hitRect == null) return;

        double x = pageOriginX + hitRect.getLeft();
        double y = pageOriginY + hitRect.getTop();
        double w = hitRect.getRight() - hitRect.getLeft();
        double h = hitRect.getBottom() - hitRect.getTop();

        // 使用半透明紫色来表示这是旧版格式的Hitbox
        g.setFill(new Color(0.5, 0, 1, 0.4));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.PURPLE);
        g.strokeRect(x, y, w, h);
    }
}
