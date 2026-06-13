package com.karaik.spmviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

@Slf4j
public class MainApplication extends Application {

    private static final Path UNCAUGHT_LOG_PATH = Path.of(
            System.getProperty("java.io.tmpdir"),
            "NeXAS_SPM_VIEWER_uncaught.log"
    );

    @Override
    public void start(Stage stage) throws Exception {
        installGlobalExceptionLogging();

        Locale.setDefault(Locale.ENGLISH);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1080, 720);
            URL url = getClass().getResource("/images/head.png");
            if (url != null) {
                stage.getIcons().add(new Image(url.toExternalForm()));
            }
            stage.setTitle("NeXAS Sprite Viewer");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            log.error("Failed to start JavaFX application", ex);
            throw ex;
        }
    }

    public static void main(String[] args) {
        installGlobalExceptionLogging();
        launch(args);
    }

    private static void installGlobalExceptionLogging() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception on thread {}", thread.getName(), throwable);
            persistUnhandledException(thread, throwable);
        });
    }

    private static void persistUnhandledException(Thread thread, Throwable throwable) {
        try {
            Files.createDirectories(UNCAUGHT_LOG_PATH.getParent());
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("=== Uncaught exception on thread: " + thread.getName() + " ===");
            throwable.printStackTrace(printWriter);
            printWriter.println();
            printWriter.flush();

            Files.writeString(
                    UNCAUGHT_LOG_PATH,
                    stringWriter.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            System.err.println("Full uncaught exception written to: " + UNCAUGHT_LOG_PATH.toAbsolutePath());
        } catch (Exception ex) {
            log.error("Failed to persist uncaught exception to {}", UNCAUGHT_LOG_PATH, ex);
        }
    }
}
