package com.karaik.spmviewer.controller.editor;

import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.c.CRect;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * 针对 Edit 模式中的几何/命令栈/图片编辑基础逻辑的快速校验。
 */
class EditorGeometryTest {

    @Test
    void commandUndoRedoWorks() {
        AtomicInteger counter = new AtomicInteger();
        EditorViewController.Command cmd = new EditorViewController.Command(
                counter::incrementAndGet,
                counter::decrementAndGet
        );

        cmd.redo();
        assertEquals(1, counter.get());
        cmd.undo();
        assertEquals(0, counter.get());
    }

    @Test
    void moveChipByUpdatesDstRect() {
        EditorViewController controller = new EditorViewController();
        Spm.SPMChipData chip = new Spm.SPMChipData();
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(10);
        rect.setTop(20);
        rect.setRight(30);
        rect.setBottom(40);
        chip.setDstRect(rect);

        controller.moveChipBy(chip, 5, -3);

        assertEquals(15, chip.getDstRect().getLeft());
        assertEquals(17, chip.getDstRect().getTop());
        assertEquals(35, chip.getDstRect().getRight());
        assertEquals(37, chip.getDstRect().getBottom());
    }

    @Test
    void moveHitByUpdatesRectHitbox() {
        EditorViewController controller = new EditorViewController();
        CRect hit = new CRect();
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(1);
        rect.setTop(2);
        rect.setRight(5);
        rect.setBottom(6);
        hit.setRect(rect);

        controller.moveHitBy(hit, 3, -1);

        assertEquals(4, hit.getRect().getLeft());
        assertEquals(1, hit.getRect().getTop());
        assertEquals(8, hit.getRect().getRight());
        assertEquals(5, hit.getRect().getBottom());
    }

    @Test
    void copyImageProducesIndependentBuffer() throws Exception {
        EditorViewController controller = new EditorViewController();
        WritableImage src = new WritableImage(2, 2);
        src.getPixelWriter().setColor(0, 0, Color.RED);

        Method copyMethod = EditorViewController.class.getDeclaredMethod("copyImage", WritableImage.class);
        copyMethod.setAccessible(true);
        WritableImage copy = (WritableImage) copyMethod.invoke(controller, src);

        assertNotSame(src, copy);
        copy.getPixelWriter().setColor(0, 0, Color.BLUE);

        // 原图未被修改，表示撤销栈快照使用的复制逻辑有效
        assertEquals(Color.RED, src.getPixelReader().getColor(0, 0));
    }
}
