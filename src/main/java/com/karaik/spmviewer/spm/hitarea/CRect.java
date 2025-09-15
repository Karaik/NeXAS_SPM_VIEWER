package com.karaik.spmviewer.spm.hitarea;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.canvas.GraphicsContext;
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

    }
}