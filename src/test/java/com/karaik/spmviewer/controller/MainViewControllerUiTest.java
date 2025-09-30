package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainViewControllerUiTest {

    private static final AtomicBoolean TOOLKIT_INITIALIZED = new AtomicBoolean(false);

    @BeforeAll
    void initToolkit() throws Exception {
        if (TOOLKIT_INITIALIZED.get()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
        latch.await();
        TOOLKIT_INITIALIZED.set(true);
    }

    @AfterEach
    void flushFxEvents() throws Exception {
        runOnFxThread(() -> null);
    }

    private <T> T runOnFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        try {
            return task.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw e;
        }
    }

    private LoadedView loadView() throws Exception {
        return runOnFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            BorderPane root = loader.load();
            MainViewController controller = loader.getController();
            CheckBox autoPlay = (CheckBox) accessField(controller, "autoPlayCheck");
            ToolBar toolBar = root.getTop() instanceof VBox vbox
                    ? vbox.getChildren().stream().filter(ToolBar.class::isInstance).map(ToolBar.class::cast).findFirst().orElse(null)
                    : null;
            FlowPane flow = toolBar == null || toolBar.getItems().isEmpty() ? null : (FlowPane) toolBar.getItems().get(0);
            return new LoadedView(root, controller, autoPlay, flow, toolBar);
        });
    }

    private Object accessField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void autoPlayCheckboxReflectsSettingsPreference() throws Exception {
        boolean original = Settings.isAutoPlayEnabled();
        try {
            Settings.setAutoPlayEnabled(false);
            LoadedView view = loadView();
            assertNotNull(view.autoPlayCheck());
            assertFalse(view.autoPlayCheck().isSelected(), "默认应遵循用户偏好 (false)");

            view.autoPlayCheck().setSelected(true);
            runOnFxThread(() -> null); // flush listener
            assertTrue(Settings.isAutoPlayEnabled(), "切换为开启后应立即写入设置");
        } finally {
            Settings.setAutoPlayEnabled(original);
        }
    }

    @Test
    void toolbarUsesWrappedFlowPaneLayout() throws Exception {
        LoadedView view = loadView();
        assertNotNull(view.toolBar(), "应能找到工具栏");
        assertNotNull(view.toolBarFlow(), "工具栏内部应使用 FlowPane 包装");
        assertTrue(view.toolBarFlow().getVgap() > 0, "FlowPane 应设置垂直间距以允许换行");
        assertTrue(view.toolBarFlow().getChildren().size() >= 5, "FlowPane 应包含不同的控制分组");
    }

    private record LoadedView(BorderPane root,
                              MainViewController controller,
                              CheckBox autoPlayCheck,
                              FlowPane toolBarFlow,
                              ToolBar toolBar) {}
}

