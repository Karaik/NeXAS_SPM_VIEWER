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

/**
 * 主界面视图的控制器。
 * 负责处理所有UI事件、状态管理和与后端逻辑的交互。
 */
public class MainViewController {

    //<editor-fold desc="FXML 控件注入">
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
    @FXML private TitledPane animTitledPane;
    @FXML private TextField animSearchField;
    //</editor-fold>

    // 业务逻辑控制器
    private SpmFileHandler spmFileHandler;
    private UiStateController uiStateController;
    private CanvasController canvasController;

    // 动画播放器
    private Timeline animTimeline;

    // 拖拽平移相关变量
    private double dragStartX, dragStartY, hValStart, vValStart;

    // 数据模型
    private final ObservableList<SpmEntry> masterSpmList = FXCollections.observableArrayList();

    // 状态变量，防止对同一文件重复加载
    private SpmEntry currentlyLoadingSpm = null;

    /**
     * FXML 初始化方法，在加载 FXML 文件后自动调用。
     */
    @FXML
    public void initialize() {
        // 初始化各个模块
        setupCanvas();
        setupControllers();
        setupUIComponents();

        // 加载上次使用的目录
        loadLastDirectory();
    }

    /**
     * 初始化 Canvas 和缩放相关的设置。
     */
    private void setupCanvas() {
        final Scale scale = new Scale(1, 1, 0, 0);
        final Group canvasGroup = new Group();
        canvasController = new CanvasController(new javafx.scene.canvas.Canvas(), canvasGroup, scale);
        canvasHolder.getChildren().add(canvasGroup);

        // 绑定缩放滑块与 Canvas 的缩放变换
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            scale.setX(newVal.doubleValue());
            scale.setY(newVal.doubleValue());
        });
    }

    /**
     * 初始化后端的业务逻辑控制器。
     */
    private void setupControllers() {
        spmFileHandler = new SpmFileHandler(masterSpmList, progressBar, statusLabel);
        uiStateController = new UiStateController(
                pageTree, animTree, imageList, animSelector,
                lblVersion, lblPages, lblImages, lblAnims,
                chipTable, hitTable
        );
    }

    /**
     * 初始化所有UI组件的事件监听、绑定和初始状态。
     */
    private void setupUIComponents() {
        setupSpmListCellFactory();
        setupActionListeners();
        setupCanvasInteractions();
        setupValueBindings();
        setupFiltering();
        setupExpandAllContextMenus();

        uiStateController.clearAllPanels();
        canvasController.clearCanvas();
    }

    /**
     * 设置 SPM 文件列表的单元格工厂，用于自定义显示。
     * 失败的条目会显示为红色，并有错误提示。
     */
    private void setupSpmListCellFactory() {
        spmListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SpmEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false); // 确保空单元格可交互
                } else {
                    setText(item.toString());
                    if (item.getStatus() == SpmEntry.Status.FAILED) {
                        setTextFill(Color.RED);
                        setTooltip(new Tooltip(item.getErrorMessage()));
                        setDisable(true); // 失败的条目不可选
                    } else {
                        setTextFill(Color.BLACK);
                        setTooltip(null);
                        setDisable(false);
                    }
                }
            }
        });
    }

    /**
     * 统一设置所有控件的动作监听器。
     */
    private void setupActionListeners() {
        // SPM 文件列表：点击或键盘选择时加载文件
        spmListView.setOnMouseClicked(event -> handleSpmSelection());
        spmListView.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                handleSpmSelection();
            }
        });

        // SPM 主列表数据变化监听：自动选择第一个
        masterSpmList.addListener((ListChangeListener<SpmEntry>) c -> {
            if (!masterSpmList.isEmpty() && spmListView.getSelectionModel().getSelectedItem() == null) {
                spmListView.getSelectionModel().selectFirst();
                handleSpmSelection();
            }
        });

        // Page 树状列表：选择一个 page 进行渲染
        pageTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null && item.isLeaf()) {
                try {
                    String pageIndexStr = item.getValue().split(" ")[0];
                    selectPage(Integer.parseInt(pageIndexStr));
                } catch (NumberFormatException ignored) {}
            }
        });

        // Animation 树状列表：选择动画中的一帧，会联动到 Page 和动画下拉框
        animTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null && item.isLeaf() && item.getValue().contains("pages=[")) {
                try {
                    String value = item.getValue();
                    String pageStr = value.substring(value.indexOf('[') + 1, value.indexOf(']'));
                    if (!pageStr.isEmpty()) {
                        int pageToSelect = Integer.parseInt(pageStr.split(",")[0].trim());
                        selectPage(pageToSelect);

                        String animDisplayName = item.getParent().getValue();
                        animSelector.getSelectionModel().select(animDisplayName);
                    }
                } catch (Exception ignored) {}
            }
        });

        // Image 列表：选择一个图片进行预览
        imageList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null) {
                try {
                    String imageIndexStr = item.split(":")[0].trim();
                    previewImage(Integer.parseInt(imageIndexStr), item);
                } catch (NumberFormatException ignored) {}
            }
        });

        // 动画下拉框：选择后自动播放
        animSelector.setOnAction(e -> {
            if (animSelector.getValue() != null) onPlay();
        });

        // "总在最前" 复选框
        alwaysOnTopCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            Stage stage = (Stage) canvasHolder.getScene().getWindow();
            stage.setAlwaysOnTop(newVal);
        });

        // 监听 Scene 变化，为其添加快捷键
        canvasHolder.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardShortcuts(newScene);
            }
        });
    }

    /**
     * 设置画布的交互，如中键拖拽、滚轮缩放。
     */
    private void setupCanvasInteractions() {
        // 鼠标中键拖拽平移
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

        // Ctrl + 滚轮缩放
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1;
                zoomSlider.setValue(zoomSlider.getValue() * zoomFactor);
                event.consume();
            }
        });
    }

    /**
     * 设置滑块和文本框之间的双向绑定。
     */
    private void setupValueBindings() {
        // 缩放滑块与文本框绑定
        NumberFormat zoomFormat = new DecimalFormat("0.0x");
        StringConverter<Number> zoomConverter = new NumberStringConverter(zoomFormat);
        zoomField.textProperty().bindBidirectional(zoomSlider.valueProperty(), zoomConverter);

        // FPS滑块与文本框绑定
        NumberFormat fpsFormat = new DecimalFormat("0");
        StringConverter<Number> fpsConverter = new NumberStringConverter(fpsFormat);
        fpsField.textProperty().bindBidirectional(fpsSlider.valueProperty(), fpsConverter);
    }

    /**
     * 设置文件列表和动画列表的搜索过滤功能。
     */
    private void setupFiltering() {
        // SPM 文件搜索
        FilteredList<SpmEntry> filteredSpmList = new FilteredList<>(masterSpmList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredSpmList.setPredicate(spmEntry -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return spmEntry.getPath().getFileName().toString().toLowerCase().contains(lowerCaseFilter);
            });
            // 过滤后若无选中项，则自动选第一个
            if (spmListView.getSelectionModel().getSelectedItem() == null && !spmListView.getItems().isEmpty()) {
                spmListView.getSelectionModel().selectFirst();
                handleSpmSelection();
            }
        });
        spmListView.setItems(filteredSpmList);

        // 动画搜索
        animSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
            if (selectedSpm != null && selectedSpm.getSpm() != null) {
                uiStateController.updateUiForSpm(selectedSpm.getSpm(), newVal);
                // 搜索时自动展开动画面板
                if (newVal != null && !newVal.trim().isEmpty()) {
                    animTitledPane.setExpanded(true);
                }
            }
        });
    }

    /**
     * 为树状视图添加 "全部展开" 的右键菜单。
     */
    private void setupExpandAllContextMenus() {
        setupExpandAllContextMenu(pageTree);
        setupExpandAllContextMenu(animTree);
    }

    private void setupExpandAllContextMenu(TreeView<?> treeView) {
        var contextMenu = new ContextMenu();
        var expandAllItem = new MenuItem("全部展开");
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

    /**
     * 设置全局键盘快捷键。
     */
    private void setupKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // 播放/暂停媒体键切换动画
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

    /**
     * 加载上一次成功打开的目录。
     */
    private void loadLastDirectory() {
        String lastDirPath = Settings.getLastDirectory();
        if (lastDirPath != null && !lastDirPath.isEmpty()) {
            Path path = Path.of(lastDirPath);
            if (Files.isDirectory(path)) {
                spmFileHandler.loadDirectory(path);
            }
        }
    }

    /**
     * 处理 SPM 列表的选择事件。
     */
    private void handleSpmSelection() {
        SpmEntry selectedEntry = spmListView.getSelectionModel().getSelectedItem();
        loadAndDisplaySpm(selectedEntry);
    }

    /**
     * 异步加载并显示选定的 SPM 文件。
     * 使用后台任务处理耗时的图片加载，避免UI卡顿。
     *
     * @param entry 要加载的 SpmEntry
     */
    private void loadAndDisplaySpm(SpmEntry entry) {
        // 如果正在加载的就是当前这个文件，则直接返回
        if (entry == currentlyLoadingSpm) {
            return;
        }
        currentlyLoadingSpm = entry;

        onStop(); // 停止当前可能在播放的动画

        if (entry == null || entry.getStatus() != SpmEntry.Status.SUCCESS) {
            uiStateController.clearAllPanels();
            canvasController.clearCanvas();
            currentlyLoadingSpm = null;
            return;
        }

        // 准备后台加载任务
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 这是耗时操作：从磁盘加载所有关联的图片
                canvasController.setCurrentSpm(entry.getSpm(), spmFileHandler.getCurrentDirectory());
                return null;
            }
        };

        // 任务成功后的UI更新
        loadTask.setOnSucceeded(e -> {
            uiStateController.updateUiForSpm(entry.getSpm(), null); // null表示全量更新

            int pageCount = Optional.ofNullable(entry.getSpm().getNumPageData()).orElse(0);
            if (pageCount > 0) {
                selectPage(0); // 默认显示第一页
            } else {
                uiStateController.updateTablesForPage(null);
                canvasController.clearCanvas();
            }
            statusLabel.setText("就绪");
            progressBar.setVisible(false);
            currentlyLoadingSpm = null; // 加载完成，重置状态
        });

        // 任务失败处理
        loadTask.setOnFailed(e -> {
            statusLabel.setText("加载失败: " + entry.getPath().getFileName());
            progressBar.setVisible(false);
            Throwable ex = loadTask.getException();
            if (ex != null) {
                ex.printStackTrace(); // 打印错误到控制台供调试
            }
            currentlyLoadingSpm = null;
        });

        // UI即时反馈：显示加载中状态
        progressBar.progressProperty().unbind(); // 确保解绑，防止与目录加载冲突
        statusLabel.setText("正在加载 " + entry.getPath().getFileName() + "...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        animSearchField.clear();
        uiStateController.clearAllPanels();
        canvasController.clearCanvas();

        // 启动后台任务
        new Thread(loadTask).start();
    }

    /**
     * 选择并渲染指定的 Page。
     *
     * @param index Page 的索引
     */
    private void selectPage(Integer index) {
        SpmEntry selectedSpm = spmListView.getSelectionModel().getSelectedItem();
        if (selectedSpm == null || index == null) {
            return;
        }

        imageList.getSelectionModel().clearSelection();

        Spm spm = selectedSpm.getSpm();
        int pageCount = Optional.ofNullable(spm.getNumPageData()).orElse(0);
        if (index < 0 || index >= pageCount) {
            return;
        }

        canvasController.setCurrentPageIndex(index);
        uiStateController.updateTablesForPage(spm.getPageData().get(index));
        canvasController.renderPage();
    }

    /**
     * 预览指定的 Image。
     *
     * @param index     Image 的索引
     * @param imageName Image 的名称（用于显示错误信息）
     */
    private void previewImage(int index, String imageName) {
        onStop();
        pageTree.getSelectionModel().clearSelection();
        uiStateController.updateTablesForPage(null);
        canvasController.previewImage(index, imageName);
    }

    //<editor-fold desc="FXML Action Handlers">
    @FXML private void onOpenDirectory() {
        spmFileHandler.openSpmDirectory(canvasHolder.getScene().getWindow());
    }

    @FXML private void onShowCharsetSettings() {
        CharsetDialog.showDialog(canvasHolder.getScene().getWindow());
    }

    @FXML private void onExportPng() {
        var fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("spm-export.png");
        File out = fc.showSaveDialog(canvasHolder.getScene().getWindow());
        if (out == null) {
            return;
        }
        try {
            var snapshot = canvasController.getCanvas().snapshot(new SnapshotParameters(), null);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null), "png", out);
            statusLabel.setText("已导出: " + out.getName());
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "导出失败: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML private void onQuit() {
        ((Stage) canvasHolder.getScene().getWindow()).close();
    }

    @FXML private void onShowShortcuts() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(canvasHolder.getScene().getWindow());
        alert.setTitle("快捷键");
        alert.setHeaderText("键盘与鼠标快捷操作");
        String content =
                "播放/暂停媒体键: 切换动画播放/暂停\n\n" +
                        "Ctrl + 鼠标滚轮: 缩放画布\n\n" +
                        "鼠标中键拖拽: 平移画布";
        alert.setContentText(content);
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
        // 暂未实现
    }

    @FXML private void onPlay() {
        SpmEntry selectedSpmEntry = spmListView.getSelectionModel().getSelectedItem();
        if (selectedSpmEntry == null || selectedSpmEntry.getSpm() == null) {
            return;
        }

        String displayName = animSelector.getValue();
        if (displayName == null) {
            return;
        }

        onStop(); // 先停止之前的动画

        try {
            // 从 "[索引] 名称" 格式中解析出动画索引
            int startIndex = displayName.indexOf('[') + 1;
            int endIndex = displayName.indexOf(']');
            if (startIndex == 0 || endIndex == -1) return; // 格式不正确

            int animIndex = Integer.parseInt(displayName.substring(startIndex, endIndex));
            List<Spm.SPMAnimData> animDataList = selectedSpmEntry.getSpm().getAnimData();

            if (animIndex < 0 || animIndex >= animDataList.size()) return; // 索引越界

            Spm.SPMAnimData anim = animDataList.get(animIndex);

            // 构建并播放动画时间线
            List<KeyFrame> keyFrames = new ArrayList<>();
            int totalDuration = 0;
            int frameMillis = (int) (1000.0 / fpsSlider.getValue());

            for (Spm.SPMPatData pat : Optional.ofNullable(anim.getPatData()).orElse(List.of())) {
                int waitFrames = Math.max(1, Optional.ofNullable(pat.getWaitFrame()).orElse(1));
                for (Integer pageNo : Optional.ofNullable(pat.getPageNo()).orElse(List.of())) {
                    totalDuration += waitFrames * frameMillis;
                    keyFrames.add(new KeyFrame(Duration.millis(totalDuration), e -> selectPage(pageNo)));
                }
            }

            if (keyFrames.isEmpty()) return;

            animTimeline = new Timeline(keyFrames.toArray(new KeyFrame[0]));
            animTimeline.setCycleCount(Timeline.INDEFINITE);
            animTimeline.play();

        } catch (NumberFormatException e) {
            System.err.println("解析动画索引失败: " + displayName);
        }
    }

    @FXML private void onStop() {
        if (animTimeline != null) {
            animTimeline.stop();
            animTimeline = null;
        }
    }
    //</editor-fold>
}