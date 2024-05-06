module com.kinsoku.timelapsemaker {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;


    opens com.kinsoku.timelapsemaker to javafx.fxml;
    exports com.kinsoku.timelapsemaker;
    exports com.kinsoku.application;
    opens com.kinsoku.application to javafx.fxml;
}