package com.example.auradesktop.Controllers;

import com.example.auradesktop.services.GeminiService;
import com.example.auradesktop.services.PdfReportService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class XRayAnalysisController {

    @FXML
    private ImageView imageView;
    @FXML
    private Button analyzeBtn;
    @FXML
    private Label resultLabel; 
    @FXML
    private Button savePdfBtn;
    @FXML
    private Label statusLabel;

    private File selectedImageFile;
    private String detailedReportContent = ""; // Store the full report for PDF

    @FXML
    public void uploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select X-Ray Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            imageView.setImage(new Image(selectedImageFile.toURI().toString()));
            analyzeBtn.setDisable(false);
            resultLabel.setText("Ready to analyze.");
            resultLabel.setStyle("-fx-border-color: #cccccc; -fx-background-color: white; -fx-text-fill: black; -fx-padding: 20;");
            savePdfBtn.setDisable(true);
            statusLabel.setText("Image loaded.");
            detailedReportContent = "";
        }
    }

    public enum AnalysisType { LUNG, BONE, BRAIN, DENTAL, EYE }
    private AnalysisType currentType = AnalysisType.LUNG; // Default

    public void setAnalysisType(AnalysisType type) {
        this.currentType = type;
        if (type == AnalysisType.BONE) {
            resultLabel.setText("Bone Fracture Mode Ready");
        } else if (type == AnalysisType.BRAIN) {
            resultLabel.setText("Brain Analysis Mode Ready");
        } else if (type == AnalysisType.DENTAL) {
            resultLabel.setText("Dental Analysis Mode Ready");
        } else if (type == AnalysisType.EYE) {
            resultLabel.setText("Eye Disease Mode Ready");
        } else {
            resultLabel.setText("Lung Disease Mode Ready");
        }
    }

    @FXML
    public void analyzeImage(ActionEvent event) {
        if (selectedImageFile == null) return;

        statusLabel.setText("Analyzing image (" + currentType + ")... Please wait.");
        analyzeBtn.setDisable(true);
        resultLabel.setText("Analyzing...");

        new Thread(() -> {
            try {
                com.google.gson.JsonObject result;
                if (currentType == AnalysisType.BONE) {
                    result = GeminiService.analyzeBone(selectedImageFile);
                } else if (currentType == AnalysisType.BRAIN) {
                    result = GeminiService.analyzeBrain(selectedImageFile);
                } else if (currentType == AnalysisType.DENTAL) {
                    result = GeminiService.analyzeDental(selectedImageFile);
                } else if (currentType == AnalysisType.EYE) {
                    result = GeminiService.analyzeEye(selectedImageFile);
                } else {
                    result = GeminiService.analyzeLung(selectedImageFile);
                }
                
                Platform.runLater(() -> {
                    if (result.has("error")) {
                        statusLabel.setText("Error occurred.");
                        // Show the actual error message on screen
                        resultLabel.setText(result.get("error").getAsString());
                        resultLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;"); 
                        detailedReportContent = result.get("error").getAsString();
                    } else {
                        // Parse JSON fields
                        String classification = result.has("classification") ? result.get("classification").getAsString() : "Unknown";
                        String verdict = result.has("verdict") ? result.get("verdict").getAsString() : "Diagnosis Complete";
                        String details = result.has("details") ? result.get("details").getAsString() : "No details provided.";
                        
                        detailedReportContent = details;

                        // UI Update logic
                        resultLabel.setText(verdict);
                        if ("Abnormal".equalsIgnoreCase(classification) || verdict.toLowerCase().contains("injured") || verdict.toLowerCase().contains("stroke") || verdict.toLowerCase().contains("tumor") || verdict.toLowerCase().contains("bleed") || verdict.toLowerCase().contains("caries") || verdict.toLowerCase().contains("infection") || verdict.toLowerCase().contains("impacted") || verdict.toLowerCase().contains("blindness") || verdict.toLowerCase().contains("glaucoma") || verdict.toLowerCase().contains("cataract")) {
                            // Red for abnormal
                            resultLabel.setStyle("-fx-border-color: #ff0000; -fx-background-color: #ffebee; -fx-text-fill: #d32f2f; -fx-padding: 20; -fx-alignment: center;");
                        } else {
                            // Green for normal
                            resultLabel.setStyle("-fx-border-color: #4caf50; -fx-background-color: #e8f5e9; -fx-text-fill: #388e3c; -fx-padding: 20; -fx-alignment: center;");
                        }
                        
                        statusLabel.setText("Analysis complete.");
                        savePdfBtn.setDisable(false);
                    }
                    analyzeBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    resultLabel.setText("Error");
                    analyzeBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void saveReport(ActionEvent event) {
        if (detailedReportContent == null || detailedReportContent.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Detailed Medical Report");
        
        // Dynamic Filename
        String prefix = "Medical_Report_";
        if (currentType == AnalysisType.BONE) prefix = "Bone_Fracture_Report_";
        else if (currentType == AnalysisType.BRAIN) prefix = "Brain_Analysis_Report_";
        else if (currentType == AnalysisType.DENTAL) prefix = "Dental_Analysis_Report_";
        else if (currentType == AnalysisType.EYE) prefix = "Eye_Analysis_Report_";
        else prefix = "Lung_Analysis_Report_";
        chooser.setInitialFileName(prefix + System.currentTimeMillis() + ".pdf");
        
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        Stage stage = (Stage) savePdfBtn.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        
        if (file == null) return;

        statusLabel.setText("Generating PDF...");
        new Thread(() -> {
            try {
                String patientName = "Hidden Patient"; 
                // Generates PDF with the FULL detailed report
                PdfReportService.createReport(file, patientName, detailedReportContent);
                
                Platform.runLater(() -> statusLabel.setText("Detailed Report Saved."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Error saving PDF: " + e.getMessage()));
            }
        }).start();
    }
}
