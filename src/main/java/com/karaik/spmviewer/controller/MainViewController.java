package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.controller.render.SpmRenderer;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.parser.SpmParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MainViewController {

    // model
    private Spm spm;

    // image store
    private final List<Image> images = new ArrayList<>();
    private Map<Integer, String> imageNames = new HashMap<>();

    // canvas & transforms
    private final Canvas canvas = new Canvas(1024, 768);
    private final Group canvasGroup = new Group(canvas);
    private final Scale scale = new Scale(1, 1, 0, 0);

    // current page
    private int currentPageIndex = -1;

    // animation
    private Timeline animTimeline;

    // pan state
    private double dragStartX, dragStartY, hValStart, vValStart;

    // FXML binds
    @FXML private MenuItem miShowPageRect;
    @FXML private MenuItem miShowHit;
    @FXML private MenuItem miShowChipBounds;

    @FXML private ToggleButton tbShowPageRect;
    @FXML private ToggleButton tbShowHit;
    @FXML private ToggleButton tbShowChipBounds;

    @FXML private Spinner<Integer> pageSpinner;
    @FXML private Slider zoomSlider;
    @FXML private Button animPlayBtn;
    @FXML private ComboBox<String> animSelector;
    @FXML private Slider fpsSlider;

    @FXML private TreeView<String> pageTree;
    @FXML private TreeView<String> animTree;
    @FXML private ListView<String> imageList;

    @FXML private ScrollPane scrollPane;
    @FXML private StackPane canvasHolder;

    @FXML private TableView<Spm.SPMChipData> chipTable;
    @FXML private TableView<Spm.SPMHitArea> hitTable;

    @FXML private Label lblVersion, lblPages, lblImages, lblAnims;
    @FXML private Label statusLabel;

    // renderer
    private final SpmRenderer renderer = new SpmRenderer();

    // initialize
    @FXML
    public void initialize() {
        canvas.getTransforms().add(scale);
        canvasHolder.getChildren().add(canvasGroup);

        setupTables();
        setupBindings();
        setupInteractions();

        tbShowPageRect.setSelected(true);
        tbShowHit.setSelected(true);
        tbShowChipBounds.setSelected(false);
    }

    // table setup
    private void setupTables() {
        {
            TableColumn<Spm.SPMChipData, String> cImg = new TableColumn<>("img#");
            cImg.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    String.valueOf(Optional.ofNullable(cd.getValue().getImageNo()).orElse(-1))));

            TableColumn<Spm.SPMChipData, String> cDst = new TableColumn<>("dstRect");
            cDst.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    renderer.rectToStr(cd.getValue().getDstRect())));

            TableColumn<Spm.SPMChipData, String> cSrc = new TableColumn<>("srcRect");
            cSrc.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    renderer.rectToStr(cd.getValue().getSrcRect())));

            TableColumn<Spm.SPMChipData, String> cSize = new TableColumn<>("size");
            cSize.setCellValueFactory(cd -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    cd.getValue().getChipWidth() + "×" + cd.getValue().getChipHeight()));

            chipTable.getColumns().setAll(cImg, cDst, cSrc, cSize);
        }
        {
            TableColumn<Spm.SPMHitArea, String> cRect = new TableColumn<>("hitRect");
            cRect.setCellValueFactory(h -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    renderer.rectToStr(h.getValue().getHitRect())));

            TableColumn<Spm.SPMHitArea, String> cU0 = new TableColumn<>("unk0");
            cU0.setCellValueFactory(h -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    String.valueOf(h.getValue().getUnk0())));

            TableColumn<Spm.SPMHitArea, String> cU1 = new TableColumn<>("unk1");
            cU1.setCellValueFactory(h -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    String.valueOf(h.getValue().getUnk1())));

            TableColumn<Spm.SPMHitArea, String> cU2 = new TableColumn<>("unk2");
            cU2.setCellValueFactory(h -> javafx.beans.binding.Bindings.createStringBinding(() ->
                    String.valueOf(h.getValue().getUnk2())));

            hitTable.getColumns().setAll(cRect, cU0, cU1, cU2);
        }
    }

    // bindings & listeners
    private void setupBindings() {
        zoomSlider.valueProperty().addListener((o, ov, nv) -> {
            scale.setX(nv.doubleValue());
            scale.setY(nv.doubleValue());
            status("Zoom: " + Math.round(nv.doubleValue() * 100) + "%");
        });

        if (miShowPageRect instanceof CheckMenuItem cmPage) {
            cmPage.selectedProperty().bindBidirectional(tbShowPageRect.selectedProperty());
        }
        if (miShowHit instanceof CheckMenuItem cmHit) {
            cmHit.selectedProperty().bindBidirectional(tbShowHit.selectedProperty());
        }
        if (miShowChipBounds instanceof CheckMenuItem cmChip) {
            cmChip.selectedProperty().bindBidirectional(tbShowChipBounds.selectedProperty());
        }

        tbShowPageRect.setOnAction(e -> renderCurrent());
        tbShowHit.setOnAction(e -> renderCurrent());
        tbShowChipBounds.setOnAction(e -> renderCurrent());

        pageSpinner.valueProperty().addListener((o, ov, nv) -> selectPage(nv));

        pageTree.getSelectionModel().selectedItemProperty().addListener((o, ov, it) -> {
            if (it == null || it.getParent() == null) return;
            try {
                int idx = Integer.parseInt(it.getValue().split(" ")[0]);
                selectPage(idx);
            } catch (Exception ignored) {}
        });

        animTree.getSelectionModel().selectedItemProperty().addListener((o, ov, it) -> {
            if (it == null || it.getParent() == null) return;
            selectAnim(it.getValue());
        });

        animSelector.setOnAction(e -> selectAnim(animSelector.getValue()));
    }

    // interactions
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
                scrollPane.setHvalue(clamp((int) (hValStart - (e.getSceneX() - dragStartX) / w), 0, 1));
                scrollPane.setVvalue(clamp((int) (vValStart - (e.getSceneY() - dragStartY) / h), 0, 1));
                e.consume();
            }
        });

        canvasHolder.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DIGIT1) onZoom1x();
            if (e.getCode() == KeyCode.EQUALS || e.getCode() == KeyCode.PLUS) zoomSlider.setValue(zoomSlider.getValue() * 1.1);
            if (e.getCode() == KeyCode.MINUS) zoomSlider.setValue(zoomSlider.getValue() / 1.1);
            if (e.getCode() == KeyCode.SPACE) {
                if (animTimeline != null && animTimeline.getStatus() == Timeline.Status.RUNNING) onStop();
                else onPlay();
            }
        });
        canvasHolder.setFocusTraversable(true);
    }

    // action: open spm
    @FXML
    private void onOpenSpm() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("SPM Files", "*.spm"), new FileChooser.ExtensionFilter("All Files", "*.*"));
        File f = fc.showOpenDialog(canvasHolder.getScene().getWindow());
        if (f == null) return;

        try {
            byte[] data = Files.readAllBytes(f.toPath());
            SpmParser parser = new SpmParser();
            this.spm = parser.parse(data, f.getName(), "windows-31j");
            fillUiAfterSpmLoad();
            status("Loaded SPM: " + f.getName());
        } catch (Exception ex) {
            showError("Failed to load SPM", ex);
        }
    }

    // action: open images
    @FXML
    private void onOpenImages() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Pick the folder containing SPM images");
        File dir = dc.showDialog(canvasHolder.getScene().getWindow());
        if (dir == null) return;

        try {
            loadImages(dir.toPath());
            renderCurrent();
            status("Images loaded: " + dir.getName());
        } catch (Exception ex) {
            showError("Failed to load images", ex);
        }
    }

    // action: export
    @FXML
    private void onExportPng() {
        if (spm == null || currentPageIndex < 0) {
            status("Open an SPM first");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("spm-page-" + currentPageIndex + ".png");
        File out = fc.showSaveDialog(canvasHolder.getScene().getWindow());
        if (out == null) return;
        try {
            var snapshot = canvas.snapshot(new SnapshotParameters(), null);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null), "png", out);
            status("Exported: " + out.getName());
        } catch (Exception ex) {
            showError("Export failed", ex);
        }
    }

    // action: quit
    @FXML
    private void onQuit() {
        canvasHolder.getScene().getWindow().hide();
    }

    // action: about
    @FXML
    private void onAbout() {
        new Alert(Alert.AlertType.INFORMATION, "SPM Viewer\nJavaFX 17+, binary SPM + image folder.").showAndWait();
    }

    // action: zoom 1x
    @FXML
    private void onZoom1x() {
        zoomSlider.setValue(1.0);
    }

    // action: fit
    @FXML
    private void onFit() {
        if (spm == null || currentPageIndex < 0) return;
        var p = spm.getPageData().get(currentPageIndex);
        var bounds = scrollPane.getViewportBounds();
        double vw = bounds.getWidth() - 32;
        double vh = bounds.getHeight() - 32;
        double zx = vw / safe(p.getPageWidth());
        double zy = vh / safe(p.getPageHeight());
        zoomSlider.setValue(Math.max(0.1, Math.min(8.0, Math.min(zx, zy))));
    }

    // action: play
    @FXML
    private void onPlay() {
        if (spm == null) return;
        String name = animSelector.getValue();
        if (name == null) return;
        stopAnim();
        var anim = spm.getAnimData().stream().filter(a -> name.equals(a.getAnimName())).findFirst().orElse(null);
        if (anim == null) return;

        List<Integer> framePages = new ArrayList<>();
        List<Integer> frameDurMs = new ArrayList<>();
        int baseMs = (int) (1000.0 / fpsSlider.getValue());

        if (anim.getPatData() != null) {
            for (Spm.SPMPatData pat : anim.getPatData()) {
                int wf = Optional.ofNullable(pat.getWaitFrame()).orElse(1);
                var pages = Optional.ofNullable(pat.getPageNo()).orElse(List.of());
                for (Integer pi : pages) {
                    framePages.add(pi);
                    frameDurMs.add(Math.max(1, wf) * baseMs);
                }
            }
        }
        if (framePages.isEmpty()) return;

        animTimeline = new Timeline();
        int t = 1;
        for (int i = 0; i < framePages.size(); i++) {
            int pageIdx = framePages.get(i);
            int dur = frameDurMs.get(i);
            t += dur;
            animTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(t), e -> selectPage(pageIdx)));
        }
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        animTimeline.play();
        status("Playing anim: " + name);
    }

    // action: stop
    @FXML
    private void onStop() {
        stopAnim();
    }

    // ui fill after loading spm
    private void fillUiAfterSpmLoad() {
        lblVersion.setText(spm.getSpmVersion());
        lblPages.setText(String.valueOf(safe(spm.getNumPageData())));
        lblImages.setText(String.valueOf(safe(spm.getNumImageData())));
        lblAnims.setText(String.valueOf(safe(spm.getNumAnimData())));

        if (spm.getImageData() != null) {
            imageNames.clear();
            for (int i = 0; i < spm.getImageData().size(); i++) {
                String name = Optional.ofNullable(spm.getImageData().get(i).getImageName()).orElse("<image " + i + ">");
                imageNames.put(i, name);
            }
            imageList.getItems().setAll(imageNames.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList());
        }

        var rootPages = new TreeItem<>("Pages");
        pageTree.setRoot(rootPages);
        rootPages.setExpanded(true);
        var pages = Optional.ofNullable(spm.getPageData()).orElse(List.of());
        for (int i = 0; i < pages.size(); i++) {
            var p = pages.get(i);
            String label = i + " (" + p.getPageWidth() + "×" + p.getPageHeight() + ") chips=" + Optional.ofNullable(p.getChipData()).map(List::size).orElse(0);
            rootPages.getChildren().add(new TreeItem<>(label));
        }

        var rootAnims = new TreeItem<>("Animations");
        animTree.setRoot(rootAnims);
        rootAnims.setExpanded(true);
        animSelector.getItems().clear();
        var anims = Optional.ofNullable(spm.getAnimData()).orElse(List.of());
        for (var a : anims) {
            var ai = new TreeItem<>(a.getAnimName());
            if (a.getPatData() != null) {
                for (int i = 0; i < a.getPatData().size(); i++) {
                    var pat = a.getPatData().get(i);
                    ai.getChildren().add(new TreeItem<>("pat[" + i + "] wf=" + pat.getWaitFrame() + " pages=" + Optional.ofNullable(pat.getPageNo()).orElse(List.of())));
                }
            }
            rootAnims.getChildren().add(ai);
            animSelector.getItems().add(a.getAnimName());
        }
        if (!animSelector.getItems().isEmpty()) animSelector.getSelectionModel().selectFirst();

        int maxPage = Math.max(0, safe(spm.getNumPageData()) - 1);
        pageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxPage, 0));
        selectPage(0);
    }

    // select page
    private void selectPage(int index) {
        if (spm == null) return;
        var pages = spm.getPageData();
        if (pages == null || pages.isEmpty()) return;
        index = clamp(index, 0, pages.size() - 1);
        currentPageIndex = index;
        pageSpinner.getValueFactory().setValue(index);

        chipTable.getItems().setAll(Optional.ofNullable(pages.get(index).getChipData()).orElse(List.of()));
        hitTable.getItems().setAll(Optional.ofNullable(pages.get(index).getHitRects()).orElse(List.of()));

        renderCurrent();
        status("Page " + index + " rendered");
    }

    // select animation
    private void selectAnim(String name) {
        if (name == null) return;
        onStop();
        onPlay();
    }

    // render
    private void renderCurrent() {
        if (spm == null || currentPageIndex < 0) return;
        var p = spm.getPageData().get(currentPageIndex);

        int w = Math.max(1, safe(p.getPageWidth()));
        int h = Math.max(1, safe(p.getPageHeight()));

        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }

        boolean showPage = tbShowPageRect.isSelected();
        boolean showHit = tbShowHit.isSelected();
        boolean showChip = tbShowChipBounds.isSelected();

        renderer.render(canvas, spm, currentPageIndex, images, showPage, showHit, showChip);
    }

    // load images
    private void loadImages(Path dir) {
        images.clear();
        if (spm == null || spm.getImageData() == null) return;
        for (int i = 0; i < spm.getImageData().size(); i++) {
            String name = Optional.ofNullable(spm.getImageData().get(i).getImageName()).orElse(i + ".png");
            Path p = dir.resolve(name);
            if (!Files.exists(p)) p = dir.resolve(i + ".png");
            try {
                images.add(new Image(new FileInputStream(p.toFile())));
            } catch (Exception e) {
                images.add(null);
            }
        }
    }

    // utils
    private void stopAnim() {
        if (animTimeline != null) animTimeline.stop();
        animTimeline = null;
        status("Animation stopped");
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int safe(Integer i) { return i == null ? 0 : i; }

    private void status(String s) { statusLabel.setText(s); }

    private void showError(String msg, Exception ex) {
        ex.printStackTrace();
        new Alert(Alert.AlertType.ERROR, msg + "\n" + ex.getMessage()).showAndWait();
    }
}
