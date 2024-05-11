package com.kinsoku.timelapsemaker;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_java;

import java.io.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_DUPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;

public class TimelapseMaker extends Task  {
    final int IMAGE_WIDTH = 2248;
    final int IMAGE_HEIGHT = 2048;
    final double gain_bias = 1.5;
    final double brightness_bias = 50.;
    DoubleProperty processed = new SimpleDoubleProperty(0.);
    int noOfLines;
    String scriptFileName;

    public TimelapseMaker(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    @Override
    protected Void call() throws Exception {
        MakeMovie(new File(scriptFileName));
        return null;
    }

    public DoubleProperty getProcessed() {
        return processed;
    }

    private int countLines(File textFile) throws IOException {
        int count = 0;
        try (LineNumberReader br = new LineNumberReader(new InputStreamReader(new FileInputStream(textFile), "SJIS"));) {
            String line;
            while ((line = br.readLine()) != null) {
                count++;
            }
        } catch (IOException ex) {
            throw ex;
        }
        return count;
    }

    private void MakeMovie(File scriptFile) throws IOException, IllegalArgumentException {
        noOfLines = countLines(new File(scriptFileName));
        if (noOfLines <= 0) {
            throw new IllegalArgumentException("Script file is empty.");
        }

        Loader.load(opencv_java.class);

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        FrameRecorder recorder = FrameRecorder.createDefault("output.mp4", IMAGE_WIDTH / 2, IMAGE_HEIGHT / 4);
        recorder.setFormat("mp4");
        recorder.setFrameRate(2);
        recorder.start();

        try (LineNumberReader br = new LineNumberReader(new FileReader(scriptFile));) {
            processed.set(0);
            String line;
            while ((line = br.readLine()) != null) {
                if (isCancelled()) {
                    break;
                }
                try {
                    String[] d = line.split(",", 0);
                    Mat leftImage = createFrameImage(d[1], d);
                    Mat rightImage = createFrameImage(d[2], d);
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

    private Mat createFrameImage(final String imageFileName, final String str[]) {
        Mat image = imread(imageFileName);
        if (image == null) {
            image = new Mat(IMAGE_HEIGHT, IMAGE_WIDTH, CV_64FC1, new Scalar(0, 0, 0, 2.0));
        }
        Mat resizedImage = new Mat();
        org.bytedeco.opencv.global.opencv_imgproc.resize(image, resizedImage, new Size((int) (IMAGE_WIDTH / 4), (int) (IMAGE_HEIGHT / 4)));
        Mat brightImage = new Mat();
        convertScaleAbs(resizedImage, brightImage, gain_bias, brightness_bias);
        putText(brightImage, new File(imageFileName).getName(), new Point(20, 20), FONT_HERSHEY_DUPLEX, 0.5, new Scalar(255, 255, 255, 2.0));
        StringBuilder sb = new StringBuilder();
        if (str[3] != null) { sb.append(str[3]);}
        if (str[4] != null) { sb.append(" R:"); sb.append(str[4]);}
        if (str[5] != null) { sb.append(" S:"); sb.append(str[5]);}
        if (str[6] != null) { sb.append(" "); sb.append(str[6]);}
        putText(brightImage, sb.toString(), new Point(20, 40), FONT_HERSHEY_DUPLEX, 0.5, new Scalar(255, 255, 255, 2.0));
        return brightImage;
    }

}
