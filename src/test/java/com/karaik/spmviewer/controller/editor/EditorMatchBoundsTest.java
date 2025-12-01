package com.karaik.spmviewer.controller.editor;

import com.karaik.spmviewer.spm.Spm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorMatchBoundsTest {

    @Test
    void matchBoundsUsesSrcSizeAndKeepsTopLeft() {
        EditorViewController controller = new EditorViewController();
        Spm.SPMChipData chip = new Spm.SPMChipData();
        Spm.SPMRect src = new Spm.SPMRect();
        src.setLeft(10); src.setTop(5); src.setRight(30); src.setBottom(25); // 20x20
        Spm.SPMRect dst = new Spm.SPMRect();
        dst.setLeft(100); dst.setTop(200); dst.setRight(110); dst.setBottom(210); // will be overwritten
        chip.setSrcRect(src);
        chip.setDstRect(dst);

        controller.matchBoundsToSprite(chip);

        assertEquals(100, chip.getDstRect().getLeft());
        assertEquals(200, chip.getDstRect().getTop());
        assertEquals(120, chip.getDstRect().getRight());
        assertEquals(220, chip.getDstRect().getBottom());
        assertEquals(20, chip.getChipWidth());
        assertEquals(20, chip.getChipHeight());
    }
}
