package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.model.Settings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.nio.charset.Charset;
import java.util.Optional;

public class CharsetDialog extends Dialog<String> {

    public CharsetDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL); // Ensures the dialog stays on top of its owner

        setTitle("Charset Settings");
        setHeaderText("Select or add a character set for SPM file parsing.");

        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("windows-31j", "UTF-8", "GBK");
        comboBox.setEditable(true); // Allow custom input
        comboBox.setValue(Settings.getCharsetName());

        grid.add(new Label("Charset:"), 0, 0);
        grid.add(comboBox, 1, 0);

        dialogPane.setContent(grid);

        // Convert the result to the selected charset name when OK is clicked.
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String selectedCharset = comboBox.getValue();
                try {
                    // Validate the charset before accepting
                    Charset.forName(selectedCharset);
                    return selectedCharset;
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Invalid Charset" + "The charset '" + selectedCharset + "' is not supported.", ButtonType.NO).showAndWait();
                    return null; // Stay in dialog
                }
            }
            return null;
        });
    }

    public static void showDialog(Window owner) {
        CharsetDialog dialog = new CharsetDialog(owner);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(Settings::setCharsetName);
    }
}