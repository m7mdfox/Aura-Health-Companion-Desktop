package com.example.auradesktop.Controllers;

import com.example.auradesktop.UserSession;
import com.example.auradesktop.models.Appointment;
import com.example.auradesktop.services.DoctorApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CalenderController implements Initializable {

    @FXML
    private DatePicker datePicker;

    @FXML
    private TableView<Appointment> appointmentsTable;

    @FXML
    private TableColumn<Appointment, String> nameColumn;

    @FXML
    private TableColumn<Appointment, String> timeColumn;

    @FXML
    private TableColumn<Appointment, String> notesColumn;

    private DoctorApiService apiService;
    private List<Appointment> allAppointments;
    private ObservableList<Appointment> filteredAppointments = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        apiService = new DoctorApiService();

        // Setup table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeRange"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // Set default date to today
        datePicker.setValue(LocalDate.now());

        // Listen for date changes
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                filterAppointmentsByDate(newDate);
            }
        });

        // Load appointments from server
        loadAppointments();
    }

    private void loadAppointments() {
        String doctorId = UserSession.getInstance().getDoctorId();
        System.out.println("📅 Calendar: Loading appointments for doctor: " + doctorId);

        if (doctorId == null || doctorId.isEmpty()) {
            System.out.println("❌ Calendar: Doctor ID is null or empty!");
            return;
        }

        apiService.getAppointments(doctorId).thenAccept(appointments -> {
            Platform.runLater(() -> {
                if (appointments == null || appointments.isEmpty()) {
                    System.out.println("📅 Calendar: No appointments found");
                    allAppointments = List.of();
                } else {
                    // Only keep confirmed appointments for the calendar
                    allAppointments = appointments.stream()
                            .filter(a -> "confirmed".equalsIgnoreCase(a.getStatus()))
                            .collect(Collectors.toList());
                    System.out.println("📅 Calendar: Loaded " + allAppointments.size() + " confirmed appointments");
                }

                // Filter by currently selected date
                filterAppointmentsByDate(datePicker.getValue());
            });
        }).exceptionally(ex -> {
            System.out.println("❌ Calendar: Error loading appointments: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void filterAppointmentsByDate(LocalDate selectedDate) {
        if (allAppointments == null) {
            return;
        }

        System.out.println("📅 Calendar: Filtering for date: " + selectedDate);

        List<Appointment> matched = allAppointments.stream()
                .filter(appt -> {
                    try {
                        // Parse the appointment date
                        String dateStr = appt.getDate();
                        LocalDate apptDate;

                        if (dateStr.contains("T") && dateStr.endsWith("Z")) {
                            // Parse ISO format: "2025-12-17T22:00:00.000Z"
                            java.time.Instant instant = java.time.Instant.parse(dateStr);
                            apptDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        } else if (dateStr.contains("T")) {
                            // Parse without Z: "2025-12-17T00:00:00"
                            apptDate = LocalDate.parse(dateStr.substring(0, 10));
                        } else {
                            // Just date: "2025-12-17"
                            apptDate = LocalDate.parse(dateStr);
                        }

                        return apptDate.equals(selectedDate);
                    } catch (Exception e) {
                        System.err.println("❌ Calendar: Error parsing date: " + appt.getDate());
                        return false;
                    }
                })
                .collect(Collectors.toList());

        System.out.println("📅 Calendar: Found " + matched.size() + " appointments for " + selectedDate);

        filteredAppointments.setAll(matched);
        appointmentsTable.setItems(filteredAppointments);
        appointmentsTable.refresh();
    }
}
