module com.example.auradesktop {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;

    opens com.example.auradesktop to javafx.fxml;
    exports com.example.auradesktop;
}