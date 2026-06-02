package com.karaik.spmviewer.controller.editor;

import com.karaik.spmviewer.controller.CanvasController;
import com.karaik.spmviewer.model.Direction;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 命中体可视化编辑器 — 处理画布上的选中、拖拽移动等交互。
 */
@Slf4j
public class HitAreaEditor implements EventHandler<MouseEvent> {

    private final Canvas canvas;
    private final CanvasController canvasController;
    private Consumer<Integer> onSelectionChanged;
    private Runnable onModified;

    private boolean enabled = false;
    private int selectedIndex = -1;
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private double[] dragOriginalBounds;

    private static final double HANDLE_SIZE = 6;
    private static final double MIN_SIZE = 2;

    public HitAreaEditor(Canvas canvas, CanvasController canvasController) {
        this.canvas = canvas;
        this.canvasController = canvasController;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            selectedIndex = -1;
            dragging = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public int getSelectedIndex() { return selectedIndex; }

    public void setOnSelectionChanged(Consumer<Integer> callback) { this.onSelectionChanged = callback; }
    public void setOnModified(Runnable callback) { this.onModified = callback; }

    public void selectNone() {
        selectedIndex = -1;
        if (onSelectionChanged != null) onSelectionChanged.accept(-1);
    }

    @Override
    public void handle(MouseEvent event) {
        if (!enabled) return;

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            onMousePressed(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            onMouseDragged(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            onMouseReleased(event);
        }
    }

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        Spm.SPMPageData page = getCurrentPage();
        if (page == null) return;

        List<Spm.SPMHitArea> hitAreas = Optional.ofNullable(page.getHitRects()).orElse(List.of());
        if (hitAreas.isEmpty()) return;

        double pageX = toPageX(event.getX());
        double pageY = toPageY(event.getY());

        int hit = hitTest(hitAreas, pageX, pageY);
        if (hit >= 0) {
            selectedIndex = hit;
            dragStartX = event.getX();
            dragStartY = event.getY();
            dragOriginalBounds = hitAreas.get(hit).getBounds();
            dragging = true;
        } else {
            selectedIndex = -1;
            dragging = false;
        }

        if (onSelectionChanged != null) onSelectionChanged.accept(selectedIndex);
    }

    private void onMouseDragged(MouseEvent event) {
        if (!dragging || selectedIndex < 0) return;

        Spm.SPMPageData page = getCurrentPage();
        if (page == null) return;

        List<Spm.SPMHitArea> hitAreas = Optional.ofNullable(page.getHitRects()).orElse(List.of());
        if (selectedIndex >= hitAreas.size()) return;

        double dx = toPageDelta(event.getX() - dragStartX);
        double dy = toPageDelta(event.getY() - dragStartY);
        if (dx == 0 && dy == 0 && event.getX() == dragStartX && event.getY() == dragStartY) return;

        moveHitArea(hitAreas.get(selectedIndex), dragOriginalBounds, dx, dy);
        dragStartX = event.getX();
        dragStartY = event.getY();
        dragOriginalBounds = hitAreas.get(selectedIndex).getBounds();

        if (onModified != null) onModified.run();
    }

    private void onMouseReleased(MouseEvent event) {
        dragging = false;
    }

    private int hitTest(List<Spm.SPMHitArea> hitAreas, double pageX, double pageY) {
        for (int i = hitAreas.size() - 1; i >= 0; i--) {
            double[] b = hitAreas.get(i).getBounds();
            if (pageX >= b[0] && pageX <= b[2] && pageY >= b[1] && pageY <= b[3]) {
                return i;
            }
        }
        return -1;
    }

    private void moveHitArea(Spm.SPMHitArea hit, double[] origBounds, double dx, double dy) {
        // 通过反射方式修改坐标。每个子类有不同的坐标字段，这里使用统一接口。
        // 策略：利用现有字段，按类型逐一处理。
        String className = hit.getClass().getSimpleName();
        try {
            switch (className) {
                case "DefaultHitArea" -> moveDefaultOrCBox(hit, origBounds, dx, dy, "xMin", "xMax", "yMin", "yMax");
                case "CRect", "LegacyRectHitArea" -> moveRect(hit, origBounds, dx, dy);
                case "CCircle" -> moveCircle(hit, origBounds, dx, dy);
                case "CBox" -> moveDefaultOrCBox(hit, origBounds, dx, dy, "minX", "maxX", "minY", "maxY");
                case "CRotatableRect" -> moveRotRect(hit, origBounds, dx, dy);
                case "CRotatableBox" -> moveRotBox(hit, origBounds, dx, dy);
                case "C2DDot" -> moveDot(hit, origBounds, dx, dy);
                case "C2DLineSegment" -> moveLine(hit, origBounds, dx, dy);
                case "CSphere" -> moveCircle(hit, origBounds, dx, dy);
            }
        } catch (Exception e) {
            log.warn("Failed to move hit area: {}", e.getMessage());
        }
    }

    private void moveDefaultOrCBox(Spm.SPMHitArea hit, double[] orig, double dx, double dy, String xMinF, String xMaxF, String yMinF, String yMaxF) throws Exception {
        double nx1 = orig[0] + dx, nx2 = orig[2] + dx;
        double ny1 = orig[1] + dy, ny2 = orig[3] + dy;
        hit.getClass().getMethod("set" + capitalize(xMinF), Integer.class).invoke(hit, (int) Math.round(nx1));
        hit.getClass().getMethod("set" + capitalize(xMaxF), Integer.class).invoke(hit, (int) Math.round(nx2));
        hit.getClass().getMethod("set" + capitalize(yMinF), Integer.class).invoke(hit, (int) Math.round(ny1));
        hit.getClass().getMethod("set" + capitalize(yMaxF), Integer.class).invoke(hit, (int) Math.round(ny2));
    }

    private void moveRect(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        Object rect = hit.getClass().getMethod("getRect").invoke(hit);
        if (rect == null) return;
        Class<?> rectClass = rect.getClass();
        int left = (Integer) rectClass.getMethod("getLeft").invoke(rect);
        int top = (Integer) rectClass.getMethod("getTop").invoke(rect);
        int right = (Integer) rectClass.getMethod("getRight").invoke(rect);
        int bottom = (Integer) rectClass.getMethod("getBottom").invoke(rect);
        int w = Math.abs(right - left);
        int h = Math.abs(bottom - top);
        int nl = (int) Math.round(orig[0] + dx);
        int nt = (int) Math.round(orig[1] + dy);
        rectClass.getMethod("setLeft", Integer.class).invoke(rect, nl);
        rectClass.getMethod("setTop", Integer.class).invoke(rect, nt);
        rectClass.getMethod("setRight", Integer.class).invoke(rect, nl + w);
        rectClass.getMethod("setBottom", Integer.class).invoke(rect, nt + h);
    }

    private void moveCircle(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        hit.getClass().getMethod("setCenterX", Integer.class).invoke(hit, (int) Math.round(orig[0] + dx + (orig[2] - orig[0]) / 2));
        hit.getClass().getMethod("setCenterY", Integer.class).invoke(hit, (int) Math.round(orig[1] + dy + (orig[3] - orig[1]) / 2));
    }

    private void moveRotRect(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        hit.getClass().getMethod("setCenterX", Integer.class).invoke(hit, (int) Math.round(orig[0] + dx + (orig[2] - orig[0]) / 2));
        hit.getClass().getMethod("setCenterY", Integer.class).invoke(hit, (int) Math.round(orig[1] + dy + (orig[3] - orig[1]) / 2));
    }

    private void moveRotBox(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        hit.getClass().getMethod("setCenterX", Integer.class).invoke(hit, (int) Math.round(orig[0] + dx + (orig[2] - orig[0]) / 2));
        hit.getClass().getMethod("setCenterY", Integer.class).invoke(hit, (int) Math.round(orig[1] + dy + (orig[3] - orig[1]) / 2));
    }

    private void moveDot(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        hit.getClass().getMethod("setX", Integer.class).invoke(hit, (int) Math.round(orig[0] + dx + 4));
        hit.getClass().getMethod("setY", Integer.class).invoke(hit, (int) Math.round(orig[1] + dy + 4));
    }

    private void moveLine(Spm.SPMHitArea hit, double[] orig, double dx, double dy) throws Exception {
        double nx1 = orig[0] + dx, nx2 = orig[2] + dx;
        double ny1 = orig[1] + dy, ny2 = orig[3] + dy;
        hit.getClass().getMethod("setX1", Integer.class).invoke(hit, (int) Math.round(nx1));
        hit.getClass().getMethod("setY1", Integer.class).invoke(hit, (int) Math.round(ny1));
        hit.getClass().getMethod("setX2", Integer.class).invoke(hit, (int) Math.round(nx2));
        hit.getClass().getMethod("setY2", Integer.class).invoke(hit, (int) Math.round(ny2));
    }

    private double toPageX(double canvasX) {
        double originX = canvas.getWidth() / 2.0;
        Spm.SPMPageData page = getCurrentPage();
        if (page != null && canvasController.getCurrentSpm() != null) {
            Settings.OriginMode originMode = Settings.getOriginMode();
            if (originMode == Settings.OriginMode.TOP_LEFT) {
                var extents = canvasController.getCurrentPageExtents();
                if (extents != null) {
                    double contentW = Math.max(extents.getWidth(), 1.0);
                    double anchorX = (canvas.getWidth() - contentW) / 2.0;
                    originX = anchorX - extents.minX() + safe(page.getRotateCenterX());
                }
            }
        }
        return canvasX - originX;
    }

    private double toPageY(double canvasY) {
        double originY = canvas.getHeight() / 2.0;
        Spm.SPMPageData page = getCurrentPage();
        if (page != null && canvasController.getCurrentSpm() != null) {
            Settings.OriginMode originMode = Settings.getOriginMode();
            if (originMode == Settings.OriginMode.TOP_LEFT) {
                var extents = canvasController.getCurrentPageExtents();
                if (extents != null) {
                    double contentH = Math.max(extents.getHeight(), 1.0);
                    double anchorY = (canvas.getHeight() - contentH) / 2.0;
                    originY = anchorY - extents.minY() + safe(page.getRotateCenterY());
                }
            }
        }
        return canvasY - originY;
    }

    private double toPageDelta(double canvasDelta) { return canvasDelta; }

    private Spm.SPMPageData getCurrentPage() {
        Spm spm = canvasController.getCurrentSpm();
        if (spm == null) return null;
        int idx = canvasController.getCurrentPageIndex();
        if (idx < 0 || spm.getPageData() == null || idx >= spm.getPageData().size()) return null;
        return spm.getPageData().get(idx);
    }

    private static double safe(Integer v) { return v == null ? 0.0 : v.doubleValue(); }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
