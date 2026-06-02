package com.karaik.spmviewer.spm.hitarea.bhe.c;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CRect extends Spm.SPMHitArea {
    private Spm.SPMRect rect;
    private Long skipped;

    @Override
    public void readInfo(BinaryReader reader) throws Exception {
        rect = new Spm.SPMRect();
        rect.setLeft(reader.readInt());
        rect.setTop(reader.readInt());
        rect.setRight(reader.readInt());
        rect.setBottom(reader.readInt());
        skipped = reader.readLong();
    }

    @Override
    public String getDisplayInfo() {
        if (rect == null) return "Rect (incomplete)";
        return String.format("Rect [%d, %d, %d, %d]", rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
    }

    @Override
    public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
        if (rect == null) return;
        int L = rect.getLeft() == null ? 0 : rect.getLeft();
        int T = rect.getTop() == null ? 0 : rect.getTop();
        int R = rect.getRight() == null ? 0 : rect.getRight();
        int B = rect.getBottom() == null ? 0 : rect.getBottom();

        double x = pageOriginX + Math.min(L, R);
        double y = pageOriginY + Math.min(T, B);
        double w = Math.abs(R - L);
        double h = Math.abs(B - T);

        g.setFill(new Color(1, 0, 0, 0.25));
        g.fillRect(x, y, w, h);
        g.setStroke(Color.RED);
        g.strokeRect(x, y, w, h);
    }

    public double[] getBounds() {
        if (rect == null) return new double[]{0, 0, 0, 0};
        int L = rect.getLeft() == null ? 0 : rect.getLeft();
        int T = rect.getTop() == null ? 0 : rect.getTop();
        int R = rect.getRight() == null ? 0 : rect.getRight();
        int B = rect.getBottom() == null ? 0 : rect.getBottom();
        return new double[]{Math.min(L, R), Math.min(T, B), Math.max(L, R), Math.max(T, B)};
    }
}
