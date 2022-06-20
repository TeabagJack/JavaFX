module com.example.javafx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;
    requires slick.util3;
    requires org.lwjgl.opengl;
    requires PNGDecoder;
    requires org.lwjgl.glfw;
    requires java.sql;

    opens com.example.javafx to javafx.fxml;
    exports com.example.javafx;
}