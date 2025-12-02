package com.example.auradesktop.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class TopBarController {

    @FXML
    public void goHome(ActionEvent event) {
        loadView("/com/example/auradesktop/Home.fxml", event);
    }

    @FXML
    public void goPatients(ActionEvent event) {
        loadView("/com/example/auradesktop/Patients.fxml", event);
    }

    @FXML
    public void goCalender(ActionEvent event) {
        loadView("/com/example/auradesktop/Calender.fxml", event);
    }

    @FXML
    public void goRequests(ActionEvent event) {
        loadView("/com/example/auradesktop/Requests.fxml", event);
    }

    private void loadView(String fxmlPath, ActionEvent event) {
        try {
            // Try to replace only the center content if the current root is a BorderPane (Main.fxml approach)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent rootCandidate = stage.getScene().getRoot();
            if (rootCandidate instanceof BorderPane) {
                // load the new content and set it in the center
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                ((BorderPane) rootCandidate).setCenter(content);
            } else {
                // fallback: replace whole scene root
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
