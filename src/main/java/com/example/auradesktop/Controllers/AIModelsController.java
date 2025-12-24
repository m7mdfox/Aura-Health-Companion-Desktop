package com.example.auradesktop.Controllers;

import javafx.fxml.FXML;

public class AIModelsController {
    
    @FXML
    public void initialize() {
        // Initialization logic for AI Models page
    }

    @FXML
    public void goXRayAnalysis(javafx.event.ActionEvent event) {
        openAnalysis(event, XRayAnalysisController.AnalysisType.LUNG);
    }
    
    @FXML
    public void goBoneAnalysis(javafx.event.ActionEvent event) {
        openAnalysis(event, XRayAnalysisController.AnalysisType.BONE);
    }

    @FXML
    public void goBrainAnalysis(javafx.event.ActionEvent event) {
        openAnalysis(event, XRayAnalysisController.AnalysisType.BRAIN);
    }

    @FXML
    public void goDentalAnalysis(javafx.event.ActionEvent event) {
        openAnalysis(event, XRayAnalysisController.AnalysisType.DENTAL);
    }

    @FXML
    public void goEyeAnalysis(javafx.event.ActionEvent event) {
        openAnalysis(event, XRayAnalysisController.AnalysisType.EYE);
    }

    private void openAnalysis(javafx.event.ActionEvent event, XRayAnalysisController.AnalysisType type) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/auradesktop/XRayAnalysis.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Set the mode
            XRayAnalysisController controller = loader.getController();
            controller.setAnalysisType(type);

            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            if (stage.getScene().getRoot() instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) stage.getScene().getRoot()).setCenter(root);
            } else {
                 stage.getScene().setRoot(root);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
