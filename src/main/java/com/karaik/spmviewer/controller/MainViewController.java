package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.karaik.spmviewer.controller.UiStateController;

import java.awt.image.BufferedImage;
import java.net.URL;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.karaik.spmviewer.controller.AnimationPlanBuilder.AnimationFrame;
import com.karaik.spmviewer.controller.editor.EditorViewController;

@Slf4j
public class MainViewController {

    @FXML private ComboBox<Settings.ParsingMode> parsingModeSelector;
    @FXML private CheckBox showCoordsCheck;
    @FXML private CheckBox showHitboxCheck;
    @FXML private CheckBox showChipBoundsCheck;
    @FXML private CheckBox showPageBoundsCheck;
    @FXML private ComboBox<Settings.BackgroundMode> backgroundModeSelector;
    @FXML private ColorPicker backgroundColorPicker;
    @FXML private ComboBox<Settings.OriginMode> originModeSelector;
    @FXML private TextField searchField;
    @FXML private Slider zoomSlider;
    @FXML private TextField zoomField;
    @FXML private ComboBox<String> animSelector;
    @FXML private CheckBox autoPlayCheck;
    @FXML private Slider animFrameSlider;
    @FXML private Label animFrameLabel;
    @FXML private Slider fpsSlider;
    @FXML private TextField fpsField;
    @FXML private CheckBox alwaysOnTopCheck;
    @FXML private ListView<SpmEntry> spmListView;
    @FXML private TreeView<String> pageTree;
    @FXML private TreeView<String> animTree;
    @FXML private ListView<String> imageList;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane canvasHolder;
    @FXML private TableView<Spm.SPMChipData> chipTable;
    @FXML private TableView<Spm.SPMHitArea> hitTable;
    @FXML private Label lblVersion, lblPages, lblImages, lblAnims;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TitledPane animTitledPane;
    @FXML private TextField animSearchField;
    //</editor-fold>

    private SpmFileHandler spmFileHandler;
    private UiStateController uiStateController;
    private CanvasController canvasController;
    private Timeline animTimeline;
    private double dragStartX, dragStartY, hValStart, vValStart;
    private final ObservableList<SpmEntry> masterSpmList = FXCollections.observableArrayList();
    private SpmEntry currentlyLoadingSpm = null;
    private long loadGen = 0;

    private final ImageRepository imageRepository = new ImageRepository();
    private List<AnimationFrame> currentAnimationFrames = Collections.emptyList();
    private int currentAnimationIndex = -1;
    private boolean updatingFrameSlider = false;
    private boolean animSelectionFromTree = false;
    private TreeSelectionContext pendingTreeSelection;

    @FXML
    public void initialize() {
        setupCanvas();
        setupControllers();
        setupUIComponents();
        loadLastDirectory();
    }

    private void setupCanvas() {
        final Scale scale = new Scale(1, 1, 0, 0);
        final Group canvasGroup = new Group();
        canvasController = new CanvasController(new javafx.scene.canvas.Canvas(), canvasGroup, scale);
        canvasHolder.getChildren().add(canvasGroup);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scale.setX(newVal.doubleValue());
            scale.setY(newVal.doubleValue());
        });
    }

    private void setupControllers() {
        spmFileHandler = new SpmFileHandler(masterSpmList, progressBar, statusLabel);
        uiStateController = new UiStateController(
                pageTree, animTree, imageList, animSelector,
                lblVersion, lblPages, lblImages, lblAnims,
                chipTable, hitTable
        );
        imageRepository.setExtraSearchRoots(Settings.getImageSearchRoots());
    }

    private void setupUIComponents() {
        setupParsingModeSelector();
        setupDisplayOptionListeners();
        setupBackgroundControls();
        setupOriginControls();
        setupAnimationControls();
        setupSpmListCellFactory();
        setupSpmListContextMenu();
        setupActionListeners();
        setupCanvasInteractions();
        setupValueBindings();
        setupFiltering();
        setupExpandAllContextMenus();
        uiStateController.clearAllPanels();
        canvasController.clearCanvas(Settings.getBackgroundMode(), Settings.getBackgroundColor());
    }

    private void setupParsingModeSelector() {
        parsingModeSelector.getItems().setAll(Settings.ParsingMode.values());
        parsingModeSelector.setValue(Settings.getParsingMode());
        parsingModeSelector.valueProperty().addListener((obs, oldMode, newMode) -> {
            if (newMode != null) {
                Settings.setParsingMode(newMode);
                spmFileHandler.reloadDirectory();
            }
        });
    }

    private void setupDisplayOptionListeners() {
        showCoordsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> renderCurrentPage());
        showHitboxCheck.selectedProperty().addListener((obs, oldVal, newVal) -> renderCurrentPage());

        autoPlayCheck.setSelected(Settings.isAutoPlayEnabled());
        autoPlayCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Settings.setAutoPlayEnabled(newVal);
            if (!newVal) {
                onStop();
            } else if (animSelector.getValue() != null) {
                onPlay();
            }
        });
    }

    private void setupOriginControls() {
        originModeSelector.getItems().setAll(Settings.OriginMode.values());
        originModeSelector.setValue(Settings.getOriginMode());
        originModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Settings.setOriginMode(newVal);
                renderCurrentPage();
            }
        });
    }

    private void setupAnimationControls() {
        animFrameSlider.setMin(0);
        animFrameSlider.setMax(0);
        animFrameSlider.setValue(0);
        animFrameSlider.setDisable(true);
        animFrameLabel.setText("0/0");
        animFrameSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingFrameSlider) {
                return;
            }
            if (currentAnimationFrames.isEmpty()) {
                return;
            }
            int target = (int) Math.round(newVal.doubleValue());
            if (target < 0 || target >= currentAnimationFrames.size()) {
                return;
            }
            if (target == currentAnimationIndex) {
                return;
            }
            onStop();
            displayAnimationFrame(target, false);
        });
    }
    private void setupBackgroundControls() {
        backgroundModeSelector.getItems().setAll(Settings.BackgroundMode.values());
        backgroundModeSelector.setValue(Settings.getBackgroundMode());
        backgroundColorPicker.setValue(Settings.getBackgroundColor());

        backgroundColorPicker.visibleProperty().bind(
                backgroundModeSelector.valueProperty().isEqualTo(Settings.BackgroundMode.SOLID_COLOR)
        );

        backgroundModeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Settings.setBackgroundMode(newVal);
                renderCurrentPage();
            }
        });

        backgroundColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Settings.setBackgroundColor(newVal);
                if (Settings.getBackgroundMode() == Settings.BackgroundMode.SOLID_COLOR) {
                    renderCurrentPage();
                }
            }
        });
    }

    private void setupSpmListCellFactory() {
        spmListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SpmEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setDisable(false);
                } else {
                    setText(item.toString());
                    if (item.getStatus() == SpmEntry.Status.FAILED) {
                        setTextFill(Color.RED); setTooltip(new Tooltip(item.getErrorMessage())); setDisable(true);
                    } else {
                        setTextFill(Color.BLACK); setTooltip(null); setDisable(false);
                    }
                }
            }
        });
    }

    private void setupSpmListContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> openEditor(spmListView.getSelectionModel().getSelectedItem()));
        menu.getItems().add(editItem);
        menu.setOnShowing(e -> {
            SpmEntry sel = spmListView.getSelectionModel().getSelectedItem();
            boolean enable = sel != null && sel.getStatus() == SpmEntry.Status.SUCCESS && sel.getSpm() != null;
            editItem.setDisable(!enable);
        });
        spmListView.setContextMenu(menu);
    }

    private void setupActionListeners() {
        spmListView.setOnMouseClicked(event -> handleSpmSelection());
        spmListView.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) handleSpmSelection();
        });

        masterSpmList.addListener((ListChangeListener<SpmEntry>) c -> {
            if (!masterSpmList.isEmpty() && spmListView.getSelectionModel().getSelectedItem() == null) {
                if (!spmListView.getItems().isEmpty()) {
                    spmListView.getSelectionModel().selectFirst();
                    handleSpmSelection();
                }
            }
        });

        pageTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null && item.isLeaf()) {
                try {
                    selectPage(Integer.parseInt(item.getValue().split(" ")[0]));
                } catch (NumberFormatException ignored) {}
            }
        });

        animTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item == null || !item.isLeaf()) {
                return;
            }
            String value = item.getValue();
            if (value == null || !value.contains("pages=[")) {
                return;
            }
            TreeItem<String> parent = item.getParent();
            if (parent == null) {
                return;
            }
            String animDisplayName = parent.getValue();
            int animIndex = parseAnimIndex(animDisplayName);
            int patIndex = parsePatIndex(value);
            List<Integer> pageNos = parsePageNumbers(value);
            if (animIndex < 0 || patIndex < 0 || pageNos.isEmpty()) {
                return;
            }
            pendingTreeSelection = new TreeSelectionContext(animIndex, patIndex, pageNos);
            animSelectionFromTree = true;
            boolean alreadySelected = animDisplayName.equals(animSelector.getValue());
            if (!alreadySelected) {
                animSelector.getSelectionModel().select(animDisplayName);
            } else {
                handleTreeSelectionAfterAnimUpdate();
            }
        });

        imageList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null) {
                try {
                    previewImage(Integer.parseInt(item.split(":")[0].trim()), item);
                } catch (NumberFormatException ignored) {}
            }
        });

        animSelector.setOnAction(e -> {
            if (animSelector.getValue() == null) {
                pendingTreeSelection = null;
                animSelectionFromTree = false;
                return;
            }
            boolean triggeredByTree = animSelectionFromTree;
            boolean displayFirstFrame = !triggeredByTree;
            if (!prepareAnimationFrames(displayFirstFrame)) {
                if (triggeredByTree) {
                    handleTreeSelectionAfterAnimUpdate();
                }
                onStop();
                return;
            }
            if (triggeredByTree) {
                handleTreeSelectionAfterAnimUpdate();
            }
            if (autoPlayCheck.isSelected()) {
                onPlay();
            } else {
                onStop();
            }
            animSelectionFromTree = false;
        });
        alwaysOnTopCheck.selectedProperty().addListener((obs, oldVal, newVal) -> ((Stage) canvasHolder.getScene().getWindow()).setAlwaysOnTop(newVal));
        canvasHolder.sceneProperty().addListener((obs, oldScene, newScene) -> { if (newScene != null) setupKeyboardShortcuts(newScene); });
    }

    private void setupCanvasInteractions() {
        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
                hValStart = scrollPane.getHvalue();
                vValStart = scrollPane.getVvalue();
                e.consume();
            }
        });
        scrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                double contentWidth = Math.max(1, scrollPane.getContent().getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth());
                double contentHeight = Math.max(1, scrollPane.getContent().getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight());
                scrollPane.setHvalue(hValStart - (e.getSceneX() - dragStartX) / contentWidth);
                scrollPane.setVvalue(vValStart - (e.getSceneY() - dragStartY) / contentHeight);
                e.consume();
            }
        });

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                zoomSlider.setValue(zoomSlider.getValue() * (event.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1));
                event.consume();
            }
        });
    }

    private void setupValueBindings() {
        NumberFormat zoomFormat = new DecimalFormat("0.0x");
        StringConverter<Number> zoomConverter = new NumberStringConverter(zoomFormat);
        zoomField.textProperty().bindBidirectional(zoomSlider.valueProperty(), zoomConverter);

        NumberFormat fpsFormat = new DecimalFormat("0");
        StringConverter<Number> fpsConverter = new NumberStringConverter(fpsFormat);
        fpsField.textProperty().bindBidirectional(fpsSlider.valueProperty(), fpsConverter);
    }

    private void setupFiltering() {
        FilteredList<SpmEntry> filteredSpmList = new FilteredList<>(masterSpmList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSpmList.setPredicate(spmEntry -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return spmEntry.getPath().getFileName().toString().toLowerCase().contains(lowerCaseFilter);
            });
            if (spmListView.getSelectionModel().getSelectedItem() == null && !spmListView.getItems().isEmpty()) {
                spmListView.getSelectionModel().selectFirst();
                handleSpmSelection();
            }
        });
        spmListView.setItems(filteredSpmList);

        animSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
            if (selectedSpm != null && selectedSpm.getSpm() != null) {
                uiStateController.updateUiForSpm(selectedSpm.getSpm(), newVal);
                if (newVal != null && !newVal.trim().isEmpty()) {
                    animTitledPane.setExpanded(true);
                }
            }
        });
    }

    private void setupExpandAllContextMenus() {
        setupExpandAllContextMenu(pageTree);
        setupExpandAllContextMenu(animTree);
    }

    private void setupExpandAllContextMenu(TreeView<?> treeView) {
        var contextMenu = new ContextMenu();
        var expandAllItem = new MenuItem("Expand All");
        expandAllItem.setOnAction(event -> {
            if (treeView.getRoot() != null) {
                expandTreeView(treeView.getRoot(), true);
            }
        });
        contextMenu.getItems().add(expandAllItem);
        treeView.setContextMenu(contextMenu);
    }

    private void expandTreeView(TreeItem<?> item, boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandTreeView(child, expand);
            }
        }
    }

    private void setupKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.PLAY || event.getCode() == KeyCode.PAUSE) {
                if (animTimeline != null && animTimeline.getStatus() == Timeline.Status.RUNNING) {
                    onStop();
                } else {
                    onPlay();
                }
                event.consume();
            }
        });
    }

    private void loadLastDirectory() {
        String lastDirPath = Settings.getLastDirectory();
        if (lastDirPath != null && !lastDirPath.isEmpty()) {
            Path path = Path.of(lastDirPath);
            if (Files.isDirectory(path)) {
                spmFileHandler.loadDirectory(path);
            }
        }
    }

    private void handleSpmSelection() {
        loadAndDisplaySpm(spmListView.getSelectionModel().getSelectedItem());
    }

    private void loadAndDisplaySpm(SpmEntry entry) {

        // 本次加载的世代号
        final long myGen = ++loadGen;

        // 停止任何正在播放的动画
        onStop();

        // 非法/未成功的条目：直接清空并退出
        if (entry == null || entry.getStatus() != SpmEntry.Status.SUCCESS) {
            currentlyLoadingSpm = null;
            uiStateController.clearAllPanels();
            canvasController.clearCanvas(Settings.getBackgroundMode(), Settings.getBackgroundColor());
            statusLabel.setText("Ready");
            progressBar.setVisible(false);
            return;
        }

        currentlyLoadingSpm = entry;

        // UI：进入“加载中”状态
        progressBar.progressProperty().unbind();
        statusLabel.setText("Loading " + entry.getPath().getFileName() + "...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        animSearchField.clear();
        uiStateController.clearAllPanels();
        canvasController.clearCanvas(Settings.getBackgroundMode(), Settings.getBackgroundColor());

        // 后台任务：只做耗时的IO/解析，避免访问FX线程对象
        Task<List<Image>> loadTask = new Task<>() {
            @Override protected List<Image> call() {
                return imageRepository.loadImages(entry.getSpm(), spmFileHandler.getCurrentDirectory());
            }
        };

        loadTask.setOnSucceeded(e -> {
            // 如果在此期间又点了别的文件，这次结果作废
            if (myGen != loadGen) {
                log.debug("Discarded stale image load for {}", entry.getPath().getFileName());
                return;
            }

            List<Image> images = Optional.ofNullable(loadTask.getValue()).orElse(List.of());
            canvasController.setCurrentSpm(entry.getSpm(), images);
            uiStateController.updateUiForSpm(entry.getSpm(), null);
            if (Optional.ofNullable(entry.getSpm().getNumPageData()).orElse(0) > 0) selectPage(0);
            else {
                uiStateController.updateTablesForPage(null);
                canvasController.clearCanvas(Settings.getBackgroundMode(), Settings.getBackgroundColor());
            }
            // 确保UI更新后将ScrollPane滚动到中心
            Platform.runLater(this::centerScrollPane);

            statusLabel.setText("Ready");
            progressBar.setVisible(false);
            currentlyLoadingSpm = null;
        });

        loadTask.setOnFailed(e -> {
            if (myGen != loadGen) {
                log.debug("Discarded stale image load failure for {}", entry.getPath().getFileName());
                return;
            }
            log.error("Failed to load {}", entry.getPath().getFileName(), loadTask.getException());
            statusLabel.setText("Failed to load " + entry.getPath().getFileName());
            progressBar.setVisible(false);
            currentlyLoadingSpm = null;
        });

        Thread t = new Thread(loadTask, "spm-load-" + myGen);
        t.setDaemon(true);
        t.start();
    }

    private void openEditor(SpmEntry entry) {
        if (entry == null || entry.getStatus() != SpmEntry.Status.SUCCESS || entry.getSpm() == null) {
            return;
        }
        try {
            URL fxml = getClass().getResource("/fxml/EditorView.fxml");
            if (fxml == null) {
                throw new IllegalStateException("EditorView.fxml not found");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            EditorViewController controller = loader.getController();
            controller.init(entry, spmFileHandler.getCurrentDirectory());

            Stage stage = new Stage();
            stage.setTitle("Edit - " + entry.getPath().getFileName());
            stage.setScene(new Scene(root, 1080, 720));
            URL iconUrl = getClass().getResource("/images/head.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
            stage.initOwner(canvasHolder.getScene().getWindow());
            stage.show();
        } catch (Exception ex) {
            log.error("Failed to open editor", ex);
            new Alert(Alert.AlertType.ERROR, "Failed to open editor: " + ex.getMessage()).showAndWait();
        }
    }

    private void selectPage(Integer index) {
        SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
        if (selectedSpm == null || index == null) return;
        imageList.getSelectionModel().clearSelection();
        Spm spm = selectedSpm.getSpm();
        int pageCount = Optional.ofNullable(spm.getNumPageData()).orElse(0);
        if (index < 0 || index >= pageCount) return;
        canvasController.setCurrentPageIndex(index);
        uiStateController.updateTablesForPage(spm.getPageData().get(index));
        renderCurrentPage();
    }

    private void previewImage(int index, String imageName) {
        onStop();
        pageTree.getSelectionModel().clearSelection();
        uiStateController.updateTablesForPage(null);
        log.debug("Previewing image index {} ({})", index, imageName);
        canvasController.previewImage(index, imageName, Settings.getBackgroundMode(), Settings.getBackgroundColor());
        Platform.runLater(this::centerScrollPane);
    }

    private void renderCurrentPage() {
        canvasController.renderPage(
                showCoordsCheck.isSelected(),
                showChipBoundsCheck.isSelected(),
                showHitboxCheck.isSelected(),
                showPageBoundsCheck.isSelected(),
                Settings.getBackgroundMode(),
                Settings.getBackgroundColor(),
                Settings.getOriginMode()
        );
    }

    /**
     * 将ScrollPane的滚动条设置到中心位置 (0.5, 0.5)。
     */
    private void centerScrollPane() {
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    @FXML private void onOpenDirectory() {
        spmFileHandler.openSpmDirectory(canvasHolder.getScene().getWindow());
    }

    @FXML private void onShowCharsetSettings() {
        CharsetDialog.showDialog(canvasHolder.getScene().getWindow());
    }

    @FXML private void onExportPng() {
        if (canvasController.getCurrentSpm() == null || canvasController.getCurrentPageIndex() < 0) {
            new Alert(Alert.AlertType.WARNING, "No page selected to export.").showAndWait();
            return;
        }
        var fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("spm-export.png");
        File out = fc.showSaveDialog(canvasHolder.getScene().getWindow());
        if (out == null) return;

        var prevMode  = Settings.getBackgroundMode();
        var prevColor = Settings.getBackgroundColor();
        boolean showCoords = showCoordsCheck.isSelected();
        boolean showChipBounds = showChipBoundsCheck.isSelected();
        boolean showHitboxes = showHitboxCheck.isSelected();
        boolean showPageBounds = showPageBoundsCheck.isSelected();
        Settings.OriginMode originMode = Settings.getOriginMode();

        try {
            Settings.setBackgroundMode(Settings.BackgroundMode.SOLID_COLOR);
            Settings.setBackgroundColor(Color.TRANSPARENT);

            var canvas = canvasController.getCanvas();
            var graphics = canvas.getGraphicsContext2D();
            graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            canvasController.renderPage(
                    showCoords,
                    showChipBounds,
                    showHitboxes,
                    false,
                    Settings.BackgroundMode.SOLID_COLOR,
                    Color.TRANSPARENT,
                    originMode
            );

            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            if (showPageBounds) {
                canvasController.getPageBoundsViewport(originMode).ifPresent(sp::setViewport);
            }
            WritableImage snapshot = canvas.snapshot(sp, null);

            writePng(snapshot, out.toPath());

            statusLabel.setText("Exported: " + out.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        } finally {
            Settings.setBackgroundMode(prevMode);
            Settings.setBackgroundColor(prevColor);
            renderCurrentPage();
        }
    }

    private void writePng(WritableImage image, Path path) throws IOException {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] buffer = new int[width];
        for (int y = 0; y < height; y++) {
            reader.getPixels(0, y, width, 1, PixelFormat.getIntArgbPreInstance(), buffer, 0, width);
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, buffer[x]);
            }
        }
        try (OutputStream os = Files.newOutputStream(path)) {
            ImageIO.write(buffered, "png", os);
        }
    }

    @FXML private void onQuit() {
        ((Stage) canvasHolder.getScene().getWindow()).close();
    }

    @FXML private void onShowShortcuts() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(canvasHolder.getScene().getWindow());
        alert.setTitle("Shortcuts");
        alert.setHeaderText("Keyboard and Mouse Shortcuts");
        alert.setContentText("Play/Pause Media Key: Toggle animation playback\n\n" +
                "Ctrl + Mouse Scroll: Zoom in/out on the canvas\n\n" +
                "Middle Mouse Button Drag: Pan the canvas");
        alert.showAndWait();
    }

    @FXML private void onAbout() {
        var aboutDialog = new Alert(Alert.AlertType.INFORMATION, "NeXAS Sprite Viewer");
        aboutDialog.initOwner(canvasHolder.getScene().getWindow());
        aboutDialog.showAndWait();
    }

    @FXML private void onZoom1x() {
        zoomSlider.setValue(1.0);
    }

    @FXML private void onFit() {
        // Not implemented
    }

    @FXML private void onPlay() {
        if (currentAnimationFrames.isEmpty()) {
            if (!prepareAnimationFrames(true)) {
                return;
            }
        } else if (currentAnimationIndex < 0 && !currentAnimationFrames.isEmpty()) {
            displayAnimationFrame(0, false);
        }
        startAnimationTimeline();
    }

    @FXML private void onStepPrev() {
        stepAnimation(-1);
    }

    @FXML private void onStepNext() {
        stepAnimation(1);
    }

    @FXML private void onStop() {
        if (animTimeline != null) {
            animTimeline.stop();
            animTimeline = null;
        }
    }

    private boolean prepareAnimationFrames(boolean displayFirstFrame) {
        SpmEntry entry = spmListView.getSelectionModel().getSelectedItem();
        if (entry == null || entry.getSpm() == null) {
            currentAnimationFrames = Collections.emptyList();
            currentAnimationIndex = -1;
            updateFrameUi();
            return false;
        }
        String displayName = animSelector.getValue();
        if (displayName == null) {
            currentAnimationFrames = Collections.emptyList();
            currentAnimationIndex = -1;
            updateFrameUi();
            return false;
        }
        List<Spm.SPMAnimData> animList = Optional.ofNullable(entry.getSpm().getAnimData()).orElse(List.of());
        int animIndex = parseAnimIndex(displayName);
        if (animIndex < 0 || animIndex >= animList.size()) {
            currentAnimationFrames = Collections.emptyList();
            currentAnimationIndex = -1;
            updateFrameUi();
            return false;
        }
        int patCount = Optional.ofNullable(entry.getSpm().getPatPageNum()).orElse(0);
        List<AnimationFrame> frames = AnimationPlanBuilder.build(animList.get(animIndex), patCount);
        if (frames.isEmpty()) {
            currentAnimationFrames = Collections.emptyList();
            currentAnimationIndex = -1;
            updateFrameUi();
            return false;
        }
        currentAnimationFrames = frames;
        currentAnimationIndex = -1;
        animFrameSlider.setDisable(frames.size() <= 1);
        animFrameSlider.setMax(Math.max(0, frames.size() - 1));
        if (displayFirstFrame) {
            displayAnimationFrame(0, false);
        } else {
            updateFrameUi();
        }
        return true;
    }

    private void startAnimationTimeline() {
        onStop();
        if (currentAnimationFrames.isEmpty()) {
            return;
        }
        if (currentAnimationIndex < 0) {
            displayAnimationFrame(0, false);
        }
        double fpsValue = Math.max(1.0, fpsSlider.getValue());
        double baseMillis = 1000.0 / fpsValue;
        List<KeyFrame> keyFrames = new ArrayList<>();
        int cumulative = 0;
        for (int i = 0; i < currentAnimationFrames.size(); i++) {
            AnimationFrame frame = currentAnimationFrames.get(i);
            int duration = (int) Math.round(frame.effectiveWaitFrames() * baseMillis);
            if (duration <= 0) {
                duration = (int) Math.round(baseMillis);
            }
            duration = Math.max(duration, 16);
            cumulative += duration;
            final int target = (i + 1) % currentAnimationFrames.size();
            keyFrames.add(new KeyFrame(Duration.millis(cumulative), e -> displayAnimationFrame(target, true)));
        }
        animTimeline = new Timeline(keyFrames.toArray(new KeyFrame[0]));
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        animTimeline.playFromStart();
    }

    private void displayAnimationFrame(int index, boolean fromTimeline) {
        if (currentAnimationFrames.isEmpty()) {
            return;
        }
        int size = currentAnimationFrames.size();
        int normalized = ((index % size) + size) % size;
        currentAnimationIndex = normalized;
        AnimationFrame frame = currentAnimationFrames.get(normalized);
        selectPage(frame.pageNo());
        updateFrameUi();
    }

    private void updateFrameUi() {
        if (currentAnimationFrames.isEmpty()) {
            updatingFrameSlider = true;
            animFrameSlider.setDisable(true);
            animFrameSlider.setMax(0);
            animFrameSlider.setValue(0);
            updatingFrameSlider = false;
            animFrameLabel.setText("0/0");
            return;
        }
        int size = currentAnimationFrames.size();
        updatingFrameSlider = true;
        animFrameSlider.setDisable(size <= 1);
        animFrameSlider.setMax(Math.max(0, size - 1));
        animFrameSlider.setValue(currentAnimationIndex < 0 ? 0 : currentAnimationIndex);
        updatingFrameSlider = false;
        int displayIndex = currentAnimationIndex < 0 ? 0 : currentAnimationIndex + 1;
        animFrameLabel.setText(displayIndex + "/" + size);
    }

    private void stepAnimation(int delta) {
        if (currentAnimationFrames.isEmpty()) {
            if (!prepareAnimationFrames(true)) {
                return;
            }
        }
        if (currentAnimationFrames.isEmpty()) {
            return;
        }
        int size = currentAnimationFrames.size();
        int current = currentAnimationIndex < 0 ? 0 : currentAnimationIndex;
        int next = Math.floorMod(current + delta, size);
        onStop();
        displayAnimationFrame(next, false);
    }

    private void handleTreeSelectionAfterAnimUpdate() {
        if (pendingTreeSelection == null) {
            animSelectionFromTree = false;
            return;
        }
        TreeSelectionContext ctx = pendingTreeSelection;
        pendingTreeSelection = null;
        int frameIndex = findFrameIndexForTreeSelection(ctx);
        if (frameIndex >= 0) {
            displayAnimationFrame(frameIndex, false);
        } else if (!ctx.pageNos().isEmpty()) {
            selectPage(ctx.pageNos().get(0));
        }
        animSelectionFromTree = false;
    }

    private int findFrameIndexForTreeSelection(TreeSelectionContext ctx) {
        SpmEntry entry = spmListView.getSelectionModel().getSelectedItem();
        if (entry == null || entry.getSpm() == null) {
            return -1;
        }
        List<Spm.SPMAnimData> animList = Optional.ofNullable(entry.getSpm().getAnimData()).orElse(List.of());
        if (ctx.animIndex() < 0 || ctx.animIndex() >= animList.size()) {
            return -1;
        }
        Spm.SPMAnimData anim = animList.get(ctx.animIndex());
        List<Spm.SPMPatData> pats = Optional.ofNullable(anim.getPatData()).orElse(List.of());
        if (ctx.patIndex() < 0 || ctx.patIndex() >= pats.size()) {
            return -1;
        }
        int patPageLimit = Optional.ofNullable(entry.getSpm().getPatPageNum()).orElse(0);
        int offset = 0;
        for (int i = 0; i < pats.size(); i++) {
            Spm.SPMPatData pat = pats.get(i);
            List<Integer> pageNos = Optional.ofNullable(pat.getPageNo()).orElse(List.of());
            if (pageNos.isEmpty()) {
                continue;
            }
            int limit = patPageLimit > 0 ? Math.min(patPageLimit, pageNos.size()) : pageNos.size();
            if (limit <= 0) {
                continue;
            }
            if (i == ctx.patIndex()) {
                int targetPage = ctx.pageNos().isEmpty() ? pageNos.get(0) : ctx.pageNos().get(0);
                for (int j = 0; j < limit; j++) {
                    Integer pageNo = pageNos.get(j);
                    if (pageNo != null && pageNo.equals(targetPage)) {
                        return offset + j;
                    }
                }
                return offset;
            }
            offset += limit;
        }
        return -1;
    }

    private int parsePatIndex(String value) {
        if (value == null) {
            return -1;
        }
        try {
            int start = value.indexOf("pat[");
            if (start >= 0) {
                int end = value.indexOf(']', start);
                if (end > start) {
                    return Integer.parseInt(value.substring(start + 4, end));
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to parse pat index from {}", value, ex);
        }
        return -1;
    }

    private List<Integer> parsePageNumbers(String value) {
        if (value == null) {
            return List.of();
        }
        int pagesIdx = value.indexOf("pages=[");
        if (pagesIdx < 0) {
            return List.of();
        }
        int start = value.indexOf('[', pagesIdx);
        int end = value.indexOf(']', start);
        if (start < 0 || end <= start) {
            return List.of();
        }
        String inside = value.substring(start + 1, end).trim();
        if (inside.isEmpty()) {
            return List.of();
        }
        String[] tokens = inside.split(",");
        List<Integer> result = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ex) {
                log.debug("Skipping non-numeric page token '{}' in {}", trimmed, value);
            }
        }
        return result;
    }

    private int parseAnimIndex(String displayName) {
        try {
            int start = displayName.indexOf('[');
            int end = displayName.indexOf(']');
            if (start >= 0 && end > start) {
                return Integer.parseInt(displayName.substring(start + 1, end));
            }
        } catch (Exception ex) {
            log.warn("Failed to parse animation index from {}", displayName, ex);
        }
        return -1;
    }

    private record TreeSelectionContext(int animIndex, int patIndex, List<Integer> pageNos) { }
}
