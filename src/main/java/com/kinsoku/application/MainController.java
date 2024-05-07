package com.kinsoku.application;

import com.kinsoku.timelapsemaker.TimelapseMaker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;

public class MainController {
    @FXML
    AnchorPane ap;
    @FXML
    TextField txtScriptFile;
    @FXML
    Button btnCreateMovie;
    @FXML
    Button btnSelScriptFile;
    @FXML
    ProgressBar progressBar;

    private TimelapseMaker maker;

    @FXML
    private void onBtnSelScriptFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select script file");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv", "*.CSV"),
                new FileChooser.ExtensionFilter("*.*", "*.*")
        );
        fc.setInitialDirectory(new File("."));
        File file = fc.showOpenDialog(null);
        if (file == null) return;
        txtScriptFile.setText(file.getAbsolutePath());
    }

    @FXML
    private void onBtnCreateMovie() {
        if (txtScriptFile.getText().isBlank()) {
            new Alert(Alert.AlertType.CONFIRMATION, "Select script file first.", ButtonType.OK).showAndWait();
            return;
        }
        if (!new File(txtScriptFile.getText()).isFile()) {
            new Alert(Alert.AlertType.WARNING, "Script file not found.", ButtonType.OK).showAndWait();
            return;
        }

        if (maker != null && maker.isRunning()) {
            maker.cancel();
            btnCreateMovie.setText("Create movie");
        } else {
            maker = new TimelapseMaker(txtScriptFile.getText());
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(maker.getProcessed());
            maker.setOnSucceeded((state) -> {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Finished.", ButtonType.OK).showAndWait();
                    btnCreateMovie.setText("Create movie");
                });
            });
            maker.setOnFailed((state) -> {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Failed.", ButtonType.OK).showAndWait();
                    btnCreateMovie.setText("Create movie");
                });
            });
            maker.setOnCancelled((state) -> {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Cancelled.", ButtonType.OK).showAndWait();
                    btnCreateMovie.setText("Create movie");
                });
            });
            try {
                btnCreateMovie.setText("Cancell");
                new Thread(maker).start();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.WARNING, ex.getMessage(), ButtonType.OK).showAndWait();
                    btnCreateMovie.setText("Create movie");
                });
            }
        }

    }

}
