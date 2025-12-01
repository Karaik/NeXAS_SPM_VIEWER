package com.karaik.spmviewer.controller.editor;

import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorWorkflowSmokeTest {

    @BeforeAll
    static void initFx() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // already started
        }
    }

    @Test
    void loadEditorAndSelectChip() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditorView.fxml"));
        Parent root = loader.load();
        EditorViewController controller = loader.getController();

        Spm spm = buildMinimalSpm();
        SpmEntry entry = new SpmEntry(Path.of("dummy.spm"));
        entry.setStatus(SpmEntry.Status.SUCCESS);
        entry.setSpm(spm);

        Path tempDir = Files.createTempDirectory("editor-smoke");
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            controller.init(entry, tempDir);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "controller init timed out");

        ListView<?> pageList = (ListView<?>) getField(controller, "pageList");
        ListView<?> chipList = (ListView<?>) getField(controller, "chipList");
        assertEquals(1, pageList.getItems().size());
        assertEquals(0, pageList.getSelectionModel().getSelectedIndex());
        assertEquals(1, chipList.getItems().size());
        assertEquals(0, chipList.getSelectionModel().getSelectedIndex());
    }

    private Object getField(EditorViewController controller, String name) {
        try {
            Field f = controller.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(controller);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Spm buildMinimalSpm() {
        Spm spm = new Spm();
        Spm.SPMRect src = new Spm.SPMRect();
        src.setLeft(0);
        src.setTop(0);
        src.setRight(16);
        src.setBottom(16);

        Spm.SPMRect dst = new Spm.SPMRect();
        dst.setLeft(0);
        dst.setTop(0);
        dst.setRight(16);
        dst.setBottom(16);

        Spm.SPMChipData chip = new Spm.SPMChipData();
        chip.setImageNo(0);
        chip.setSrcRect(src);
        chip.setDstRect(dst);
        chip.setChipWidth(16);
        chip.setChipHeight(16);

        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setNumChipData(1);
        page.setPageWidth(64);
        page.setPageHeight(64);
        page.setChipData(List.of(chip));

        spm.setPageData(List.of(page));
        spm.setNumPageData(1);
        spm.setImageData(List.of());
        spm.setNumImageData(0);
        spm.setSpmVersion("SPM VER-2.00");
        return spm;
    }
}
