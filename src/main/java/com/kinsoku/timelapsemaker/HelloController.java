package com.kinsoku.timelapsemaker;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_java;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class HelloController {
    @FXML
    AnchorPane ap;
    @FXML
    TextField txtScriptFile;
    @FXML
    Button btnGenerateMovie;
    @FXML
    Button btnSelScriptFile;
    @FXML
    ProgressBar progressBar;

    final int IMAGE_WIDTH = 2248;
    final int IMAGE_HEIGHT = 2048;
    final double gain_bias = 1.5;
    final double brightness_bias = 50.;
    DoubleProperty processed = new SimpleDoubleProperty(0.);
    int noOfLines;

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
    private void onBtnGenerateMovie() {
        if (txtScriptFile.getText().isBlank()) {
            new Alert(Alert.AlertType.CONFIRMATION, "Select script file first.", ButtonType.OK).showAndWait();
            return;
        }
        if (!new File(txtScriptFile.getText()).isFile()) {
            new Alert(Alert.AlertType.WARNING, "Script file not found.", ButtonType.OK).showAndWait();
            return;
        }

        try {
            noOfLines = countLines(new File(txtScriptFile.getText()));
            if (noOfLines <= 0) {
                new Alert(Alert.AlertType.WARNING, "Script file is empty.", ButtonType.OK).showAndWait();
                return;
            }
            final Task<Void> movieMaker = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    MakeMovie(new File(txtScriptFile.getText()));
                    return null;
                }
            };
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(processed);
            movieMaker.setOnSucceeded((state)->{
                Platform.runLater(()->new Alert(Alert.AlertType.INFORMATION, "Finished.", ButtonType.OK).showAndWait());
            });
            new Thread(movieMaker).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private int countLines(File textFile) throws IOException {
        int count = 0;
        try (LineNumberReader br = new LineNumberReader(new FileReader(textFile));) {
            String line;
            while ((line = br.readLine()) != null) {
                count++;
            }
        } catch (IOException ex) {
            throw ex;
        }
        return count;
    }

    private void MakeMovie(File scriptFile) throws IOException {
        Loader.load(opencv_java.class);

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        FrameRecorder recorder = FrameRecorder.createDefault("output.avi", IMAGE_WIDTH / 2, IMAGE_HEIGHT / 4);
        recorder.setFrameRate(2);
        recorder.start();

        try (LineNumberReader br = new LineNumberReader(new FileReader(scriptFile));) {
            processed.set(0);
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] d = line.split(",", 0);
                    Mat leftImage = createFrameImage(d[1]);
                    Mat rightImage = createFrameImage(d[2]);
                    Mat builtImage = new Mat();
                    hconcat(new MatVector(leftImage, rightImage), builtImage);
//                imshow("monitor", builtImage);
                    Frame frame = converter.convert(builtImage);
                    recorder.record(frame);
                } catch (RuntimeException ex) {
                    System.out.println(ex.getMessage());
                }
                processed.set((double)br.getLineNumber() / noOfLines);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            recorder.stop();
        }
    }

    private Mat createFrameImage(String imageFileName) {
        Mat image = imread(imageFileName);
        if (image == null) {
            image = new Mat(IMAGE_HEIGHT, IMAGE_WIDTH, CV_64FC1, new Scalar(0, 0, 0, 2.0));
        }
        Mat resizedImage = new Mat();
        org.bytedeco.opencv.global.opencv_imgproc.resize(image, resizedImage, new Size((int) (IMAGE_WIDTH / 4), (int) (IMAGE_HEIGHT / 4)));
        Mat brightImage = new Mat();
        convertScaleAbs(resizedImage, brightImage, gain_bias, brightness_bias);
        putText(brightImage, new File(imageFileName).getName(), new Point(20, 20), FONT_HERSHEY_DUPLEX, 0.5, new Scalar(255, 255, 255, 2.0));
        return brightImage;
    }
}
