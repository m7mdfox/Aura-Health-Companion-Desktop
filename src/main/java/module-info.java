module Aura.Desktop {
    requires com.google.gson;
    requires java.net.http;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires org.json;

    // Socket.IO dependencies
    requires socket.io.client;
    requires engine.io.client;

    // --- EXPORTS ---
    // Allow JavaFX Graphics to start the App
    exports com.example.auradesktop.Applications to javafx.graphics;

    // --- OPENS (Permissions) ---

    // Allow FXML Loader to access Controllers
    opens com.example.auradesktop.Applications to javafx.fxml;
    opens com.example.auradesktop.Controllers to javafx.fxml;

    // Allow GSON (JSON) AND JavaFX (Tables) to access Models
    // This fixes your crash:
    opens com.example.auradesktop.models to com.google.gson, javafx.base;

    // If you use PropertyValueFactory in other packages, you might need to export/open them too.
    exports com.example.auradesktop;
}