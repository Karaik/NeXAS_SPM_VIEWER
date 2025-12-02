package com.karaik.spmviewer.controller.editor;

import javafx.scene.image.PixelReader;
import javafx.scene.canvas.Canvas;
import javafx.scene.Group;
import javafx.scene.transform.Scale;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorSpritePatchTest {

    @BeforeAll
    static void initFx() throws Exception {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void patchSpriteIntoImageReplacesRegion() throws Exception {
        EditorViewController controller = new EditorViewController();
        // provide a minimal canvas controller so loadedImages is available
        var cc = new com.karaik.spmviewer.controller.CanvasController(new Canvas(), new Group(), new Scale());
        var field = EditorViewController.class.getDeclaredField("canvasController");
        field.setAccessible(true);
        field.set(controller, cc);

        WritableImage base = new WritableImage(4, 4);
        base.getPixelWriter().setColor(1, 1, Color.RED);

        WritableImage sprite = new WritableImage(2, 2);
        sprite.getPixelWriter().setColor(0, 0, Color.BLUE);
        sprite.getPixelWriter().setColor(1, 1, Color.BLUE);

        cc.getLoadedImages().add(base);

        // use reflection to access patchSpriteIntoImage (private)
        var m = EditorViewController.class.getDeclaredMethod("patchSpriteIntoImage", int.class, WritableImage.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(controller, 0, sprite, 1, 1);

        PixelReader pr = cc.getLoadedImages().get(0).getPixelReader();
        assertEquals(Color.BLUE, pr.getColor(1, 1));
        assertEquals(Color.BLUE, pr.getColor(2, 2));
        // untouched pixel remains default (transparent)
        assertEquals(Color.TRANSPARENT, pr.getColor(0, 0));
    }
}
