package com.karaik.spmviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Locale;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        Locale.setDefault(Locale.ENGLISH);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 920, 720);
        URL url = getClass().getResource("/images/head.png");
        if (url != null) {
            stage.getIcons().add(new Image(url.toExternalForm()));
        }
        stage.setTitle("NeXAS Sprite Viewer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}