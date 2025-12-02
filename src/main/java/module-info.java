module com.example.auradesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    // calendarfx removed: using JavaFX built-in DatePicker instead

    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;

    // For FXML loading
    opens com.example.auradesktop to javafx.fxml;
    opens com.example.auradesktop.Controllers to javafx.fxml;

    // ⚠ JavaFX needs access to Application class
    exports com.example.auradesktop.Applications;
    exports com.example.auradesktop;
}
