package com.example.auradesktop.Controllers;

import com.example.auradesktop.UserSession;
import com.example.auradesktop.models.Appointment;
import com.example.auradesktop.services.DoctorApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RequestsController implements Initializable {

    @FXML
    private TableView<Appointment> requestsTable;
    @FXML
    private TableColumn<Appointment, String> nameColumn;
    @FXML
    private TableColumn<Appointment, String> timeColumn;
    @FXML
    private TableColumn<Appointment, Void> statusColumn;

    private ObservableList<Appointment> requestList = FXCollections.observableArrayList();
    private DoctorApiService apiService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        apiService = new DoctorApiService();

        // 1. Setup Columns
        // Ensure Appointment.java has getPatientName() and getStartTime()
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        // 2. Add Buttons
        addButtonToTable();

        // 3. Load Data
        loadPendingAppointments();
    }

    private void loadPendingAppointments() {
        String doctorId = UserSession.getInstance().getDoctorId();
        System.out.println("=== REQUESTS SCREEN DEBUG ===");
        System.out.println("Doctor ID from session: " + doctorId);

        if (doctorId == null || doctorId.isEmpty()) {
            System.out.println("❌ ERROR: Doctor ID is null or empty!");
            return;
        }

        apiService.getAppointments(doctorId).thenAccept(appointments -> {
            System.out.println("API Response received.");

            if (appointments == null) {
                System.out.println("❌ API returned null!");
                return;
            }

            System.out.println("Total fetched from DB: " + appointments.size());

            // DEBUG: Print each appointment details
            for (var appt : appointments) {
                System.out.println("  📋 ID: " + appt.getId() +
                        " | Patient: " + appt.getPatientName() +
                        " | Status: '" + appt.getStatus() + "'");
            }

            // --- FILTER LOGIC ---
            var pending = appointments.stream()
                    // Only show "requested" items in this table
                    .filter(a -> "requested".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());

            System.out.println("Filtered (Visible in Table): " + pending.size());

            Platform.runLater(() -> {
                requestList.setAll(pending);
                requestsTable.setItems(requestList);
                requestsTable.refresh(); // Force table refresh

                if (pending.isEmpty()) {
                    System.out.println("Table is empty because no appointments have status 'requested'");
                } else {
                    System.out.println("✅ Added " + pending.size() + " items to table");
                }
            });
        }).exceptionally(ex -> {
            System.out.println("❌ API Error: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void addButtonToTable() {
        Callback<TableColumn<Appointment, Void>, TableCell<Appointment, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Appointment, Void> call(final TableColumn<Appointment, Void> param) {
                return new TableCell<>() {
                    private final Button acceptBtn = new Button("Accept");
                    private final Button rejectBtn = new Button("Reject");
                    private final HBox pane = new HBox(10, acceptBtn, rejectBtn);

                    {
                        acceptBtn.getStyleClass().add("btn-accept");
                        rejectBtn.getStyleClass().add("btn-reject");

                        acceptBtn.setOnAction(event -> {
                            Appointment appt = getTableView().getItems().get(getIndex());
                            handleAction(appt, "confirmed");
                        });

                        rejectBtn.setOnAction(event -> {
                            Appointment appt = getTableView().getItems().get(getIndex());
                            handleAction(appt, "cancelled");
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        };
        statusColumn.setCellFactory(cellFactory);
    }

    private void handleAction(Appointment appt, String status) {
        apiService.updateStatus(appt.getId(), status).thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    // Remove from the table immediately because it's no longer "requested"
                    requestList.remove(appt);
                    System.out.println("Appointment marked as " + status);

                    // If confirmed, it will now appear in the CHAT screen (PatientsController)
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update status");
                    alert.show();
                }
            });
        });
    }
}