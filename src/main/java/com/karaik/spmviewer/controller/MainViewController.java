package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainViewController {

    // --- FXML Binds ---
    // MODIFIED: Changed MenuItem to CheckMenuItem to match the FXML definition
    @FXML private CheckMenuItem miShowPageRect, miShowHit, miShowChipBounds;
    @FXML private ToggleButton tbShowPageRect, tbShowHit, tbShowChipBounds;
    @FXML private Spinner<Integer> pageSpinner;
    @FXML private Slider zoomSlider;
    @FXML private ComboBox<String> animSelector;
    @FXML private Slider fpsSlider;
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

    // --- Controllers and Services ---
    private SpmFileHandler spmFileHandler;
    private UiStateController uiStateController;
    private CanvasController canvasController;

    private Timeline animTimeline;
    private double dragStartX, dragStartY, hValStart, vValStart;

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
        spmFileHandler = new SpmFileHandler(spmListView, progressBar, statusLabel);
        uiStateController = new UiStateController(pageTree, animTree, imageList, animSelector,
                lblVersion, lblPages, lblImages, lblAnims, chipTable, hitTable);

        // --- Setup UI ---
        setupSpmListCellFactory();
        setupBindingsAndListeners();
        setupInteractions();

        // Initial state
        uiStateController.clearAllPanels();
        canvasController.clearCanvas();
        pageSpinner.setDisable(true);
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
        // View toggles
        tbShowPageRect.selectedProperty().bindBidirectional(miShowPageRect.selectedProperty());
        tbShowHit.selectedProperty().bindBidirectional(miShowHit.selectedProperty());
        tbShowChipBounds.selectedProperty().bindBidirectional(miShowChipBounds.selectedProperty());
        tbShowPageRect.setOnAction(e -> renderCurrentPage());
        tbShowHit.setOnAction(e -> renderCurrentPage());
        tbShowChipBounds.setOnAction(e -> renderCurrentPage());

        // SPM List selection
        spmListView.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null && entry.getStatus() == SpmEntry.Status.SUCCESS) {
                selectSpm(entry);
            }
        });

        // Page/Anim/Image selection
        pageSpinner.valueProperty().addListener((o, ov, nv) -> selectPage(nv));
        pageTree.getSelectionModel().selectedItemProperty().addListener((o, ov, item) -> {
            if (item != null && item.getParent() != null) {
                try {
                    selectPage(Integer.parseInt(item.getValue().split(" ")[0]));
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
    }

    // --- Selection and State Change Logic ---

    private void selectSpm(SpmEntry entry) {
        onStop();
        canvasController.setCurrentSpm(entry.getSpm(), spmFileHandler.getCurrentDirectory());
        uiStateController.updateUiForSpm(entry.getSpm());

        int pageCount = Optional.ofNullable(entry.getSpm().getNumPageData()).orElse(0);
        if (pageCount > 0) {
            pageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, pageCount - 1, 0));
            pageSpinner.setDisable(false);
            selectPage(0);
        } else {
            pageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, -1, -1));
            pageSpinner.setDisable(true);
            uiStateController.updateTablesForPage(null);
            canvasController.clearCanvas();
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

        if (!pageSpinner.getValue().equals(index)) {
            pageSpinner.getValueFactory().setValue(index);
        }
    }

    private void previewImage(int index, String imageName) {
        onStop();
        pageTree.getSelectionModel().clearSelection();
        pageSpinner.setDisable(true);
        uiStateController.updateTablesForPage(null);
        canvasController.previewImage(index, imageName);
    }

    private void renderCurrentPage() {
        canvasController.renderPage(tbShowPageRect, tbShowHit, tbShowChipBounds);
    }

    // --- FXML Action Handlers ---

    @FXML private void onOpenDirectory() {
        spmFileHandler.openSpmDirectory(canvasHolder.getScene().getWindow());
    }

    @FXML private void onExportPng() {
        // This logic remains simple and can stay here
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
        canvasHolder.getScene().getWindow().hide();
    }

    @FXML private void onAbout() {
        new Alert(Alert.AlertType.INFORMATION, "NeXAS Sprite Viewer\nMajor refactoring complete.").showAndWait();
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

        String animName = animSelector.getValue();
        if (animName == null) return;

        onStop();

        var anim = selectedSpm.getSpm().getAnimData().stream()
                .filter(a -> animName.equals(a.getAnimName())).findFirst().orElse(null);
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