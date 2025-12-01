package com.karaik.spmviewer.controller.editor;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 确认 Editor FXML 能正常加载并生成控制器。
 */
class EditorFxmlLoadTest {

    @BeforeAll
    static void initFx() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // already initialized
        }
    }

    @Test
    void loadEditorFxml() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditorView.fxml"));
        Parent root = loader.load();
        assertNotNull(root);
        assertNotNull(loader.getController());
    }
}
