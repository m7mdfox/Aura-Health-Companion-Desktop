package com.example.auradesktop.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class HomeController {

    // Ensure this field is injected from the FXML (fx:id="tableView")
    @FXML
    private TableView<?> tableView;

    // initialize() may be called before injection if fx:id/controller mismatch exists;
    // guard the existing logic to avoid NullPointerException and provide a helpful warning.
    @FXML
    public void initialize() {
        if (tableView != null) {
            // Explicitly load the stylesheet for the TableView
            tableView.getStylesheets().add(getClass().getResource("/com/example/auradesktop/tablestyle.css").toExternalForm());
        } else {
            // Minimal non-invasive logging to help debug injection mismatch.
            System.err.println("Warning: HomeController.tableView is null. Check fx:id in FXML and @FXML injection.");
            // Optionally continue other safe initialization:
            // ...existing code that does not depend on tableView...
        }
    }
}
