package com.karaik.spmviewer.controller.editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaik.spmviewer.controller.CanvasController;
import com.karaik.spmviewer.controller.ImageRepository;
import com.karaik.spmviewer.controller.render.SpmRenderer;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.c.*;
import com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea;
import com.karaik.spmviewer.spm.parser.SpmBinaryWriter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * 简化版 Editor 骨架：加载 SPM + 图片，支持基本浏览与渲染。
 * 后续编辑能力将在此基础上迭代。
 */
@Slf4j
public class EditorViewController {

    @FXML private ListView<String> pageList;
    @FXML private ListView<String> chipList;
    @FXML private ListView<String> hitList;
    @FXML private ListView<String> animList;
    @FXML private ListView<String> imageList;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane canvasHolder;
    @FXML private Slider zoomSlider;
    @FXML private TextField zoomField;
    @FXML private CheckBox showCoordsCheck;
    @FXML private CheckBox showHitboxCheck;
    @FXML private CheckBox showChipBoundsCheck;
    @FXML private CheckBox showPageBoundsCheck;
    @FXML private Label statusLabel;
    @FXML private TextField pageWidthField;
    @FXML private TextField pageHeightField;
    @FXML private TextField rotateCenterXField;
    @FXML private TextField rotateCenterYField;
    @FXML private TextField pageLeftField;
    @FXML private TextField pageTopField;
    @FXML private TextField pageRightField;
    @FXML private TextField pageBottomField;
    @FXML private TextField chipImageNoField;
    @FXML private TextField dstXField;
    @FXML private TextField dstYField;
    @FXML private TextField dstWField;
    @FXML private TextField dstHField;
    @FXML private TextField srcXField;
    @FXML private TextField srcYField;
    @FXML private TextField srcWField;
    @FXML private TextField srcHField;
    @FXML private TextField drawOptionField;
    @FXML private TextField drawOptionValueField;
    @FXML private TextField chipOptionField;
    @FXML private Label hitTypeLabel;
    @FXML private TextField hitAField;
    @FXML private TextField hitBField;
    @FXML private TextField hitCField;
    @FXML private TextField hitDField;
    @FXML private ImageView chipPreview;

    private final ImageRepository imageRepository = new ImageRepository();
    private CanvasController canvasController;
    private SpmEntry spmEntry;
    private Path workingDirectory;
    private int selectedChipIndex = -1;
    private int selectedHitIndex = -1;
    private javafx.scene.canvas.Canvas overlayCanvas;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private DragTarget dragTarget = DragTarget.NONE;
    private double dragStartX;
    private double dragStartY;
    private double accumulatedDx;
    private double accumulatedDy;
    private Canvas dragCanvas;
    private SpmRenderer.PageContext dragContext;
    private boolean dirty = false;
    private String baseTitle = "Edit";
    private boolean stageSized = false;

    public void init(SpmEntry entry, Path workingDirectory) {
        this.spmEntry = entry;
        this.workingDirectory = workingDirectory;
        this.baseTitle = "Edit - " + entry.getPath().getFileName();
        loadSpm(entry);
        setDirty(false);
    }

    @FXML
    public void initialize() {
        setupCanvas();
        setupZoomBinding();
        setupDisplayFlags();
        setupSelections();
        setupScrollInteractions();
        setupShortcuts();
        setupStageSizing();
    }

    private void setupStageSizing() {
        canvasHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            newScene.windowProperty().addListener((o, oldWin, newWin) -> {
                if (newWin instanceof Stage stage && !stageSized) {
                    stage.setWidth(1080);
                    stage.setHeight(720);
                    stage.setMinWidth(900);
                    stage.setMinHeight(600);
                    stageSized = true;
                }
            });
        });
    }

    private void setupCanvas() {
        Scale scale = new Scale(1, 1, 0, 0);
        Group canvasGroup = new Group();
        javafx.scene.canvas.Canvas baseCanvas = new javafx.scene.canvas.Canvas();
        overlayCanvas = new javafx.scene.canvas.Canvas();
        overlayCanvas.setMouseTransparent(true);
        canvasController = new CanvasController(baseCanvas, canvasGroup, scale);
        canvasGroup.getChildren().add(overlayCanvas);
        canvasHolder.getChildren().add(canvasGroup);
        dragCanvas = baseCanvas;
        zoomSlider.valueProperty().addListener((obs, o, n) -> {
            scale.setX(n.doubleValue());
            scale.setY(n.doubleValue());
            overlayCanvas.setScaleX(n.doubleValue());
            overlayCanvas.setScaleY(n.doubleValue());
        });

        baseCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleCanvasPress);
        baseCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleCanvasDrag);
        baseCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleCanvasRelease);
    }

    private void setupZoomBinding() {
        NumberFormat zoomFormat = new DecimalFormat("0.0x");
        StringConverter<Number> zoomConverter = new NumberStringConverter(zoomFormat);
        zoomField.textProperty().bindBidirectional(zoomSlider.valueProperty(), zoomConverter);
        zoomField.setPromptText("zoom");
    }

    private void setupDisplayFlags() {
        showCoordsCheck.setSelected(true);
        showHitboxCheck.setSelected(true);
        showChipBoundsCheck.setSelected(false);
        showPageBoundsCheck.setSelected(false);

        showCoordsCheck.selectedProperty().addListener((obs, o, n) -> render());
        showHitboxCheck.selectedProperty().addListener((obs, o, n) -> render());
        showChipBoundsCheck.selectedProperty().addListener((obs, o, n) -> render());
        showPageBoundsCheck.selectedProperty().addListener((obs, o, n) -> render());
    }

    private void setupSelections() {
        pageList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            if (n == null || n.intValue() < 0) return;
            canvasController.setCurrentPageIndex(n.intValue());
            populateChipAndHitLists();
            render();
        });
        animList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            int idx = n == null ? -1 : n.intValue();
            if (idx >= 0 && spmEntry != null && spmEntry.getSpm() != null) {
                var anims = Optional.ofNullable(spmEntry.getSpm().getAnimData()).orElse(List.of());
                if (idx < anims.size()) {
                    var anim = anims.get(idx);
                    statusLabel.setText("Anim #" + idx + " frames: " + Optional.ofNullable(anim.getPatData()).orElse(List.of()).size());
                }
            }
        });
        chipList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            selectedChipIndex = n == null ? -1 : n.intValue();
            populateChipFields(selectedChipIndex < 0 ? null : spmEntry.getSpm()
                    .getPageData().get(canvasController.getCurrentPageIndex()).getChipData().get(selectedChipIndex));
            render();
        });
        chipList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                onEditSprite();
            }
        });
        hitList.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            selectedHitIndex = n == null ? -1 : n.intValue();
            populateHitFields(selectedHitIndex < 0 ? null : spmEntry.getSpm()
                    .getPageData().get(canvasController.getCurrentPageIndex()).getHitRects().get(selectedHitIndex));
            render();
        });
        imageList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) return;
            try {
                int idx = Integer.parseInt(n.split(":")[0].trim());
                canvasController.previewImage(idx, n, Settings.getBackgroundMode(), Settings.getBackgroundColor());
            } catch (Exception ex) {
                log.debug("parse image index failed: {}", n);
            }
        });
        imageList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                onEditImage();
            }
        });
    }

    private void setupScrollInteractions() {
        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                canvasHolder.requestFocus();
            }
        });
        canvasHolder.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DIGIT1 && e.isControlDown()) {
                zoomSlider.setValue(1.0);
                e.consume();
            }
        });
    }

    private void setupShortcuts() {
        canvasHolder.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.isControlDown()) {
                    if (e.getCode() == KeyCode.S) {
                        onSaveSpmBinary();
                        e.consume();
                    } else if (e.getCode() == KeyCode.Z) {
                        onUndo();
                        e.consume();
                    } else if (e.getCode() == KeyCode.Y) {
                        onRedo();
                        e.consume();
                    } else if (e.getCode() == KeyCode.E) {
                        onEditImage();
                        e.consume();
                    }
                } else if (e.getCode() == KeyCode.SPACE) {
                    scrollPane.requestFocus(); // 用于中键拖拽，Space 也聚焦以便滚轮缩放
                }
            });
        });
    }

    private void loadSpm(SpmEntry entry) {
        if (entry == null || entry.getSpm() == null) {
            statusLabel.setText("No SPM loaded");
            return;
        }
        Spm spm = entry.getSpm();
        statusLabel.setText("Editing: " + entry.getPath().getFileName() + " | Tips: select Page/Chip/Hit, drag on canvas; double-click an image to edit; Ctrl+E to open brush; Ctrl+S to save to tmp/");

        List<String> pages = new ArrayList<>();
        for (int i = 0; i < Optional.ofNullable(spm.getPageData()).orElse(List.of()).size(); i++) {
            pages.add("Page " + i);
        }
        pageList.setItems(FXCollections.observableArrayList(pages));

        List<String> anims = new ArrayList<>();
        List<Spm.SPMAnimData> animData = Optional.ofNullable(spm.getAnimData()).orElse(List.of());
        for (int i = 0; i < animData.size(); i++) {
            String name = animData.get(i).getAnimName();
            anims.add("Anim[" + i + "] " + (name == null ? "" : name));
        }
        animList.setItems(FXCollections.observableArrayList(anims));

        List<String> images = new ArrayList<>();
        List<Spm.SPMImageData> imgData = Optional.ofNullable(spm.getImageData()).orElse(List.of());
        for (int i = 0; i < imgData.size(); i++) {
            images.add(i + ": " + Optional.ofNullable(imgData.get(i).getImageName()).orElse(""));
        }
        imageList.setItems(FXCollections.observableArrayList(images));

        List<Image> loaded = imageRepository.loadImages(spm, workingDirectory);
        canvasController.setCurrentSpm(spm, loaded);

        if (!pages.isEmpty()) {
            pageList.getSelectionModel().select(0);
        } else {
            canvasController.clearCanvas(Settings.getBackgroundMode(), Settings.getBackgroundColor());
        }
        populateChipAndHitLists();
        populatePageFields();
    }

    private void render() {
        canvasController.renderPage(
                showCoordsCheck.isSelected(),
                showChipBoundsCheck.isSelected(),
                showHitboxCheck.isSelected(),
                showPageBoundsCheck.isSelected(),
                Settings.getBackgroundMode(),
                Settings.getBackgroundColor(),
                Settings.getOriginMode()
        );
        renderOverlay();
    }

    @FXML
    private void onZoom1x() {
        zoomSlider.setValue(1.0);
    }

    @FXML
    private void onFitView() {
        double viewW = scrollPane.getViewportBounds().getWidth();
        double viewH = scrollPane.getViewportBounds().getHeight();
        if (viewW <= 0 || viewH <= 0) {
            viewW = scrollPane.getWidth();
            viewH = scrollPane.getHeight();
        }
        double canvasW = canvasController.getCanvas().getWidth();
        double canvasH = canvasController.getCanvas().getHeight();
        if (canvasW <= 0 || canvasH <= 0 || viewW <= 0 || viewH <= 0) return;
        double scale = Math.min(viewW / canvasW, viewH / canvasH);
        scale = Math.max(zoomSlider.getMin(), Math.min(scale, zoomSlider.getMax()));
        zoomSlider.setValue(scale);
        scrollPane.requestLayout();
    }

    @FXML
    public void onUndo() {
        if (undoStack.isEmpty()) return;
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        render();
        setDirty(true);
    }

    @FXML
    public void onRedo() {
        if (redoStack.isEmpty()) return;
        Command cmd = redoStack.pop();
        cmd.redo();
        undoStack.push(cmd);
        render();
        setDirty(true);
    }

    @FXML
    private void onEditImage() {
        if (imageList.getSelectionModel().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Select an image to edit.").showAndWait();
            return;
        }
        int idx;
        try {
            idx = Integer.parseInt(imageList.getSelectionModel().getSelectedItem().split(":")[0].trim());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot parse image index.").showAndWait();
            return;
        }
        List<Image> images = canvasController.getLoadedImages();
        if (idx < 0 || idx >= images.size()) {
            new Alert(Alert.AlertType.ERROR, "Image index out of range.").showAndWait();
            return;
        }
        openImageEditor(idx, images.get(idx));
    }

    @FXML
    private void onSaveImage() {
        if (imageList.getSelectionModel().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Select an image to save.").showAndWait();
            return;
        }
        int idx;
        try {
            idx = Integer.parseInt(imageList.getSelectionModel().getSelectedItem().split(":")[0].trim());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot parse image index.").showAndWait();
            return;
        }
        List<Image> images = canvasController.getLoadedImages();
        if (idx < 0 || idx >= images.size()) {
            new Alert(Alert.AlertType.ERROR, "Image index out of range.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        chooser.setInitialFileName("image-" + idx + ".png");
        File defaultDir = ensureTmpDir();
        if (defaultDir != null && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File target = chooser.showSaveDialog(canvasHolder.getScene().getWindow());
        if (target == null) return;
        try {
            Image img = images.get(idx);
            saveImageToFile(img, target);
            setDirty(false); // image saved
            statusLabel.setText("Saved image to " + target.getName());
        } catch (Exception ex) {
            log.error("Save image failed", ex);
            new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onExportJson() {
        if (spmEntry == null || spmEntry.getSpm() == null) {
            new Alert(Alert.AlertType.ERROR, "No SPM loaded.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export SPM JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        chooser.setInitialFileName(spmEntry.getPath().getFileName().toString().replaceAll("\\.spm$", "") + "-edited.json");
        File defaultDir = ensureTmpDir();
        if (defaultDir != null && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File target = chooser.showSaveDialog(canvasHolder.getScene().getWindow());
        if (target == null) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, spmEntry.getSpm());
            statusLabel.setText("Exported JSON: " + target.getName());
        } catch (Exception ex) {
            log.error("Export JSON failed", ex);
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onSaveSpmBinary() {
        if (spmEntry == null || spmEntry.getSpm() == null) {
            new Alert(Alert.AlertType.ERROR, "No SPM loaded.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save SPM");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SPM", "*.spm"));
        chooser.setInitialFileName(spmEntry.getPath().getFileName().toString().replaceAll("\\.spm$", "") + "-edited.spm");
        File defaultDir = ensureTmpDir();
        if (defaultDir != null && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File target = chooser.showSaveDialog(canvasHolder.getScene().getWindow());
        if (target == null) return;
        try (var fos = new java.io.FileOutputStream(target)) {
            List<String> errors = validateSpm();
            if (!errors.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, String.join("\n", errors)).showAndWait();
                return;
            }
            SpmBinaryWriter.write(spmEntry.getSpm(), fos, Settings.getParsingMode());
            setDirty(false);
            statusLabel.setText("Saved SPM: " + target.getName());
        } catch (Exception ex) {
            log.error("Save SPM failed", ex);
            new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onApplyPage() {
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (invalidPage(pageIdx)) return;
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        int newW = parseIntOr(pageWidthField.getText(), page.getPageWidth());
        int newH = parseIntOr(pageHeightField.getText(), page.getPageHeight());
        int newCx = parseIntOr(rotateCenterXField.getText(), page.getRotateCenterX());
        int newCy = parseIntOr(rotateCenterYField.getText(), page.getRotateCenterY());
        Integer l = parseNullable(pageLeftField.getText());
        Integer t = parseNullable(pageTopField.getText());
        Integer r = parseNullable(pageRightField.getText());
        Integer b = parseNullable(pageBottomField.getText());

        Spm.SPMRect oldRect = page.getPageRect();
        Spm.SPMRect newRect = null;
        if (l != null && t != null && r != null && b != null) {
            newRect = new Spm.SPMRect();
            newRect.setLeft(l); newRect.setTop(t); newRect.setRight(r); newRect.setBottom(b);
        }
        Spm.SPMRect oldRectCopy = cloneRect(oldRect);
        final Spm.SPMRect newRectFinal = newRect;

        int oldW = safeInt(page.getPageWidth());
        int oldH = safeInt(page.getPageHeight());
        int oldCx = safeInt(page.getRotateCenterX());
        int oldCy = safeInt(page.getRotateCenterY());

        Command cmd = new Command(() -> {
            page.setPageWidth(newW);
            page.setPageHeight(newH);
            page.setRotateCenterX(newCx);
            page.setRotateCenterY(newCy);
            page.setPageRect(newRectFinal);
        }, () -> {
            page.setPageWidth(oldW);
            page.setPageHeight(oldH);
            page.setRotateCenterX(oldCx);
            page.setRotateCenterY(oldCy);
            page.setPageRect(oldRectCopy);
        });
        cmd.redo();
        undoStack.push(cmd);
        redoStack.clear();
        render();
        setDirty(true);
        setDirty(true);
    }

    @FXML
    private void onApplyChip() {
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (invalidPage(pageIdx)) return;
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        if (selectedChipIndex < 0 || page.getChipData() == null || selectedChipIndex >= page.getChipData().size()) {
            return;
        }
        var chip = page.getChipData().get(selectedChipIndex);
        // snapshot old
        ChipSnapshot oldSnap = ChipSnapshot.from(chip);

        Integer dstX = parseNullable(dstXField.getText());
        Integer dstY = parseNullable(dstYField.getText());
        Integer dstW = parseNullable(dstWField.getText());
        Integer dstH = parseNullable(dstHField.getText());
        Integer srcX = parseNullable(srcXField.getText());
        Integer srcY = parseNullable(srcYField.getText());
        Integer srcW = parseNullable(srcWField.getText());
        Integer srcH = parseNullable(srcHField.getText());
        int imgNo = parseIntOr(chipImageNoField.getText(), chip.getImageNo());
        int drawOpt = parseIntOr(drawOptionField.getText(), chip.getDrawOption() == null ? 0 : chip.getDrawOption().intValue());
        int drawOptVal = parseIntOr(drawOptionValueField.getText(), chip.getDrawOptionValue() == null ? 0 : chip.getDrawOptionValue().intValue());
        int option = parseIntOr(chipOptionField.getText(), chip.getOption());

        Command cmd = new Command(() -> {
            chip.setImageNo(imgNo);
            if (chip.getDstRect() == null) chip.setDstRect(new Spm.SPMRect());
            updateRect(chip.getDstRect(), dstX, dstY, dstW, dstH);
            if (chip.getSrcRect() == null) chip.setSrcRect(new Spm.SPMRect());
            updateRect(chip.getSrcRect(), srcX, srcY, srcW, srcH);
            chip.setDrawOption((long) drawOpt);
            chip.setDrawOptionValue((long) drawOptVal);
            chip.setOption(option);
        }, () -> oldSnap.restore(chip));
        cmd.redo();
        undoStack.push(cmd);
        redoStack.clear();
        render();
        setDirty(true);
        setDirty(true);
    }

    @FXML
    private void onApplyHit() {
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (invalidPage(pageIdx)) return;
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        if (selectedHitIndex < 0 || page.getHitRects() == null || selectedHitIndex >= page.getHitRects().size()) {
            return;
        }
        var hit = page.getHitRects().get(selectedHitIndex);
        HitSnapshot snapshot = HitSnapshot.from(hit);
        Integer a = parseNullable(hitAField.getText());
        Integer b = parseNullable(hitBField.getText());
        Integer c = parseNullable(hitCField.getText());
        Integer d = parseNullable(hitDField.getText());

        Command cmd = new Command(() -> applyHitFields(hit, a, b, c, d), () -> snapshot.restore(hit));
        cmd.redo();
        undoStack.push(cmd);
        redoStack.clear();
        render();
        setDirty(true);
        setDirty(true);
    }

    public void closeWindow() {
        Stage stage = (Stage) canvasHolder.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private void renderOverlay() {
        if (overlayCanvas == null) return;
        javafx.scene.canvas.Canvas base = canvasController.getCanvas();
        overlayCanvas.setWidth(base.getWidth());
        overlayCanvas.setHeight(base.getHeight());
        var g = overlayCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

        if (spmEntry == null || spmEntry.getSpm() == null) {
            return;
        }
        int pageIdx = canvasController.getCurrentPageIndex();
        if (pageIdx < 0) {
            return;
        }
        var spm = spmEntry.getSpm();
        if (spm.getPageData() == null || pageIdx >= spm.getPageData().size()) {
            return;
        }

        var renderer = canvasController.getRenderer();
        var page = spm.getPageData().get(pageIdx);
        var ext = canvasController.getCurrentPageExtents();
        var ctx = renderer.computePageContext(page, Settings.getOriginMode(), ext,
                overlayCanvas.getWidth(), overlayCanvas.getHeight());

        if (showChipBoundsCheck.isSelected() && selectedChipIndex >= 0 && page.getChipData() != null && selectedChipIndex < page.getChipData().size()) {
            var chip = page.getChipData().get(selectedChipIndex);
            var rect = chip.getDstRect();
            if (rect != null) {
                double x = ctx.translateX() + safe(rect.getLeft());
                double y = ctx.translateY() + safe(rect.getTop());
                double w = safe(rect.getRight()) - safe(rect.getLeft());
                double h = safe(rect.getBottom()) - safe(rect.getTop());
                g.setStroke(Color.CYAN);
                g.setLineWidth(2.0);
                g.strokeRect(x, y, w, h);
            }
        }

        if (showHitboxCheck.isSelected() && selectedHitIndex >= 0 && page.getHitRects() != null && selectedHitIndex < page.getHitRects().size()) {
            var hit = page.getHitRects().get(selectedHitIndex);
            g.save();
            g.setGlobalAlpha(0.9);
            hit.drawSelf(g, ctx.pageOriginX(), ctx.pageOriginY());
            g.restore();
        }

        updateChipPreview();
    }

    private void populateChipAndHitLists() {
        selectedChipIndex = -1;
        selectedHitIndex = -1;
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (spmEntry == null || spmEntry.getSpm() == null || pageIdx < 0) {
            chipList.setItems(FXCollections.emptyObservableList());
            hitList.setItems(FXCollections.emptyObservableList());
            return;
        }
        var spm = spmEntry.getSpm();
        if (spm.getPageData() == null || pageIdx >= spm.getPageData().size()) {
            chipList.setItems(FXCollections.emptyObservableList());
            hitList.setItems(FXCollections.emptyObservableList());
            return;
        }
        var page = spm.getPageData().get(pageIdx);

        List<String> chips = new ArrayList<>();
        List<Spm.SPMChipData> chipData = Optional.ofNullable(page.getChipData()).orElse(List.of());
        for (int i = 0; i < chipData.size(); i++) {
            chips.add("Chip[" + i + "] img=" + chipData.get(i).getImageNo());
        }
        chipList.setItems(FXCollections.observableArrayList(chips));

        List<String> hits = new ArrayList<>();
        List<Spm.SPMHitArea> hitData = Optional.ofNullable(page.getHitRects()).orElse(List.of());
        for (int i = 0; i < hitData.size(); i++) {
            hits.add("Hit[" + i + "] type=" + hitData.get(i).getShapeType());
        }
        hitList.setItems(FXCollections.observableArrayList(hits));
        populatePageFields();
        populateChipFields(null);
        populateHitFields(null);
    }

    private static double safe(Integer v) {
        return v == null ? 0.0 : v.doubleValue();
    }

    private void handleCanvasPress(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;
        dragStartX = e.getX();
        dragStartY = e.getY();
        accumulatedDx = 0;
        accumulatedDy = 0;
        dragTarget = DragTarget.NONE;

        int pageIdx = canvasController.getCurrentPageIndex();
        if (pageIdx < 0 || spmEntry == null || spmEntry.getSpm() == null) {
            return;
        }
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        dragContext = canvasController.getRenderer().computePageContext(
                page, Settings.getOriginMode(), canvasController.getCurrentPageExtents(),
                dragCanvas.getWidth(), dragCanvas.getHeight()
        );
        if (selectedChipIndex >= 0) {
            dragTarget = DragTarget.CHIP;
        } else if (selectedHitIndex >= 0) {
            dragTarget = DragTarget.HIT;
        }
    }

    private void handleCanvasDrag(MouseEvent e) {
        if (dragTarget == DragTarget.NONE) return;
        double dx = e.getX() - dragStartX;
        double dy = e.getY() - dragStartY;
        double deltaX = dx - accumulatedDx;
        double deltaY = dy - accumulatedDy;
        accumulatedDx = dx;
        accumulatedDy = dy;

        int pageIdx = canvasController.getCurrentPageIndex();
        if (pageIdx < 0 || spmEntry == null || spmEntry.getSpm() == null) return;
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        if (dragTarget == DragTarget.CHIP && selectedChipIndex >= 0 && page.getChipData() != null &&
                selectedChipIndex < page.getChipData().size()) {
            moveChipBy(page.getChipData().get(selectedChipIndex), (int) Math.round(deltaX), (int) Math.round(deltaY));
        } else if (dragTarget == DragTarget.HIT && selectedHitIndex >= 0 && page.getHitRects() != null &&
                selectedHitIndex < page.getHitRects().size()) {
            moveHitBy(page.getHitRects().get(selectedHitIndex), (int) Math.round(deltaX), (int) Math.round(deltaY));
        }
        render();
    }

    private void handleCanvasRelease(MouseEvent e) {
        if (dragTarget == DragTarget.NONE) return;
        if (Math.abs(accumulatedDx) < 1 && Math.abs(accumulatedDy) < 1) {
            dragTarget = DragTarget.NONE;
            return;
        }
        int totalDx = (int) Math.round(accumulatedDx);
        int totalDy = (int) Math.round(accumulatedDy);

        int pageIdx = canvasController.getCurrentPageIndex();
        if (pageIdx < 0 || spmEntry == null || spmEntry.getSpm() == null) {
            dragTarget = DragTarget.NONE;
            return;
        }
        var page = spmEntry.getSpm().getPageData().get(pageIdx);

        if (dragTarget == DragTarget.CHIP && selectedChipIndex >= 0 && page.getChipData() != null &&
                selectedChipIndex < page.getChipData().size()) {
            var chip = page.getChipData().get(selectedChipIndex);
            Command cmd = new Command(() -> moveChipBy(chip, totalDx, totalDy),
                    () -> moveChipBy(chip, -totalDx, -totalDy));
            undoStack.push(cmd);
            redoStack.clear();
            setDirty(true);
        } else if (dragTarget == DragTarget.HIT && selectedHitIndex >= 0 && page.getHitRects() != null &&
                selectedHitIndex < page.getHitRects().size()) {
            var hit = page.getHitRects().get(selectedHitIndex);
            Command cmd = new Command(() -> moveHitBy(hit, totalDx, totalDy),
                    () -> moveHitBy(hit, -totalDx, -totalDy));
            undoStack.push(cmd);
            redoStack.clear();
            setDirty(true);
        }
        dragTarget = DragTarget.NONE;
    }

    public void moveChipBy(Spm.SPMChipData chip, int dx, int dy) {
        if (chip == null) return;
        Spm.SPMRect dst = chip.getDstRect();
        if (dst == null) return;
        dst.setLeft((int) Math.round(safe(dst.getLeft()) + dx));
        dst.setRight((int) Math.round(safe(dst.getRight()) + dx));
        dst.setTop((int) Math.round(safe(dst.getTop()) + dy));
        dst.setBottom((int) Math.round(safe(dst.getBottom()) + dy));
    }

    public void moveHitBy(Spm.SPMHitArea hit, int dx, int dy) {
        if (hit == null) return;
        if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r) {
            Spm.SPMRect rect = r.getRect();
            if (rect != null) {
                rect.setLeft((int) Math.round(safe(rect.getLeft()) + dx));
                rect.setRight((int) Math.round(safe(rect.getRight()) + dx));
                rect.setTop((int) Math.round(safe(rect.getTop()) + dy));
                rect.setBottom((int) Math.round(safe(rect.getBottom()) + dy));
            }
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea h) {
            h.setXMin((int) Math.round(safe(h.getXMin()) + dx));
            h.setXMax((int) Math.round(safe(h.getXMax()) + dx));
            h.setYMin((int) Math.round(safe(h.getYMin()) + dy));
            h.setYMax((int) Math.round(safe(h.getYMax()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect r) {
            r.setCenterX((int) Math.round(safe(r.getCenterX()) + dx));
            r.setCenterY((int) Math.round(safe(r.getCenterY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle c) {
            c.setCenterX((int) Math.round(safe(c.getCenterX()) + dx));
            c.setCenterY((int) Math.round(safe(c.getCenterY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DLineSegment l) {
            l.setX1((int) Math.round(safe(l.getX1()) + dx));
            l.setX2((int) Math.round(safe(l.getX2()) + dx));
            l.setY1((int) Math.round(safe(l.getY1()) + dy));
            l.setY2((int) Math.round(safe(l.getY2()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DDot d) {
            d.setX((int) Math.round(safe(d.getX()) + dx));
            d.setY((int) Math.round(safe(d.getY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CBox b) {
            b.setMinX((int) Math.round(safe(b.getMinX()) + dx));
            b.setMaxX((int) Math.round(safe(b.getMaxX()) + dx));
            b.setMinY((int) Math.round(safe(b.getMinY()) + dy));
            b.setMaxY((int) Math.round(safe(b.getMaxY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableBox rb) {
            rb.setCenterX((int) Math.round(safe(rb.getCenterX()) + dx));
            rb.setCenterY((int) Math.round(safe(rb.getCenterY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CSphere s) {
            s.setCenterX((int) Math.round(safe(s.getCenterX()) + dx));
            s.setCenterY((int) Math.round(safe(s.getCenterY()) + dy));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
            Spm.SPMRect rect = lr.getHitRect();
            if (rect != null) {
                rect.setLeft((int) Math.round(safe(rect.getLeft()) + dx));
                rect.setRight((int) Math.round(safe(rect.getRight()) + dx));
                rect.setTop((int) Math.round(safe(rect.getTop()) + dy));
                rect.setBottom((int) Math.round(safe(rect.getBottom()) + dy));
            }
        } else if (hit instanceof LegacyRectHitArea lr2) {
            Spm.SPMRect rect = lr2.getHitRect();
            if (rect != null) {
                rect.setLeft((int) Math.round(safe(rect.getLeft()) + dx));
                rect.setRight((int) Math.round(safe(rect.getRight()) + dx));
                rect.setTop((int) Math.round(safe(rect.getTop()) + dy));
                rect.setBottom((int) Math.round(safe(rect.getBottom()) + dy));
            }
        }
    }

    private void openImageEditor(int imageIndex, Image image) {
        if (image == null) {
            new Alert(Alert.AlertType.ERROR, "图片为空").showAndWait();
            return;
        }
        WritableImage working = toWritable(image);
        Canvas imgCanvas = new Canvas(working.getWidth(), working.getHeight());
        GraphicsContext g = imgCanvas.getGraphicsContext2D();
        g.drawImage(working, 0, 0);

        Deque<WritableImage> history = new ArrayDeque<>();
        history.push(copyImage(working));

        ColorPicker colorPicker = new ColorPicker(Color.RED);
        Slider brushSize = new Slider(1, 32, 6);
        Button undoBtn = new Button("Undo Draw");
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        Label hint = new Label("Drag to paint. Undo saves snapshots per press.");

        imgCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            history.push(copyImage(snapshotCanvas(imgCanvas)));
        });

        imgCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double size = brushSize.getValue();
            g.setFill(colorPicker.getValue());
            g.fillOval(e.getX() - size / 2.0, e.getY() - size / 2.0, size, size);
        });

        undoBtn.setOnAction(ev -> {
            if (history.size() <= 1) return;
            history.pop(); // discard current
            WritableImage prev = history.peek();
            if (prev != null) {
                g.clearRect(0, 0, imgCanvas.getWidth(), imgCanvas.getHeight());
                g.drawImage(prev, 0, 0);
            }
        });

        saveBtn.setOnAction(ev -> {
            WritableImage newImg = snapshotCanvas(imgCanvas);
            canvasController.getLoadedImages().set(imageIndex, newImg);
            render();
            ((Stage) saveBtn.getScene().getWindow()).close();
            setDirty(true);
        });
        cancelBtn.setOnAction(ev -> ((Stage) cancelBtn.getScene().getWindow()).close());

        HBox tools = new HBox(8, new Label("Color"), colorPicker, new Label("Brush"), brushSize, undoBtn, saveBtn, cancelBtn);
        tools.getChildren().add(0, hint);
        BorderPane root = new BorderPane(imgCanvas);
        root.setBottom(tools);
        Stage stage = new Stage();
        stage.initOwner(canvasHolder.getScene().getWindow());
        stage.initModality(Modality.NONE);
        stage.setTitle("Image Edit #" + imageIndex);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    private void onEditSprite() {
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (invalidPage(pageIdx)) {
            new Alert(Alert.AlertType.INFORMATION, "Select a page/chip first.").showAndWait();
            return;
        }
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        if (selectedChipIndex < 0 || page.getChipData() == null || selectedChipIndex >= page.getChipData().size()) {
            new Alert(Alert.AlertType.INFORMATION, "Select a chip to replace its sprite.").showAndWait();
            return;
        }
        var chip = page.getChipData().get(selectedChipIndex);
        if (chip.getImageNo() == null || chip.getSrcRect() == null) {
            new Alert(Alert.AlertType.ERROR, "Chip has no image/srcRect.").showAndWait();
            return;
        }
        int imgIdx = chip.getImageNo();
        List<Image> images = canvasController.getLoadedImages();
        if (imgIdx < 0 || imgIdx >= images.size()) {
            new Alert(Alert.AlertType.ERROR, "Image index out of range: " + imgIdx).showAndWait();
            return;
        }
        Spm.SPMRect src = chip.getSrcRect();
        int sx = (int) Math.round(safe(src.getLeft()));
        int sy = (int) Math.round(safe(src.getTop()));
        int sw = (int) Math.round(safe(src.getRight()) - safe(src.getLeft()));
        int sh = (int) Math.round(safe(src.getBottom()) - safe(src.getTop()));
        if (sw <= 0 || sh <= 0) {
            new Alert(Alert.AlertType.ERROR, "Invalid src rect size.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        Image base = images.get(imgIdx);
        if (sx < 0 || sy < 0 || sx + sw > base.getWidth() || sy + sh > base.getHeight()) {
            new Alert(Alert.AlertType.ERROR, "Src rect out of image bounds.").showAndWait();
            return;
        }
        PixelReader reader = base.getPixelReader();
        if (reader == null) {
            new Alert(Alert.AlertType.ERROR, "Image unreadable.").showAndWait();
            return;
        }
        WritableImage sprite = new WritableImage(reader, sx, sy, sw, sh);
        openSpriteEditor(imgIdx, sprite, sx, sy);
    }

    @FXML
    private void onImportSprite() {
        int pageIdx = pageList.getSelectionModel().getSelectedIndex();
        if (invalidPage(pageIdx)) {
            new Alert(Alert.AlertType.INFORMATION, "Select a page/chip first.").showAndWait();
            return;
        }
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        if (selectedChipIndex < 0 || page.getChipData() == null || selectedChipIndex >= page.getChipData().size()) {
            new Alert(Alert.AlertType.INFORMATION, "Select a chip to replace its sprite.").showAndWait();
            return;
        }
        var chip = page.getChipData().get(selectedChipIndex);
        if (chip.getImageNo() == null || chip.getSrcRect() == null) {
            new Alert(Alert.AlertType.ERROR, "Chip has no image/srcRect.").showAndWait();
            return;
        }
        int imgIdx = chip.getImageNo();
        List<Image> images = canvasController.getLoadedImages();
        if (imgIdx < 0 || imgIdx >= images.size()) {
            new Alert(Alert.AlertType.ERROR, "Image index out of range: " + imgIdx).showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Sprite (PNG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File defaultDir = ensureTmpDir();
        if (defaultDir != null && defaultDir.isDirectory()) {
            chooser.setInitialDirectory(defaultDir);
        }
        File png = chooser.showOpenDialog(canvasHolder.getScene().getWindow());
        if (png == null) return;

        Image imported;
        try {
            imported = new Image(png.toURI().toString());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load image: " + ex.getMessage()).showAndWait();
            return;
        }
        Image base = images.get(imgIdx);
        Spm.SPMRect src = chip.getSrcRect();
        int sx = (int) Math.round(safe(src.getLeft()));
        int sy = (int) Math.round(safe(src.getTop()));
        int sw = (int) Math.round(safe(src.getRight()) - safe(src.getLeft()));
        int sh = (int) Math.round(safe(src.getBottom()) - safe(src.getTop()));
        int newW = (int) Math.round(imported.getWidth());
        int newH = (int) Math.round(imported.getHeight());
        if (sx + newW > base.getWidth() || sy + newH > base.getHeight()) {
            new Alert(Alert.AlertType.ERROR, "Imported image exceeds source bounds.").showAndWait();
            return;
        }
        WritableImage importedWritable = toWritable(imported);

        // snapshot old rects for undo
        Spm.SPMRect oldSrc = cloneRect(chip.getSrcRect());
        Spm.SPMRect oldDst = cloneRect(chip.getDstRect());
        WritableImage beforePatch = snapshotRegion(base, sx, sy, newW, newH);

        Command cmd = new Command(() -> {
            patchSpriteIntoImage(imgIdx, importedWritable, sx, sy);
            if (chip.getSrcRect() == null) chip.setSrcRect(new Spm.SPMRect());
            chip.getSrcRect().setLeft(sx);
            chip.getSrcRect().setTop(sy);
            chip.getSrcRect().setRight(sx + newW);
            chip.getSrcRect().setBottom(sy + newH);

            if (chip.getDstRect() == null) chip.setDstRect(new Spm.SPMRect());
            chip.getDstRect().setRight(chip.getDstRect().getLeft() == null ? newW : chip.getDstRect().getLeft() + newW);
            chip.getDstRect().setBottom(chip.getDstRect().getTop() == null ? newH : chip.getDstRect().getTop() + newH);

            chip.setChipWidth(newW);
            chip.setChipHeight(newH);
            render();
            setDirty(true);
        }, () -> {
            if (beforePatch != null) {
                patchSpriteIntoImage(imgIdx, beforePatch, sx, sy);
            }
            if (chip.getSrcRect() != null && oldSrc != null) {
                chip.setSrcRect(cloneRect(oldSrc));
            }
            if (chip.getDstRect() != null && oldDst != null) {
                chip.setDstRect(cloneRect(oldDst));
            }
            render();
            setDirty(true);
        });
        cmd.redo();
        undoStack.push(cmd);
        redoStack.clear();
        statusLabel.setText("Sprite imported: " + png.getName());
    }

    private void openSpriteEditor(int imageIndex, WritableImage sprite, int srcX, int srcY) {
        Canvas spriteCanvas = new Canvas(sprite.getWidth(), sprite.getHeight());
        GraphicsContext g = spriteCanvas.getGraphicsContext2D();
        g.drawImage(sprite, 0, 0);

        Deque<WritableImage> history = new ArrayDeque<>();
        history.push(copyImage(sprite));

        ColorPicker colorPicker = new ColorPicker(Color.RED);
        Slider brushSize = new Slider(1, 32, 6);
        Button undoBtn = new Button("Undo");
        Button saveBtn = new Button("Apply to image");
        Button cancelBtn = new Button("Cancel");
        Label hint = new Label("Editing sprite only. Changes will patch the source image region.");

        spriteCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> history.push(copyImage(snapshotCanvas(spriteCanvas))));
        spriteCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double size = brushSize.getValue();
            g.setFill(colorPicker.getValue());
            g.fillOval(e.getX() - size / 2.0, e.getY() - size / 2.0, size, size);
        });

        undoBtn.setOnAction(ev -> {
            if (history.size() <= 1) return;
            history.pop();
            WritableImage prev = history.peek();
            if (prev != null) {
                g.clearRect(0, 0, spriteCanvas.getWidth(), spriteCanvas.getHeight());
                g.drawImage(prev, 0, 0);
            }
        });

        saveBtn.setOnAction(ev -> {
            WritableImage edited = snapshotCanvas(spriteCanvas);
            patchSpriteIntoImage(imageIndex, edited, srcX, srcY);
            render();
            ((Stage) saveBtn.getScene().getWindow()).close();
            setDirty(true);
        });
        cancelBtn.setOnAction(ev -> ((Stage) cancelBtn.getScene().getWindow()).close());

        HBox tools = new HBox(8, hint, new Label("Color"), colorPicker, new Label("Brush"), brushSize, undoBtn, saveBtn, cancelBtn);
        BorderPane root = new BorderPane(spriteCanvas);
        root.setBottom(tools);
        Stage stage = new Stage();
        stage.initOwner(canvasHolder.getScene().getWindow());
        stage.initModality(Modality.NONE);
        stage.setTitle("Sprite Edit (image #" + imageIndex + ")");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private WritableImage toWritable(Image img) {
        if (img instanceof WritableImage wi) {
            return wi;
        }
        WritableImage w = new WritableImage((int) img.getWidth(), (int) img.getHeight());
        PixelReader pr = img.getPixelReader();
        w.getPixelWriter().setPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), pr, 0, 0);
        return w;
    }

    private WritableImage copyImage(WritableImage src) {
        WritableImage copy = new WritableImage((int) src.getWidth(), (int) src.getHeight());
        copy.getPixelWriter().setPixels(0, 0, (int) src.getWidth(), (int) src.getHeight(), src.getPixelReader(), 0, 0);
        return copy;
    }

    private WritableImage snapshotCanvas(Canvas canvas) {
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, snapshot);
        return snapshot;
    }

    private void patchSpriteIntoImage(int imageIndex, WritableImage sprite, int srcX, int srcY) {
        List<Image> images = canvasController.getLoadedImages();
        if (imageIndex < 0 || imageIndex >= images.size()) {
            return;
        }
        Image base = images.get(imageIndex);
        WritableImage writableBase = ensureWritable(base);
        writableBase.getPixelWriter().setPixels(srcX, srcY, (int) sprite.getWidth(), (int) sprite.getHeight(),
                sprite.getPixelReader(), 0, 0);
        images.set(imageIndex, writableBase);
    }

    private WritableImage ensureWritable(Image img) {
        if (img instanceof WritableImage wi) {
            return wi;
        }
        WritableImage w = new WritableImage((int) img.getWidth(), (int) img.getHeight());
        PixelReader pr = img.getPixelReader();
        w.getPixelWriter().setPixels(0, 0, (int) img.getWidth(), (int) img.getHeight(), pr, 0, 0);
        return w;
    }

    private WritableImage snapshotRegion(Image base, int x, int y, int w, int h) {
        if (base == null || base.getPixelReader() == null) return null;
        int bw = (int) base.getWidth();
        int bh = (int) base.getHeight();
        int rw = Math.min(w, bw - x);
        int rh = Math.min(h, bh - y);
        if (rw <= 0 || rh <= 0) return null;
        return new WritableImage(base.getPixelReader(), x, y, rw, rh);
    }

    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        Stage stage = canvasHolder.getScene() == null ? null : (Stage) canvasHolder.getScene().getWindow();
        if (stage != null) {
            stage.setTitle(baseTitle + (dirty ? " *" : ""));
            statusLabel.setText((dirty ? "Unsaved changes. " : "") + "Tips: select Page/Chip/Hit, drag to move; Ctrl+E edit image; Ctrl+S save to tmp/");
        }
    }

    private List<String> validateSpm() {
        List<String> errors = new ArrayList<>();
        if (spmEntry == null || spmEntry.getSpm() == null) {
            errors.add("No SPM data.");
            return errors;
        }
        List<Image> imgs = canvasController.getLoadedImages();
        var spm = spmEntry.getSpm();
        var pages = Optional.ofNullable(spm.getPageData()).orElse(List.of());
        for (int pi = 0; pi < pages.size(); pi++) {
            var page = pages.get(pi);
            List<Spm.SPMChipData> chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
            for (int ci = 0; ci < chips.size(); ci++) {
                var c = chips.get(ci);
                int imgNo = c.getImageNo() == null ? -1 : c.getImageNo();
                if (imgNo < 0 || imgNo >= imgs.size()) {
                    errors.add("Page " + pi + " Chip " + ci + " imageNo out of range: " + imgNo);
                } else if (c.getSrcRect() != null) {
                    Image img = imgs.get(imgNo);
                    if (img != null) {
                        double sw = safe(c.getSrcRect().getRight()) - safe(c.getSrcRect().getLeft());
                        double sh = safe(c.getSrcRect().getBottom()) - safe(c.getSrcRect().getTop());
                        if (sw < 0 || sh < 0) {
                            errors.add("Page " + pi + " Chip " + ci + " srcRect has negative size.");
                        }
                        if (safe(c.getSrcRect().getLeft()) < 0 || safe(c.getSrcRect().getTop()) < 0
                                || safe(c.getSrcRect().getRight()) > img.getWidth()
                                || safe(c.getSrcRect().getBottom()) > img.getHeight()) {
                            errors.add("Page " + pi + " Chip " + ci + " srcRect exceeds image bounds.");
                        }
                    }
                }
            }
        }
        return errors;
    }

    private void saveImageToFile(Image img, File target) throws Exception {
        if (img == null) {
            throw new IllegalArgumentException("image is null");
        }
        WritableImage wi = toWritable(img);
        int width = (int) wi.getWidth();
        int height = (int) wi.getHeight();
        PixelReader reader = wi.getPixelReader();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] buffer = new int[width];
        for (int y = 0; y < height; y++) {
            reader.getPixels(0, y, width, 1, javafx.scene.image.PixelFormat.getIntArgbPreInstance(), buffer, 0, width);
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, buffer[x]);
            }
        }
        ImageIO.write(buffered, "png", target);
    }

    private void updateChipPreview() {
        if (chipPreview == null) return;
        chipPreview.setImage(null);
        if (spmEntry == null || spmEntry.getSpm() == null) return;
        int pageIdx = canvasController.getCurrentPageIndex();
        if (pageIdx < 0) return;
        var spm = spmEntry.getSpm();
        if (spm.getPageData() == null || pageIdx >= spm.getPageData().size()) return;
        var page = spm.getPageData().get(pageIdx);
        if (selectedChipIndex < 0 || page.getChipData() == null || selectedChipIndex >= page.getChipData().size()) return;
        var chip = page.getChipData().get(selectedChipIndex);
        if (chip.getImageNo() == null) return;
        List<Image> images = canvasController.getLoadedImages();
        int imgIdx = chip.getImageNo();
        if (imgIdx < 0 || imgIdx >= images.size()) return;
        Image img = images.get(imgIdx);
        if (img == null) return;
        Spm.SPMRect src = chip.getSrcRect();
        if (src == null) return;
        int sx = (int) Math.round(safe(src.getLeft()));
        int sy = (int) Math.round(safe(src.getTop()));
        int sw = (int) Math.round(safe(src.getRight()) - safe(src.getLeft()));
        int sh = (int) Math.round(safe(src.getBottom()) - safe(src.getTop()));
        if (sw <= 0 || sh <= 0) return;
        if (sx + sw > img.getWidth() || sy + sh > img.getHeight()) return;
        PixelReader reader = img.getPixelReader();
        if (reader == null) return;
        WritableImage slice = new WritableImage(reader, sx, sy, sw, sh);
        chipPreview.setImage(slice);
    }

    public static class Command {
        private final Runnable redo;
        private final Runnable undo;
        public Command(Runnable redo, Runnable undo) {
            this.redo = redo;
            this.undo = undo;
        }
        public void redo() { redo.run(); }
        public void undo() { undo.run(); }
    }

    private enum DragTarget { NONE, CHIP, HIT }

    // ---------- form helpers ----------
    private void populatePageFields() {
        int pageIdx = canvasController.getCurrentPageIndex();
        if (invalidPage(pageIdx)) {
            pageWidthField.setText("");
            pageHeightField.setText("");
            rotateCenterXField.setText("");
            rotateCenterYField.setText("");
            pageLeftField.setText("");
            pageTopField.setText("");
            pageRightField.setText("");
            pageBottomField.setText("");
            return;
        }
        var page = spmEntry.getSpm().getPageData().get(pageIdx);
        pageWidthField.setText(String.valueOf(safeInt(page.getPageWidth())));
        pageHeightField.setText(String.valueOf(safeInt(page.getPageHeight())));
        rotateCenterXField.setText(String.valueOf(safeInt(page.getRotateCenterX())));
        rotateCenterYField.setText(String.valueOf(safeInt(page.getRotateCenterY())));
        if (page.getPageRect() != null) {
            pageLeftField.setText(String.valueOf(safeInt(page.getPageRect().getLeft())));
            pageTopField.setText(String.valueOf(safeInt(page.getPageRect().getTop())));
            pageRightField.setText(String.valueOf(safeInt(page.getPageRect().getRight())));
            pageBottomField.setText(String.valueOf(safeInt(page.getPageRect().getBottom())));
        } else {
            pageLeftField.setText("");
            pageTopField.setText("");
            pageRightField.setText("");
            pageBottomField.setText("");
        }
    }

    private void populateChipFields(Spm.SPMChipData chip) {
        if (chip == null) {
            chipImageNoField.setText("");
            dstXField.setText("");
            dstYField.setText("");
            dstWField.setText("");
            dstHField.setText("");
            srcXField.setText("");
            srcYField.setText("");
            srcWField.setText("");
            srcHField.setText("");
            drawOptionField.setText("");
            drawOptionValueField.setText("");
            chipOptionField.setText("");
            return;
        }
        chipImageNoField.setText(String.valueOf(safeInt(chip.getImageNo())));
        Rect dst = Rect.from(chip.getDstRect());
        Rect src = Rect.from(chip.getSrcRect());
        dstXField.setText(String.valueOf(dst.x));
        dstYField.setText(String.valueOf(dst.y));
        dstWField.setText(String.valueOf(dst.w));
        dstHField.setText(String.valueOf(dst.h));
        srcXField.setText(String.valueOf(src.x));
        srcYField.setText(String.valueOf(src.y));
        srcWField.setText(String.valueOf(src.w));
        srcHField.setText(String.valueOf(src.h));
        drawOptionField.setText(String.valueOf(chip.getDrawOption() == null ? 0 : chip.getDrawOption()));
        drawOptionValueField.setText(String.valueOf(chip.getDrawOptionValue() == null ? 0 : chip.getDrawOptionValue()));
        chipOptionField.setText(String.valueOf(safeInt(chip.getOption())));
    }

    private void populateHitFields(Spm.SPMHitArea hit) {
        if (hit == null) {
            hitTypeLabel.setText("-");
            hitAField.setText("");
            hitBField.setText("");
            hitCField.setText("");
            hitDField.setText("");
            return;
        }
        hitTypeLabel.setText(String.valueOf(hit.getShapeType()));
        if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r && r.getRect() != null) {
            hitAField.setText(String.valueOf(safeInt(r.getRect().getLeft())));
            hitBField.setText(String.valueOf(safeInt(r.getRect().getTop())));
            hitCField.setText(String.valueOf(safeInt(r.getRect().getRight())));
            hitDField.setText(String.valueOf(safeInt(r.getRect().getBottom())));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea d) {
            hitAField.setText(String.valueOf(safeInt(d.getXMin())));
            hitBField.setText(String.valueOf(safeInt(d.getYMin())));
            hitCField.setText(String.valueOf(safeInt(d.getXMax())));
            hitDField.setText(String.valueOf(safeInt(d.getYMax())));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect rr) {
            hitAField.setText(String.valueOf(safeInt(rr.getCenterX())));
            hitBField.setText(String.valueOf(safeInt(rr.getCenterY())));
            hitCField.setText(String.valueOf(safeInt(rr.getWidth())));
            hitDField.setText(String.valueOf(safeInt(rr.getHeight())));
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle c) {
            hitAField.setText(String.valueOf(safeInt(c.getCenterX())));
            hitBField.setText(String.valueOf(safeInt(c.getCenterY())));
            hitCField.setText(String.valueOf(safeInt(c.getRadius())));
            hitDField.setText("");
        } else {
            hitAField.setText("");
            hitBField.setText("");
            hitCField.setText("");
            hitDField.setText("");
        }
    }

    private void applyHitFields(Spm.SPMHitArea hit, Integer a, Integer b, Integer c, Integer d) {
        if (hit == null) return;
        if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r) {
            if (r.getRect() == null) r.setRect(new Spm.SPMRect());
            applyRect(r.getRect(), a, b, c, d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea dh) {
            if (a != null) dh.setXMin(a);
            if (b != null) dh.setYMin(b);
            if (c != null) dh.setXMax(c);
            if (d != null) dh.setYMax(d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect rr) {
            if (a != null) rr.setCenterX(a);
            if (b != null) rr.setCenterY(b);
            if (c != null) rr.setWidth(c);
            if (d != null) rr.setHeight(d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle cc) {
            if (a != null) cc.setCenterX(a);
            if (b != null) cc.setCenterY(b);
            if (c != null) cc.setRadius(c);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DLineSegment ls) {
            if (a != null) ls.setX1(a);
            if (b != null) ls.setY1(b);
            if (c != null) ls.setX2(c);
            if (d != null) ls.setY2(d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.C2DDot dot) {
            if (a != null) dot.setX(a);
            if (b != null) dot.setY(b);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CBox box) {
            if (a != null) box.setMinX(a);
            if (b != null) box.setMinY(b);
            if (c != null) box.setMaxX(c);
            if (d != null) box.setMaxY(d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableBox rb) {
            if (a != null) rb.setCenterX(a);
            if (b != null) rb.setCenterY(b);
            if (c != null) rb.setSizeX(c);
            if (d != null) rb.setSizeY(d);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CSphere s) {
            if (a != null) s.setCenterX(a);
            if (b != null) s.setCenterY(b);
            if (c != null) s.setRadius(c);
        } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
            if (lr.getHitRect() == null) lr.setHitRect(new Spm.SPMRect());
            applyRect(lr.getHitRect(), a, b, c, d);
        } else if (hit instanceof LegacyRectHitArea lr2) {
            if (lr2.getHitRect() == null) lr2.setHitRect(new Spm.SPMRect());
            applyRect(lr2.getHitRect(), a, b, c, d);
        }
    }

    private void applyRect(Spm.SPMRect rect, Integer l, Integer t, Integer r, Integer b) {
        if (l != null) rect.setLeft(l);
        if (t != null) rect.setTop(t);
        if (r != null) rect.setRight(r);
        if (b != null) rect.setBottom(b);
    }

    private static Spm.SPMRect cloneRect(Spm.SPMRect rect) {
        if (rect == null) return null;
        Spm.SPMRect copy = new Spm.SPMRect();
        copy.setLeft(rect.getLeft());
        copy.setTop(rect.getTop());
        copy.setRight(rect.getRight());
        copy.setBottom(rect.getBottom());
        return copy;
    }

    private void updateRect(Spm.SPMRect rect, Integer x, Integer y, Integer w, Integer h) {
        if (rect == null) return;
        if (x != null) {
            rect.setLeft(x);
        }
        if (y != null) {
            rect.setTop(y);
        }
        if (w != null) {
            rect.setRight(x != null ? x + w : rect.getLeft() + w);
        }
        if (h != null) {
            rect.setBottom(y != null ? y + h : rect.getTop() + h);
        }
    }

    private int parseIntOr(String text, Integer fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            return fallback == null ? 0 : fallback;
        }
    }

    private Integer parseNullable(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean invalidPage(int pageIdx) {
        return spmEntry == null || spmEntry.getSpm() == null || pageIdx < 0
                || spmEntry.getSpm().getPageData() == null
                || pageIdx >= spmEntry.getSpm().getPageData().size();
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 确保在当前 SPM 所在目录下创建 tmp 子目录，供导出/保存使用，避免覆盖原文件。
     */
    private File ensureTmpDir() {
        File baseDir = null;
        if (spmEntry != null && spmEntry.getPath() != null && spmEntry.getPath().getParent() != null) {
            baseDir = spmEntry.getPath().getParent().toFile();
        } else if (workingDirectory != null) {
            baseDir = workingDirectory.toFile();
        }
        if (baseDir == null) {
            return null;
        }
        File tmp = new File(baseDir, "tmp");
        if (!tmp.exists()) {
            boolean ok = tmp.mkdirs();
            if (!ok) {
                log.warn("failed to create tmp dir: {}", tmp.getAbsolutePath());
            }
        }
        return tmp;
    }

    public SpmEntry getSpmEntry() {
        return spmEntry;
    }

    public Deque<Command> getUndoStack() {
        return undoStack;
    }

    private static class Rect {
        int x, y, w, h;
        static Rect from(Spm.SPMRect r) {
            Rect rr = new Rect();
            if (r == null) return rr;
            int L = r.getLeft() == null ? 0 : r.getLeft();
            int T = r.getTop() == null ? 0 : r.getTop();
            int R = r.getRight() == null ? L : r.getRight();
            int B = r.getBottom() == null ? T : r.getBottom();
            rr.x = L;
            rr.y = T;
            rr.w = R - L;
            rr.h = B - T;
            return rr;
        }
    }

    private static class ChipSnapshot {
        private final Spm.SPMRect dst;
        private final Spm.SPMRect src;
        private final Integer imageNo;
        private final Long drawOpt;
        private final Long drawOptVal;
        private final Integer option;

        private ChipSnapshot(Spm.SPMRect dst, Spm.SPMRect src, Integer imageNo, Long drawOpt, Long drawOptVal, Integer option) {
            this.dst = dst;
            this.src = src;
            this.imageNo = imageNo;
            this.drawOpt = drawOpt;
            this.drawOptVal = drawOptVal;
            this.option = option;
        }

        static ChipSnapshot from(Spm.SPMChipData chip) {
            return new ChipSnapshot(cloneRect(chip.getDstRect()), cloneRect(chip.getSrcRect()),
                    chip.getImageNo(), chip.getDrawOption(), chip.getDrawOptionValue(), chip.getOption());
        }

        void restore(Spm.SPMChipData chip) {
            chip.setDstRect(cloneRect(dst));
            chip.setSrcRect(cloneRect(src));
            chip.setImageNo(imageNo);
            chip.setDrawOption(drawOpt);
            chip.setDrawOptionValue(drawOptVal);
            chip.setOption(option);
        }
    }

    private static class HitSnapshot {
        private final Integer a, b, c, d;
        private final Spm.SPMRect rectCopy;
        HitSnapshot(Integer a, Integer b, Integer c, Integer d, Spm.SPMRect rectCopy) {
            this.a = a; this.b = b; this.c = c; this.d = d; this.rectCopy = rectCopy;
        }
        static HitSnapshot from(Spm.SPMHitArea hit) {
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r && r.getRect() != null) {
                return new HitSnapshot(r.getRect().getLeft(), r.getRect().getTop(), r.getRect().getRight(), r.getRect().getBottom(), cloneRect(r.getRect()));
            }
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea d) {
                return new HitSnapshot(d.getXMin(), d.getYMin(), d.getXMax(), d.getYMax(), null);
            }
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect rr) {
                return new HitSnapshot(rr.getCenterX(), rr.getCenterY(), rr.getWidth(), rr.getHeight(), null);
            }
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle c) {
                return new HitSnapshot(c.getCenterX(), c.getCenterY(), c.getRadius(), null, null);
            }
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
                return new HitSnapshot(null, null, null, null, cloneRect(lr.getHitRect()));
            }
            if (hit instanceof LegacyRectHitArea lr2) {
                return new HitSnapshot(null, null, null, null, cloneRect(lr2.getHitRect()));
            }
            return new HitSnapshot(null, null, null, null, null);
        }
        void restore(Spm.SPMHitArea hit) {
            if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRect r) {
                r.setRect(cloneRect(rectCopy));
            } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.DefaultHitArea df) {
                if (a != null) df.setXMin(a);
                if (b != null) df.setYMin(b);
                if (c != null) df.setXMax(c);
                if (d != null) df.setYMax(d);
            } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CRotatableRect rr) {
                if (a != null) rr.setCenterX(a);
                if (b != null) rr.setCenterY(b);
                if (c != null) rr.setWidth(c);
                if (d != null) rr.setHeight(d);
            } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bhe.c.CCircle cc) {
                if (a != null) cc.setCenterX(a);
                if (b != null) cc.setCenterY(b);
                if (c != null) cc.setRadius(c);
            } else if (hit instanceof com.karaik.spmviewer.spm.hitarea.bsdx.LegacyRectHitArea lr) {
                lr.setHitRect(cloneRect(rectCopy));
            } else if (hit instanceof LegacyRectHitArea lr2) {
                lr2.setHitRect(cloneRect(rectCopy));
            }
        }
    }
}
