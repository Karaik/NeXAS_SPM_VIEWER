package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.spm.Spm;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

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

    public void updateUiForSpm(Spm spm, String animFilter) {
        if (animFilter == null) {
            clearAllPanels();
        } else {
            animTree.setRoot(null);
            animSelector.getItems().clear();
        }

        if (spm == null) return;

        if (animFilter == null) {
            lblVersion.setText(spm.getSpmVersion());
            lblPages.setText(String.valueOf(safe(spm.getNumPageData())));
            lblImages.setText(String.valueOf(safe(spm.getNumImageData())));
            lblAnims.setText(String.valueOf(safe(spm.getNumAnimData())));

            var images = Optional.ofNullable(spm.getImageData()).orElse(List.of());
            for (int i = 0; i < images.size(); i++) {
                String name = Optional.ofNullable(images.get(i).getImageName()).orElse("<image " + i + ">");
                imageList.getItems().add(i + ": " + name);
            }

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
        }

        var rootAnims = new TreeItem<>("Animations");
        animTree.setRoot(rootAnims);
        rootAnims.setExpanded(true);
        var anims = Optional.ofNullable(spm.getAnimData()).orElse(List.of());

        for (int i = 0; i < anims.size(); i++) {
            var a = anims.get(i);
            String animName = Optional.ofNullable(a.getAnimName()).orElse("").trim();
            if (animFilter != null && !animFilter.trim().isEmpty()) {
                if (!animName.toLowerCase().contains(animFilter.trim().toLowerCase())) {
                    continue;
                }
            }
            String displayName = String.format("[%d] %s", i, animName).trim();

            var ai = new TreeItem<>(displayName);
            if (a.getPatData() != null) {
                for (int j = 0; j < a.getPatData().size(); j++) {
                    var pat = a.getPatData().get(j);
                    String patLabel = String.format("pat[%d] wf=%d pages=%s", j, pat.getWaitFrame(),
                            Optional.ofNullable(pat.getPageNo()).orElse(List.of()));
                    ai.getChildren().add(new TreeItem<>(patLabel));
                }
            }
            rootAnims.getChildren().add(ai);
            animSelector.getItems().add(displayName);
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
