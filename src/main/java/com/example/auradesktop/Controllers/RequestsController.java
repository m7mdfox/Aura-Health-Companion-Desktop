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

    @FXML private TableView<Appointment> requestsTable;
    @FXML private TableColumn<Appointment, String> nameColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, Void> statusColumn;

    private ObservableList<Appointment> requestList = FXCollections.observableArrayList();
    private DoctorApiService apiService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        apiService = new DoctorApiService();

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("patientName")); // Matches getter in model
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        addButtonToTable();
        loadPendingAppointments();
    }

    // In RequestsController.java

    private void loadPendingAppointments() {
        String doctorId = UserSession.getInstance().getDoctorId();

        apiService.getAppointments(doctorId).thenAccept(appointments -> {
            System.out.println("Total fetched: " + appointments.size());

            // --- FIX: CHANGE "pending" TO "requested" ---
            var pending = appointments.stream()
                    .filter(a -> "requested".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());

            System.out.println("Filtered requests: " + pending.size());

            Platform.runLater(() -> {
                requestList.setAll(pending);
                requestsTable.setItems(requestList);
            });
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
                    requestList.remove(appt); // Remove row from table
                    System.out.println("Appointment " + status);
                } else {
                    System.err.println("Failed to update status");
                }
            });
        });
    }
}