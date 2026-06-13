package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Scale;
import javafx.stage.DirectoryChooser;
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
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

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
    private boolean exportInProgress = false;
    private ExportContext currentExportContext;
    private Task<?> currentBusyTask;
    private ChangeListener<String> busyStatusListener;

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
        showCoordsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshActiveView());
        showHitboxCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshActiveView());
        showChipBoundsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshActiveView());
        showPageBoundsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshActiveView());

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
                refreshActiveView();
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
                refreshActiveView();
            }
        });

        backgroundColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Settings.setBackgroundColor(newVal);
                if (Settings.getBackgroundMode() == Settings.BackgroundMode.SOLID_COLOR) {
                    refreshActiveView();
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
                    int idx = Integer.parseInt(item.split(":")[0].trim());
                    refreshImagePreviewWithBounds(idx);
                } catch (NumberFormatException ignored) {}
            }
        });
        setupImageContextMenu();

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
        setupExpandAllContextMenu(pageTree, true);
        setupExpandAllContextMenu(animTree, false);
    }

    private void setupImageContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem exportChip = new MenuItem("Export Chip Bounds Only (PNG)");
        exportChip.setOnAction(e -> exportImageWithBounds(true));
        MenuItem exportPage = new MenuItem("Export Page Bounds Only (PNG)");
        exportPage.setOnAction(e -> exportImageWithBounds(false));
        menu.getItems().addAll(exportChip, exportPage);
        imageList.setContextMenu(menu);
    }

    private void setupExpandAllContextMenu(TreeView<?> treeView, boolean pageTreeMenu) {
        var contextMenu = new ContextMenu();
        var expandAllItem = new MenuItem("Expand All");
        expandAllItem.setOnAction(event -> {
            if (treeView.getRoot() != null) {
                expandTreeView(treeView.getRoot(), true);
            }
        });
        contextMenu.getItems().add(expandAllItem);
        if (pageTreeMenu) {
            MenuItem exportPagesByPage = new MenuItem("Export All Pages by Page Bounds");
            exportPagesByPage.setOnAction(event -> exportPagesBatch(false));
            MenuItem exportPagesByChip = new MenuItem("Export All Pages by Chip Bounds");
            exportPagesByChip.setOnAction(event -> exportPagesBatch(true));
            contextMenu.getItems().addAll(new SeparatorMenuItem(), exportPagesByPage, exportPagesByChip);
            contextMenu.setOnShowing(event -> {
                boolean enabled = hasLoadedSpm() && isRootTreeSelection(pageTree);
                exportPagesByPage.setDisable(!enabled || exportInProgress);
                exportPagesByChip.setDisable(!enabled || exportInProgress);
            });
        } else {
            MenuItem exportAnimsByPage = new MenuItem("Export All Animation Slots by Page Bounds");
            exportAnimsByPage.setOnAction(event -> exportAnimationsBatch(false));
            MenuItem exportAnimsByChip = new MenuItem("Export All Animation Slots by Chip Bounds");
            exportAnimsByChip.setOnAction(event -> exportAnimationsBatch(true));
            contextMenu.getItems().addAll(new SeparatorMenuItem(), exportAnimsByPage, exportAnimsByChip);
            contextMenu.setOnShowing(event -> {
                boolean enabled = hasLoadedSpm() && isRootTreeSelection(animTree);
                exportAnimsByPage.setDisable(!enabled || exportInProgress);
                exportAnimsByChip.setDisable(!enabled || exportInProgress);
            });
        }
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
            setStatusText("Ready");
            progressBar.setVisible(false);
            return;
        }

        currentlyLoadingSpm = entry;

        // UI：进入“加载中”状态
        progressBar.progressProperty().unbind();
        setStatusText("Loading " + entry.getPath().getFileName() + "...");
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

            setStatusText("Ready");
            progressBar.setVisible(false);
            currentlyLoadingSpm = null;
        });

        loadTask.setOnFailed(e -> {
            if (myGen != loadGen) {
                log.debug("Discarded stale image load failure for {}", entry.getPath().getFileName());
                return;
            }
            log.error("Failed to load {}", entry.getPath().getFileName(), loadTask.getException());
            setStatusText("Failed to load " + entry.getPath().getFileName());
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
        currentExportContext = ExportContext.page(index);
        renderCurrentPage();
    }

    private void previewImage(int index, String imageName) {
        onStop();
        pageTree.getSelectionModel().clearSelection();
        uiStateController.updateTablesForPage(null);
        log.debug("Previewing image index {} ({})", index, imageName);
        currentExportContext = ExportContext.image(index);
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

    private void refreshActiveView() {
        int imageIndex = getSelectedImageIndex();
        if (imageIndex >= 0) {
            refreshImagePreviewWithBounds(imageIndex);
            return;
        }
        renderCurrentPage();
    }

    private boolean hasLoadedSpm() {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        return selected != null && selected.getStatus() == SpmEntry.Status.SUCCESS && selected.getSpm() != null;
    }

    private boolean isRootTreeSelection(TreeView<?> treeView) {
        if (treeView == null || treeView.getRoot() == null) {
            return false;
        }
        TreeItem<?> selected = treeView.getSelectionModel().getSelectedItem();
        return selected == null || selected == treeView.getRoot();
    }

    private void exportPagesBatch(boolean chipMode) {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getSpm() == null) {
            return;
        }
        Path targetDir = chooseExportDirectory(chipMode ? "Export Pages by Chip Bounds" : "Export Pages by Page Bounds");
        if (targetDir == null) {
            return;
        }
        List<ExportItem> items = buildPageExportItems(selected, chipMode);
        startBatchExport(items, targetDir, chipMode ? "Exporting page chips..." : "Exporting pages...");
    }

    private void exportAnimationsBatch(boolean chipMode) {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getSpm() == null) {
            return;
        }
        Path targetDir = chooseExportDirectory(chipMode ? "Export Animation Slots by Chip Bounds" : "Export Animation Slots by Page Bounds");
        if (targetDir == null) {
            return;
        }
        List<ExportItem> items = buildAnimationExportItems(selected, chipMode);
        startBatchExport(items, targetDir, chipMode ? "Exporting animation chips..." : "Exporting animation slots...");
    }

    private Path chooseExportDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        Path lastExport = Settings.getLastExportDir();
        if (lastExport != null && lastExport.toFile().isDirectory()) {
            chooser.setInitialDirectory(lastExport.toFile());
        }
        File chosen = chooser.showDialog(canvasHolder.getScene().getWindow());
        return chosen == null ? null : chosen.toPath();
    }

    private List<ExportItem> buildPageExportItems(SpmEntry entry, boolean chipMode) {
        String spmBaseName = SpmExportSupport.sanitizeFileComponent(
                SpmExportSupport.stripExtension(entry.getPath().getFileName().toString()));
        List<Image> images = List.copyOf(canvasController.getLoadedImages());
        List<Spm.SPMPageData> pages = Optional.ofNullable(entry.getSpm().getPageData()).orElse(List.of());
        List<ExportItem> items = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Spm.SPMPageData page = pages.get(pageIndex);
            if (chipMode) {
                List<Spm.SPMChipData> chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
                for (int chipIndex = 0; chipIndex < chips.size(); chipIndex++) {
                    Spm.SPMChipData chip = chips.get(chipIndex);
                    SpmExportSupport.IntRect chipBounds = SpmExportSupport.computeChipBounds(chip);
                    if (chipBounds == null) {
                        continue;
                    }
                    String fileName = String.format("%s_page_%03d_chip_%03d_%dx%d.png",
                            spmBaseName, pageIndex, chipIndex, chipBounds.width(), chipBounds.height());
                    items.add(new ExportItem(fileName, () -> SpmExportSupport.renderChipImage(chip, images)));
                }
            } else {
                SpmExportSupport.IntRect pageBounds = SpmExportSupport.computePageBounds(page);
                if (pageBounds == null) {
                    continue;
                }
                String fileName = String.format("%s_page_%03d_%dx%d.png",
                        spmBaseName, pageIndex, pageBounds.width(), pageBounds.height());
                items.add(new ExportItem(fileName, () -> SpmExportSupport.renderPageImage(page, images, pageBounds)));
            }
        }
        return items;
    }

    private List<ExportItem> buildAnimationExportItems(SpmEntry entry, boolean chipMode) {
        String spmBaseName = SpmExportSupport.sanitizeFileComponent(
                SpmExportSupport.stripExtension(entry.getPath().getFileName().toString()));
        List<Image> images = List.copyOf(canvasController.getLoadedImages());
        List<Spm.SPMPageData> pages = Optional.ofNullable(entry.getSpm().getPageData()).orElse(List.of());
        List<SpmExportSupport.AnimationSlotRef> slots = SpmExportSupport.collectAnimationSlots(entry.getSpm());
        List<ExportItem> items = new ArrayList<>();
        for (SpmExportSupport.AnimationSlotRef slot : slots) {
            if (slot.pageIndex() < 0 || slot.pageIndex() >= pages.size()) {
                continue;
            }
            Spm.SPMPageData page = pages.get(slot.pageIndex());
            if (chipMode) {
                List<Spm.SPMChipData> chips = Optional.ofNullable(page.getChipData()).orElse(List.of());
                for (int chipIndex = 0; chipIndex < chips.size(); chipIndex++) {
                    Spm.SPMChipData chip = chips.get(chipIndex);
                    SpmExportSupport.IntRect chipBounds = SpmExportSupport.computeChipBounds(chip);
                    if (chipBounds == null) {
                        continue;
                    }
                    String fileName = String.format("%s_anim_%03d_pat_%03d_slot_%03d_page_%03d_chip_%03d_%dx%d.png",
                            spmBaseName, slot.animIndex(), slot.patIndex(), slot.slotIndex(),
                            slot.pageIndex(), chipIndex, chipBounds.width(), chipBounds.height());
                    items.add(new ExportItem(fileName, () -> SpmExportSupport.renderChipImage(chip, images)));
                }
            } else {
                SpmExportSupport.IntRect pageBounds = SpmExportSupport.computePageBounds(page);
                if (pageBounds == null) {
                    continue;
                }
                String fileName = String.format("%s_anim_%03d_pat_%03d_slot_%03d_page_%03d_%dx%d.png",
                        spmBaseName, slot.animIndex(), slot.patIndex(), slot.slotIndex(),
                        slot.pageIndex(), pageBounds.width(), pageBounds.height());
                items.add(new ExportItem(fileName, () -> SpmExportSupport.renderPageImage(page, images, pageBounds)));
            }
        }
        return items;
    }

    private void startBatchExport(List<ExportItem> items, Path targetDir, String progressMessage) {
        if (exportInProgress) {
            return;
        }
        if (items.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Nothing to export.").showAndWait();
            return;
        }

        Task<ExportSummary> exportTask = new Task<>() {
            @Override
            protected ExportSummary call() throws Exception {
                Files.createDirectories(targetDir);
                int exported = 0;
                int skipped = 0;
                int total = items.size();
                updateProgress(0, Math.max(1, total));
                updateMessage(progressMessage);
                for (int i = 0; i < total; i++) {
                    ExportItem item = items.get(i);
                    updateMessage(progressMessage + " " + (i + 1) + "/" + total);
                    WritableImage image = runOnFxThread(item.renderer());
                    if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                        skipped++;
                    } else {
                        writePng(image, targetDir.resolve(item.fileName()));
                        exported++;
                    }
                    updateProgress(i + 1, total);
                }
                return new ExportSummary(exported, skipped, total, targetDir);
            }
        };

        beginBusyState(exportTask);
        exportTask.setOnSucceeded(event -> {
            ExportSummary summary = exportTask.getValue();
            Settings.setLastExportDir(summary.targetDir());
            endBusyState();
            setStatusText(String.format("Exported %d of %d files to %s",
                    summary.exported(), summary.total(), summary.targetDir().getFileName()));
        });
        exportTask.setOnFailed(event -> {
            Throwable ex = exportTask.getException();
            log.error("batch export failed", ex);
            endBusyState();
            new Alert(Alert.AlertType.ERROR, "Export failed: " + (ex == null ? "unknown error" : ex.getMessage())).showAndWait();
        });

        Thread exportThread = new Thread(exportTask, "spm-export");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void beginBusyState(Task<?> task) {
        exportInProgress = true;
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        if (busyStatusListener != null && currentBusyTask != null) {
            currentBusyTask.messageProperty().removeListener(busyStatusListener);
        }
        currentBusyTask = task;
        busyStatusListener = (obs, oldVal, newVal) -> setStatusText(newVal == null ? "" : newVal);
        task.messageProperty().addListener(busyStatusListener);
        setStatusText(task.getMessage() == null ? "" : task.getMessage());
        progressBar.setVisible(true);
        setMainUiDisabled(true);
    }

    private void endBusyState() {
        exportInProgress = false;
        if (currentBusyTask != null && busyStatusListener != null) {
            currentBusyTask.messageProperty().removeListener(busyStatusListener);
        }
        currentBusyTask = null;
        busyStatusListener = null;
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        setMainUiDisabled(false);
    }

    private void setMainUiDisabled(boolean disabled) {
        if (canvasHolder.getScene() == null || canvasHolder.getScene().getRoot() == null) {
            return;
        }
        Parent root = canvasHolder.getScene().getRoot();
        if (root instanceof BorderPane borderPane) {
            if (borderPane.getTop() != null) {
                borderPane.getTop().setDisable(disabled);
            }
            if (borderPane.getCenter() != null) {
                borderPane.getCenter().setDisable(disabled);
            }
        }
        progressBar.setDisable(false);
        statusLabel.setDisable(false);
    }

    /**
     * 将ScrollPane的滚动条设置到中心位置 (0.5, 0.5)。
     */
    private void centerScrollPane() {
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    private <T> T runOnFxThread(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get();
    }

    private void setStatusText(String text) {
        if (statusLabel.textProperty().isBound()) {
            statusLabel.textProperty().unbind();
        }
        statusLabel.setText(text);
    }

    @FXML private void onOpenDirectory() {
        spmFileHandler.openSpmDirectory(canvasHolder.getScene().getWindow());
    }

    @FXML private void onShowCharsetSettings() {
        CharsetDialog.showDialog(canvasHolder.getScene().getWindow());
    }

    @FXML private void onExportPng() {
        if (!hasLoadedSpm()) {
            new Alert(Alert.AlertType.WARNING, "No SPM selected to export.").showAndWait();
            return;
        }
        var fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName(buildCurrentExportFileName());
        Path lastExport = Settings.getLastExportDir();
        if (lastExport != null && lastExport.toFile().isDirectory()) {
            fc.setInitialDirectory(lastExport.toFile());
        }
        File out = fc.showSaveDialog(canvasHolder.getScene().getWindow());
        if (out == null) return;

        try {
            WritableImage snapshot = captureCurrentViewForExport();
            if (snapshot == null) {
                new Alert(Alert.AlertType.WARNING, "Nothing to export for the current view.").showAndWait();
                return;
            }
            writePng(snapshot, out.toPath());
            Settings.setLastExportDir(out.toPath().getParent());
            setStatusText("Exported: " + out.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
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

    private WritableImage captureCurrentViewForExport() {
        int imageIndex = getSelectedImageIndex();
        if (imageIndex >= 0) {
            return captureImagePreviewExport(imageIndex);
        }
        return capturePageExport();
    }

    private WritableImage capturePageExport() {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        int pageIndex = canvasController.getCurrentPageIndex();
        if (selected == null || selected.getSpm() == null || pageIndex < 0) {
            return null;
        }
        List<Spm.SPMPageData> pages = Optional.ofNullable(selected.getSpm().getPageData()).orElse(List.of());
        if (pageIndex >= pages.size()) {
            return null;
        }
        Spm.SPMPageData page = pages.get(pageIndex);
        SpmExportSupport.IntRect pageBounds = SpmExportSupport.computePageBounds(page);
        if (pageBounds == null) {
            return null;
        }
        return SpmExportSupport.renderPageImage(page, List.copyOf(canvasController.getLoadedImages()), pageBounds);
    }

    private WritableImage captureImagePreviewExport(int imageIndex) {
        if (imageIndex < 0 || imageIndex >= canvasController.getLoadedImages().size()) {
            return null;
        }
        Image image = canvasController.getLoadedImages().get(imageIndex);
        if (image == null) {
            return null;
        }
        WritableImage out = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.drawImage(image, 0, 0);
        if (showChipBoundsCheck.isSelected() || showPageBoundsCheck.isSelected()) {
            drawImageBoundsOverlay(g, imageIndex, 0.0, 0.0);
        }
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, out);
    }

    private String buildCurrentExportFileName() {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        String spmBaseName = selected == null
                ? "spm"
                : SpmExportSupport.sanitizeFileComponent(
                SpmExportSupport.stripExtension(selected.getPath().getFileName().toString()));

        int imageIndex = getSelectedImageIndex();
        if (imageIndex >= 0) {
            Image image = imageIndex < canvasController.getLoadedImages().size()
                    ? canvasController.getLoadedImages().get(imageIndex)
                    : null;
            int width = image == null ? 0 : (int) image.getWidth();
            int height = image == null ? 0 : (int) image.getHeight();
            return String.format("%s_image_%03d_%dx%d.png", spmBaseName, imageIndex, width, height);
        }

        int pageIndex = canvasController.getCurrentPageIndex();
        if (currentExportContext != null && currentExportContext.mode() == ExportContextMode.ANIMATION
                && currentExportContext.animationFrame() != null) {
            AnimationFrame frame = currentExportContext.animationFrame();
            SpmExportSupport.IntRect bounds = currentPageBoundsForNaming();
            int width = bounds == null ? 0 : bounds.width();
            int height = bounds == null ? 0 : bounds.height();
            return String.format("%s_anim_%03d_pat_%03d_slot_%03d_page_%03d_%dx%d.png",
                    spmBaseName, parseAnimIndex(animSelector.getValue()), frame.patIndex(), frame.slotIndex(),
                    pageIndex, width, height);
        }

        SpmExportSupport.IntRect bounds = currentPageBoundsForNaming();
        int width = bounds == null ? 0 : bounds.width();
        int height = bounds == null ? 0 : bounds.height();
        return String.format("%s_page_%03d_%dx%d.png", spmBaseName, Math.max(pageIndex, 0), width, height);
    }

    private SpmExportSupport.IntRect currentPageBoundsForNaming() {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        int pageIndex = canvasController.getCurrentPageIndex();
        if (selected == null || selected.getSpm() == null || pageIndex < 0) {
            return null;
        }
        List<Spm.SPMPageData> pages = Optional.ofNullable(selected.getSpm().getPageData()).orElse(List.of());
        if (pageIndex >= pages.size()) {
            return null;
        }
        return SpmExportSupport.computePageBounds(pages.get(pageIndex));
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
        currentExportContext = ExportContext.animation(frame);
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

    private void exportImageWithBounds(boolean chipMode) {
        SpmEntry selected = spmListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getSpm() == null || imageList.getSelectionModel().isEmpty()) return;
        int imgIdx = getSelectedImageIndex();
        if (imgIdx < 0) return;
        List<Image> imgs = List.copyOf(canvasController.getLoadedImages());
        WritableImage out = SpmExportSupport.renderImageBoundsOverlay(selected.getSpm(), imgs, imgIdx, chipMode);
        if (out == null) {
            new Alert(Alert.AlertType.INFORMATION, "Nothing to export.").showAndWait();
            return;
        }

        String baseName = buildImageExportBaseName(selected.getSpm(), imgIdx);
        String suffix = chipMode ? "_chip_bounds" : "_page_bounds";

        FileChooser chooser = new FileChooser();
        chooser.setTitle(chipMode ? "Export Chip Bounds Only" : "Export Page Bounds Only");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        chooser.setInitialFileName(baseName + suffix + "_" + (int) out.getWidth() + "x" + (int) out.getHeight() + ".png");
        Path lastExport = Settings.getLastExportDir();
        if (lastExport != null && lastExport.toFile().isDirectory()) {
            chooser.setInitialDirectory(lastExport.toFile());
        }
        File target = chooser.showSaveDialog(canvasHolder.getScene().getWindow());
        if (target == null) return;
        try {
            writePng(out, target.toPath());
            setStatusText("Exported bounds: " + target.getName());
            Settings.setLastExportDir(target.toPath().getParent());
        } catch (Exception ex) {
            log.error("export bounds failed", ex);
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    private String buildImageExportBaseName(Spm spm, int imageIndex) {
        List<Spm.SPMImageData> images = spm == null ? List.of() : Optional.ofNullable(spm.getImageData()).orElse(List.of());
        if (imageIndex >= 0 && imageIndex < images.size()) {
            Spm.SPMImageData imageData = images.get(imageIndex);
            String imageName = imageData == null ? null : imageData.getImageName();
            if (imageName != null && !imageName.isBlank()) {
                return SpmExportSupport.sanitizeFileComponent(SpmExportSupport.stripExtension(imageName));
            }
        }
        return "image-" + imageIndex;
    }

    private void overlayImageBounds(int imgIdx) {
        if (imgIdx < 0) return;
        SpmEntry selectedEntry = spmListView.getSelectionModel().getSelectedItem();
        if (selectedEntry == null || selectedEntry.getSpm() == null) {
            return;
        }
        List<Image> imgs = canvasController.getLoadedImages();
        if (imgIdx < 0 || imgIdx >= imgs.size()) return;
        Image img = imgs.get(imgIdx);
        if (img == null) return;
        double imgW = img.getWidth();
        double imgH = img.getHeight();

        // locate where image was drawn in previewImage
        double canvasW = canvasController.getCanvas().getWidth();
        double canvasH = canvasController.getCanvas().getHeight();
        double offsetX = (canvasW - imgW) / 2.0;
        double offsetY = (canvasH - imgH) / 2.0;

        GraphicsContext g = canvasController.getCanvas().getGraphicsContext2D();
        g.save();
        drawImageBoundsOverlay(g, imgIdx, offsetX, offsetY);
        g.restore();
    }

    private void drawImageBoundsOverlay(GraphicsContext g, int imgIdx, double offsetX, double offsetY) {
        SpmEntry selectedEntry = spmListView.getSelectionModel().getSelectedItem();
        if (selectedEntry == null || selectedEntry.getSpm() == null) {
            return;
        }
        if (showChipBoundsCheck.isSelected()) {
            g.setStroke(Color.CYAN);
            g.setFill(Color.color(0, 1, 1, 0.15));
            g.setLineWidth(2.0);
            for (Spm.SPMPageData page : Optional.ofNullable(selectedEntry.getSpm().getPageData()).orElse(List.of())) {
                for (Spm.SPMChipData chip : Optional.ofNullable(page.getChipData()).orElse(List.of())) {
                    if (chip.getImageNo() == null || !chip.getImageNo().equals(imgIdx)) {
                        continue;
                    }
                    Spm.SPMRect src = chip.getSrcRect();
                    if (src == null) {
                        continue;
                    }
                    double x = offsetX + safeInt(src.getLeft());
                    double y = offsetY + safeInt(src.getTop());
                    double w = safeInt(src.getRight()) - safeInt(src.getLeft());
                    double h = safeInt(src.getBottom()) - safeInt(src.getTop());
                    g.fillRect(x, y, w, h);
                    g.strokeRect(x, y, w, h);
                }
            }
        }
        if (showPageBoundsCheck.isSelected()) {
            g.setStroke(Color.ORANGE);
            g.setLineWidth(1.5);
            List<Spm.SPMPageData> pages = Optional.ofNullable(selectedEntry.getSpm().getPageData()).orElse(List.of());
            for (Spm.SPMPageData page : pages) {
                double minX = Double.POSITIVE_INFINITY;
                double minY = Double.POSITIVE_INFINITY;
                double maxX = Double.NEGATIVE_INFINITY;
                double maxY = Double.NEGATIVE_INFINITY;
                boolean has = false;
                for (Spm.SPMChipData chip : Optional.ofNullable(page.getChipData()).orElse(List.of())) {
                    if (chip.getImageNo() == null || !chip.getImageNo().equals(imgIdx)) {
                        continue;
                    }
                    Spm.SPMRect src = chip.getSrcRect();
                    if (src == null) {
                        continue;
                    }
                    double x1 = safeInt(src.getLeft());
                    double y1 = safeInt(src.getTop());
                    double x2 = safeInt(src.getRight());
                    double y2 = safeInt(src.getBottom());
                    minX = Math.min(minX, Math.min(x1, x2));
                    minY = Math.min(minY, Math.min(y1, y2));
                    maxX = Math.max(maxX, Math.max(x1, x2));
                    maxY = Math.max(maxY, Math.max(y1, y2));
                    has = true;
                }
                if (has) {
                    g.strokeRect(offsetX + minX, offsetY + minY, maxX - minX, maxY - minY);
                }
            }
        }
    }

    private void refreshImagePreviewWithBounds(int idx) {
        if (idx < 0) return;
        try {
            String imageName = imageList.getSelectionModel().getSelectedItem();
            if (imageName == null) {
                return;
            }
            previewImage(idx, imageName);
            if (showChipBoundsCheck.isSelected() || showPageBoundsCheck.isSelected()) {
                overlayImageBounds(idx);
            }
        } catch (Exception ex) {
            log.debug("refresh image preview failed", ex);
        }
    }

    private int getSelectedImageIndex() {
        if (imageList.getSelectionModel().isEmpty()) return -1;
        try {
            return Integer.parseInt(imageList.getSelectionModel().getSelectedItem().split(":")[0].trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private enum ExportContextMode {
        PAGE,
        ANIMATION,
        IMAGE
    }

    private record ExportContext(ExportContextMode mode, Integer pageIndex, Integer imageIndex, AnimationFrame animationFrame) {
        static ExportContext page(int pageIndex) {
            return new ExportContext(ExportContextMode.PAGE, pageIndex, null, null);
        }

        static ExportContext image(int imageIndex) {
            return new ExportContext(ExportContextMode.IMAGE, null, imageIndex, null);
        }

        static ExportContext animation(AnimationFrame frame) {
            return new ExportContext(ExportContextMode.ANIMATION, frame.pageNo(), null, frame);
        }
    }

    private record ExportItem(String fileName, Callable<WritableImage> renderer) {
    }

    private record ExportSummary(int exported, int skipped, int total, Path targetDir) {
    }

    private record TreeSelectionContext(int animIndex, int patIndex, List<Integer> pageNos) { }
}
