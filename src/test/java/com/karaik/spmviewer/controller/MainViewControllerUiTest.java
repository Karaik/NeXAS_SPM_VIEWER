package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private Object invokeMethod(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @Test
    void autoPlayCheckboxReflectsSettingsPreference() throws Exception {
        boolean original = Settings.isAutoPlayEnabled();
        try {
            Settings.setAutoPlayEnabled(false);
            LoadedView view = loadView();
            assertNotNull(view.autoPlayCheck());
            assertFalse(view.autoPlayCheck().isSelected(), "默认值应跟随设置中的自动播放偏好");

            view.autoPlayCheck().setSelected(true);
            runOnFxThread(() -> null);
            assertTrue(Settings.isAutoPlayEnabled(), "切换自动播放后应立即写回设置");
        } finally {
            Settings.setAutoPlayEnabled(original);
        }
    }

    @Test
    void toolbarUsesWrappedFlowPaneLayout() throws Exception {
        LoadedView view = loadView();
        assertNotNull(view.toolBar(), "应能找到顶部工具栏");
        assertNotNull(view.toolBarFlow(), "工具栏内部应使用 FlowPane 包装");
        assertTrue(view.toolBarFlow().getVgap() > 0, "FlowPane 应设置垂直间距以支持换行");
        assertTrue(view.toolBarFlow().getChildren().size() >= 5, "FlowPane 应包含多组工具控件");
    }

    @Test
    void displayTogglesRefreshPageAndImagePreview() throws Exception {
        LoadedView view = loadView();

        runOnFxThread(() -> {
            MainViewController controller = view.controller();
            CanvasController canvasController = (CanvasController) accessField(controller, "canvasController");
            @SuppressWarnings("unchecked")
            ListView<SpmEntry> spmListView = (ListView<SpmEntry>) accessField(controller, "spmListView");
            @SuppressWarnings("unchecked")
            ListView<String> imageList = (ListView<String>) accessField(controller, "imageList");
            @SuppressWarnings("unchecked")
            javafx.collections.ObservableList<SpmEntry> masterSpmList =
                    (javafx.collections.ObservableList<SpmEntry>) accessField(controller, "masterSpmList");
            CheckBox showHitboxCheck = (CheckBox) accessField(controller, "showHitboxCheck");
            CheckBox showChipBoundsCheck = (CheckBox) accessField(controller, "showChipBoundsCheck");
            CheckBox showPageBoundsCheck = (CheckBox) accessField(controller, "showPageBoundsCheck");

            Spm spm = createSampleSpm();
            WritableImage image = createSampleImage();
            SpmEntry entry = new SpmEntry(Path.of("sample.spm"));
            entry.setStatus(SpmEntry.Status.SUCCESS);
            entry.setSpm(spm);

            masterSpmList.setAll(entry);
            spmListView.getSelectionModel().select(entry);
            imageList.getItems().setAll("0: sample.png");

            canvasController.setCurrentSpm(spm, List.of(image));
            canvasController.setCurrentPageIndex(0);
            invokeMethod(controller, "renderCurrentPage", new Class<?>[0]);

            WritableImage pageBefore = canvasController.getCanvas().snapshot(null, null);
            showHitboxCheck.setSelected(true);
            WritableImage pageWithHitbox = canvasController.getCanvas().snapshot(null, null);
            showChipBoundsCheck.setSelected(true);
            showPageBoundsCheck.setSelected(true);
            WritableImage pageWithBounds = canvasController.getCanvas().snapshot(null, null);

            assertTrue(countPixels(pageWithHitbox, this::isMagenta) > 0, "页面模式下开启 hitbox 后应显示洋红色标记");
            assertTrue(countChangedPixels(pageBefore, pageWithHitbox) > 0, "开启 hitbox 后页面画面应发生变化");
            assertTrue(countChangedPixels(pageWithHitbox, pageWithBounds) > 0, "开启 bounds 后页面画面应继续发生变化");

            showHitboxCheck.setSelected(false);
            showChipBoundsCheck.setSelected(false);
            showPageBoundsCheck.setSelected(false);
            imageList.getSelectionModel().select(0);

            WritableImage previewBefore = canvasController.getCanvas().snapshot(null, null);
            showChipBoundsCheck.setSelected(true);
            showPageBoundsCheck.setSelected(true);
            WritableImage previewAfter = canvasController.getCanvas().snapshot(null, null);

            assertTrue(countPixels(previewBefore, this::isWhiteImagePixel) > 0, "整图预览应保留底图内容");
            assertTrue(countPixels(previewAfter, this::isWhiteImagePixel) > 0, "切换显示开关后整图预览不应被清空");
            assertTrue(countChangedPixels(previewBefore, previewAfter) > 0, "整图预览模式下开启 bounds 后应出现额外叠加内容");
            return null;
        });
    }

    private Spm createSampleSpm() {
        Spm spm = new Spm();
        spm.setSpmVersion("VER-2.00");
        spm.setNumPageData(1);
        spm.setNumImageData(1);

        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setNumChipData(2);
        page.setPageWidth(24);
        page.setPageHeight(24);
        page.setPageRect(rect(-10, -10, 10, 10));
        page.setRotateCenterX(0);
        page.setRotateCenterY(0);

        Spm.SPMChipData chipA = new Spm.SPMChipData();
        chipA.setImageNo(0);
        chipA.setDstRect(rect(-8, -8, -2, -2));
        chipA.setSrcRect(rect(0, 0, 6, 6));

        Spm.SPMChipData chipB = new Spm.SPMChipData();
        chipB.setImageNo(0);
        chipB.setDstRect(rect(2, 2, 8, 8));
        chipB.setSrcRect(rect(8, 8, 14, 14));

        TestHitArea hitArea = new TestHitArea();
        hitArea.setId((short) 1);
        hitArea.setShapeType((short) 99);

        page.setChipData(List.of(chipA, chipB));
        page.setHitRects(List.of(hitArea));
        spm.setPageData(List.of(page));

        Spm.SPMImageData imageData = new Spm.SPMImageData();
        imageData.setImageName("sample.png");
        spm.setImageData(List.of(imageData));
        return spm;
    }

    private WritableImage createSampleImage() {
        WritableImage image = new WritableImage(16, 16);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }
        return image;
    }

    private Spm.SPMRect rect(int left, int top, int right, int bottom) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(left);
        rect.setTop(top);
        rect.setRight(right);
        rect.setBottom(bottom);
        return rect;
    }

    private int countPixels(WritableImage image, PixelMatcher matcher) {
        var reader = image.getPixelReader();
        int matches = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (matcher.matches(reader.getColor(x, y))) {
                    matches++;
                }
            }
        }
        return matches;
    }

    private int countChangedPixels(WritableImage before, WritableImage after) {
        var beforeReader = before.getPixelReader();
        var afterReader = after.getPixelReader();
        int changed = 0;
        int width = (int) Math.min(before.getWidth(), after.getWidth());
        int height = (int) Math.min(before.getHeight(), after.getHeight());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!beforeReader.getColor(x, y).equals(afterReader.getColor(x, y))) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private boolean isMagenta(Color color) {
        return color.getOpacity() > 0.2 && color.getRed() > 0.7 && color.getGreen() < 0.4 && color.getBlue() > 0.7;
    }

    private boolean isWhiteImagePixel(Color color) {
        return color.getOpacity() > 0.9 && color.getRed() > 0.9 && color.getGreen() > 0.9 && color.getBlue() > 0.9;
    }

    private interface PixelMatcher {
        boolean matches(Color color);
    }

    private static final class TestHitArea extends Spm.SPMHitArea {
        @Override
        public void readInfo(BinaryReader reader) {
        }

        @Override
        public String getDisplayInfo() {
            return "test-hit";
        }

        @Override
        public void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY) {
            g.setStroke(Color.MAGENTA);
            g.setLineWidth(2.0);
            g.strokeRect(pageOriginX - 3, pageOriginY - 3, 6, 6);
        }
    }

    private record LoadedView(BorderPane root,
                              MainViewController controller,
                              CheckBox autoPlayCheck,
                              FlowPane toolBarFlow,
                              ToolBar toolBar) {}
}
