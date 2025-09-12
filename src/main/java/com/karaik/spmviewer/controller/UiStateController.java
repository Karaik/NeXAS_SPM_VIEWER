package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.SpmEntry;
import com.karaik.spmviewer.spm.Spm;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class UiStateController {

    private final TreeView<String> pageTree;
    private final TreeView<String> animTree;
    private final ListView<String> imageList;
    private final ComboBox<String> animSelector;
    private final Label lblVersion, lblPages, lblImages, lblAnims;
    private final TableView<Spm.SPMChipData> chipTable;
    private final TableView<Spm.SPMHitArea> hitTable;

    public UiStateController(TreeView<String> pageTree, TreeView<String> animTree, ListView<String> imageList,
                             ComboBox<String> animSelector, Label lblVersion, Label lblPages, Label lblImages,
                             Label lblAnims, TableView<Spm.SPMChipData> chipTable, TableView<Spm.SPMHitArea> hitTable) {
        this.pageTree = pageTree;
        this.animTree = animTree;
        this.imageList = imageList;
        this.animSelector = animSelector;
        this.lblVersion = lblVersion;
        this.lblPages = lblPages;
        this.lblImages = lblImages;
        this.lblAnims = lblAnims;
        this.chipTable = chipTable;
        this.hitTable = hitTable;
    }

    public void updateUiForSpm(Spm spm) {
        clearAllPanels();
        if (spm == null) return;

        // Info Tab
        lblVersion.setText(spm.getSpmVersion());
        lblPages.setText(String.valueOf(safe(spm.getNumPageData())));
        lblImages.setText(String.valueOf(safe(spm.getNumImageData())));
        lblAnims.setText(String.valueOf(safe(spm.getNumAnimData())));

        // Images List
        if (spm.getImageData() != null) {
            for (int i = 0; i < spm.getImageData().size(); i++) {
                String name = Optional.ofNullable(spm.getImageData().get(i).getImageName()).orElse("<image " + i + ">");
                imageList.getItems().add(i + ": " + name);
            }
        }

        // Pages Tree
        var rootPages = new TreeItem<>("Pages");
        pageTree.setRoot(rootPages);
        rootPages.setExpanded(true);
        var pages = Optional.ofNullable(spm.getPageData()).orElse(List.of());
        for (int i = 0; i < pages.size(); i++) {
            var p = pages.get(i);
            String label = String.format("%d (%d×%d) chips=%d",
                    i, p.getPageWidth(), p.getPageHeight(),
                    Optional.ofNullable(p.getChipData()).map(List::size).orElse(0));
            rootPages.getChildren().add(new TreeItem<>(label));
        }

        // Animations Tree and ComboBox
        var rootAnims = new TreeItem<>("Animations");
        animTree.setRoot(rootAnims);
        rootAnims.setExpanded(true);
        var anims = Optional.ofNullable(spm.getAnimData()).orElse(List.of());
        for (var a : anims) {
            var ai = new TreeItem<>(a.getAnimName());
            if (a.getPatData() != null) {
                for (int i = 0; i < a.getPatData().size(); i++) {
                    var pat = a.getPatData().get(i);
                    String patLabel = String.format("pat[%d] wf=%d pages=%s", i, pat.getWaitFrame(),
                            Optional.ofNullable(pat.getPageNo()).orElse(List.of()));
                    ai.getChildren().add(new TreeItem<>(patLabel));
                }
            }
            rootAnims.getChildren().add(ai);
            animSelector.getItems().add(a.getAnimName());
        }
        if (!animSelector.getItems().isEmpty()) animSelector.getSelectionModel().selectFirst();
    }

    public void updateTablesForPage(Spm.SPMPageData page) {
        if (page != null) {
            chipTable.getItems().setAll(Optional.ofNullable(page.getChipData()).orElse(List.of()));
            hitTable.getItems().setAll(Optional.ofNullable(page.getHitRects()).orElse(List.of()));
        } else {
            chipTable.getItems().clear();
            hitTable.getItems().clear();
        }
    }

    public void clearAllPanels() {
        lblVersion.setText("-");
        lblPages.setText("-");
        lblImages.setText("-");
        lblAnims.setText("-");

        pageTree.setRoot(null);
        animTree.setRoot(null);
        imageList.getItems().clear();
        animSelector.getItems().clear();
        chipTable.getItems().clear();
        hitTable.getItems().clear();
    }

    private int safe(Integer i) { return i == null ? 0 : i; }
}