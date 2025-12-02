package com.example.auradesktop.Controllers;

import com.example.auradesktop.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class HomeController {

    @FXML
    private TableView<?> tableView;

    @FXML
    private Label doctorNameLabel;

    @FXML
    public void initialize() {
        // 1. Set the Doctor's Name from the Session
        UserSession session = UserSession.getInstance();
        if (session.getDoctorName() != null && !session.getDoctorName().isEmpty()) {
            doctorNameLabel.setText("Dr. " + session.getDoctorName());
        } else {
            doctorNameLabel.setText("Dr. Guest");
        }

        // 2. Initialize TableView (if it exists in your logic)
        if (tableView != null) {
            tableView.getStylesheets().add(getClass().getResource("/com/example/auradesktop/tablestyle.css").toExternalForm());
        }
    }
}