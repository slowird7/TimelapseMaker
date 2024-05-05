package com.kinsoku.timelapsemaker;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

class OpenCVUtilTest {

    void testGetColor() {
        // 画像読み込み
        Path img = Paths.get("C:\\gomibako\\rgb.jpg");
        Mat mat = imread(img.toFile().getAbsolutePath());

        // 画像のサイズを取得する
        UByteIndexer srcIndexer = mat.createIndexer();
        long rows = srcIndexer.sizes()[0];
        long cols = srcIndexer.sizes()[1];

        for (int x = 0; x < rows; x++) {
            int[] values = new int[3];
            for (int y = 0; y < cols; y++) {
                // getでx, y位置の色を取得することが出来る。
                srcIndexer.get(x, y, values);
                // valuesにはRGBではなくBGRの順番で格納される
                System.out.println("B = " + values[0] + ", G = " + values[1] + ", R = " + values[2]);
            }
        }
    }
}