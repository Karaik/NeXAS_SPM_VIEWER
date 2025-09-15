package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainViewController {

    // --- FXML Binds ---
    @FXML private TextField searchField;
    @FXML private Slider zoomSlider;
    @FXML private TextField zoomField;
    @FXML private ComboBox<String> animSelector;
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
    // MODIFIED: Added FXML fields for animation search
    @FXML private TitledPane animTitledPane;
    @FXML private TextField animSearchField;

    private SpmFileHandler spmFileHandler;
    private UiStateController uiStateController;
    private CanvasController canvasController;

    private Timeline animTimeline;
    private double dragStartX, dragStartY, hValStart, vValStart;

    private ObservableList<SpmEntry> masterSpmList;
    private SpmEntry currentlyLoadingSpm = null;

    @FXML
    public void initialize() {
        // --- Canvas and Scaling Setup ---
        final Scale scale = new Scale(1, 1, 0, 0);
        final Group canvasGroup = new Group();
        canvasController = new CanvasController(new javafx.scene.canvas.Canvas(), canvasGroup, scale);
        canvasHolder.getChildren().add(canvasGroup);
        zoomSlider.valueProperty().addListener((o, ov, nv) -> {
            scale.setX(nv.doubleValue());
            scale.setY(nv.doubleValue());
        });

        // --- Initialize Controllers ---
        masterSpmList = FXCollections.observableArrayList();
        spmFileHandler = new SpmFileHandler(masterSpmList, progressBar, statusLabel);

        uiStateController = new UiStateController(pageTree, animTree, imageList, animSelector,
                lblVersion, lblPages, lblImages, lblAnims, chipTable, hitTable);

        // --- Setup UI ---
        setupSpmListCellFactory();
        setupBindingsAndListeners();
        setupInteractions();
        setupValueBindings();
        setupFiltering();
        setupExpandAllContextMenus();
        // MODIFIED: Setup animation filtering listener
        setupAnimFiltering();

        // Initial state
        uiStateController.clearAllPanels();
        canvasController.clearCanvas();

        loadLastDirectory();
    }

    // --- Setup Methods ---

    private void setupSpmListCellFactory() {
        spmListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SpmEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                    setDisable(false);
                } else {
                    setText(item.toString());
                    if (item.getStatus() == SpmEntry.Status.FAILED) {
                        setTextFill(Color.RED);
                        setTooltip(new Tooltip(item.getErrorMessage()));
                        setDisable(true);
                    } else {
                        setTextFill(Color.BLACK);
                        setTooltip(null);
                        setDisable(false);
                    }
                }
            }
        });
    }

    private void setupBindingsAndListeners() {
        spmListView.setOnMouseClicked(event -> {
            loadAndDisplaySpm(spmListView.getSelectionModel().getSelectedItem());
        });

        spmListView.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                loadAndDisplaySpm(spmListView.getSelectionModel().getSelectedItem());
            }
        });

        masterSpmList.addListener((ListChangeListener<SpmEntry>) c -> {
            if (!masterSpmList.isEmpty() && spmListView.getSelectionModel().getSelectedItem() == null) {
                if (!spmListView.getItems().isEmpty()) {
                    spmListView.getSelectionModel().selectFirst();
                    loadAndDisplaySpm(spmListView.getSelectionModel().getSelectedItem());
                }
            }
        });

        pageTree.getSelectionModel().selectedItemProperty().addListener((o, ov, item) -> {
            if (item != null && item.isLeaf()) {
                try {
                    selectPage(Integer.parseInt(item.getValue().split(" ")[0]));
                } catch (Exception ignored) {}
            }
        });

        animTree.getSelectionModel().selectedItemProperty().addListener((o, ov, item) -> {
            if (item != null && item.isLeaf()) {
                try {
                    String value = item.getValue();
                    if (value.contains("pages=[")) {
                        String pageStr = value.substring(value.indexOf('[') + 1, value.indexOf(']'));
                        if (!pageStr.isEmpty()) {
                            int pageToSelect = Integer.parseInt(pageStr.split(",")[0].trim());
                            selectPage(pageToSelect);
                            String animDisplayName = item.getParent().getValue();
                            animSelector.getSelectionModel().select(animDisplayName);
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        imageList.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                try {
                    int index = Integer.parseInt(item.split(":")[0].trim());
                    previewImage(index, item);
                } catch (Exception ignored) {}
            }
        });

        animSelector.setOnAction(e -> { if (animSelector.getValue() != null) onPlay(); });

        alwaysOnTopCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Stage stage = (Stage) canvasHolder.getScene().getWindow();
            stage.setAlwaysOnTop(newVal);
        });

        canvasHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardShortcuts(newScene);
            }
        });
    }

    private void setupInteractions() {
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
                double w = Math.max(scrollPane.getContent().getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth(), 1);
                double h = Math.max(scrollPane.getContent().getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight(), 1);
                scrollPane.setHvalue(Math.max(0, Math.min(1, hValStart - (e.getSceneX() - dragStartX) / w)));
                scrollPane.setVvalue(Math.max(0, Math.min(1, vValStart - (e.getSceneY() - dragStartY) / h)));
                e.consume();
            }
        });

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double zoomFactor = 1.1;
                double delta = event.getDeltaY();
                if (delta < 0) {
                    zoomSlider.setValue(zoomSlider.getValue() / zoomFactor);
                } else {
                    zoomSlider.setValue(zoomSlider.getValue() * zoomFactor);
                }
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

    // --- NEW FUNCTIONALITY METHODS ---

    private void setupFiltering() {
        FilteredList<SpmEntry> filteredList = new FilteredList<>(masterSpmList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(spmEntry -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return spmEntry.getPath().getFileName().toString().toLowerCase().contains(lowerCaseFilter);
            });
            if (spmListView.getSelectionModel().getSelectedItem() == null && !spmListView.getItems().isEmpty()) {
                spmListView.getSelectionModel().selectFirst();
                loadAndDisplaySpm(spmListView.getSelectionModel().getSelectedItem());
            }
        });

        spmListView.setItems(filteredList);
    }

    // MODIFIED: New method to handle animation search
    private void setupAnimFiltering() {
        animSearchField.textProperty().addListener((obs, ov, nv) -> {
            SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
            if (selectedSpm != null && selectedSpm.getSpm() != null) {
                // Re-populate the animation UI elements with the new filter
                uiStateController.updateUiForSpm(selectedSpm.getSpm(), nv);
                // Auto-expand the pane for better user experience when searching
                if (nv != null && !nv.trim().isEmpty()) {
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
        ContextMenu contextMenu = new ContextMenu();
        MenuItem expandAllItem = new MenuItem("Expand All");
        expandAllItem.setOnAction(event -> {
            if (treeView.getRoot() != null) {
                expandTreeView(treeView.getRoot());
            }
        });
        contextMenu.getItems().add(expandAllItem);
        treeView.setContextMenu(contextMenu);
    }

    private void expandTreeView(TreeItem<?> item) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(true);
            for (TreeItem<?> child : item.getChildren()) {
                expandTreeView(child);
            }
        }
    }

    // --- END NEW FUNCTIONALITY METHODS ---

    private void setupKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.PLAY || event.getCode() == KeyCode.PASTE) {
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

    private void loadAndDisplaySpm(SpmEntry entry) {
        if (entry == currentlyLoadingSpm) {
            return;
        }
        currentlyLoadingSpm = entry;

        onStop();

        if (entry == null || entry.getStatus() != SpmEntry.Status.SUCCESS) {
            uiStateController.clearAllPanels();
            canvasController.clearCanvas();
            currentlyLoadingSpm = null;
            return;
        }

        progressBar.progressProperty().unbind();

        statusLabel.setText("Loading " + entry.getPath().getFileName() + "...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        // MODIFIED: Clear the animation search field when loading a new file
        animSearchField.clear();
        uiStateController.clearAllPanels();
        canvasController.clearCanvas();

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                canvasController.setCurrentSpm(entry.getSpm(), spmFileHandler.getCurrentDirectory());
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            // MODIFIED: Pass null as the filter to indicate a full refresh
            uiStateController.updateUiForSpm(entry.getSpm(), null);

            int pageCount = Optional.ofNullable(entry.getSpm().getNumPageData()).orElse(0);
            if (pageCount > 0) {
                selectPage(0);
            } else {
                uiStateController.updateTablesForPage(null);
                canvasController.clearCanvas();
            }
            statusLabel.setText("Ready");
            progressBar.setVisible(false);
            currentlyLoadingSpm = null;
        });

        loadTask.setOnFailed(e -> {
            statusLabel.setText("Failed to load " + entry.getPath().getFileName());
            progressBar.setVisible(false);
            Throwable ex = loadTask.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            currentlyLoadingSpm = null;
        });

        new Thread(loadTask).start();
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
        canvasController.previewImage(index, imageName);
    }

    private void renderCurrentPage() {
        canvasController.renderPage();
    }

    // --- FXML Action Handlers ---

    @FXML private void onOpenDirectory() {
        spmFileHandler.openSpmDirectory(canvasHolder.getScene().getWindow());
    }

    @FXML private void onShowCharsetSettings() {
        CharsetDialog.showDialog(canvasHolder.getScene().getWindow());
    }

    @FXML private void onExportPng() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("spm-export.png");
        File out = fc.showSaveDialog(canvasHolder.getScene().getWindow());
        if (out == null) return;
        try {
            var snapshot = canvasController.getCanvas().snapshot(new SnapshotParameters(), null);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null), "png", out);
            statusLabel.setText("Exported: " + out.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML private void onQuit() {
        ((Stage) canvasHolder.getScene().getWindow()).close();
    }

    @FXML private void onShowShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(canvasHolder.getScene().getWindow());
        alert.setTitle("Shortcuts");
        alert.setHeaderText("Keyboard and Mouse Shortcuts");
        String content =
                "Play/Pause Media Key: Toggle animation playback\n\n" +
                        "Ctrl + Mouse Scroll: Zoom in/out on the canvas\n\n" +
                        "Middle Mouse Button Drag: Pan the canvas";
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void onAbout() {
        Alert aboutDialog = new Alert(Alert.AlertType.INFORMATION, "NeXAS Sprite Viewer");
        aboutDialog.initOwner(canvasHolder.getScene().getWindow());
        aboutDialog.showAndWait();
    }

    @FXML private void onZoom1x() {
        zoomSlider.setValue(1.0);
    }

    @FXML private void onFit() {
        // This also remains simple
    }

    @FXML private void onPlay() {
        SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
        if (selectedSpm == null || selectedSpm.getSpm() == null) return;

        String displayName = animSelector.getValue();
        if (displayName == null) return;

        onStop();

        Spm.SPMAnimData anim;
        try {
            // MODIFIED: Parse the index from the display name "[index] name"
            int startIndex = displayName.indexOf('[') + 1;
            int endIndex = displayName.indexOf(']');
            if (startIndex == 0 || endIndex == -1) return; // Invalid format

            int animIndex = Integer.parseInt(displayName.substring(startIndex, endIndex));
            List<Spm.SPMAnimData> animDataList = selectedSpm.getSpm().getAnimData();

            if (animIndex < 0 || animIndex >= animDataList.size()) return; // Index out of bounds
            anim = animDataList.get(animIndex);

        } catch (NumberFormatException e) {
            // Could fail if display name format is unexpectedly changed.
            return;
        }

        if (anim == null) return;

        List<KeyFrame> keyFrames = new ArrayList<>();
        int totalDuration = 0;
        int baseMs = (int) (1000.0 / fpsSlider.getValue());

        for (Spm.SPMPatData pat : Optional.ofNullable(anim.getPatData()).orElse(List.of())) {
            int waitFrames = Math.max(1, Optional.ofNullable(pat.getWaitFrame()).orElse(1));
            for (Integer pageNo : Optional.ofNullable(pat.getPageNo()).orElse(List.of())) {
                totalDuration += waitFrames * baseMs;
                keyFrames.add(new KeyFrame(Duration.millis(totalDuration), e -> selectPage(pageNo)));
            }
        }

        if (keyFrames.isEmpty()) return;

        animTimeline = new Timeline(keyFrames.toArray(new KeyFrame[0]));
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        animTimeline.play();
    }

    @FXML private void onStop() {
        if (animTimeline != null) {
            animTimeline.stop();
            animTimeline = null;
        }
    }
}