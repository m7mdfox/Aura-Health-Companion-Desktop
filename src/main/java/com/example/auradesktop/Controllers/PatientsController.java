package com.example.auradesktop.Controllers;

import com.example.auradesktop.UserSession;
import com.example.auradesktop.models.Appointment;
import com.example.auradesktop.models.ChatMessage;
import com.example.auradesktop.services.DoctorApiService;
import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.time.Instant; // IMPORT THIS
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PatientsController implements Initializable {

    @FXML private VBox patientsListContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField messageInputField;
    @FXML private Label currentChatNameLabel;
    @FXML private Label currentChatStatusLabel;

    private Appointment activeAppointment;
    private Socket socket;
    private DoctorApiService apiService;
    private String currentDoctorId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        apiService = new DoctorApiService();
        currentDoctorId = UserSession.getInstance().getDoctorId();

        chatMessagesContainer.heightProperty().addListener((obs, old, val) -> chatScrollPane.setVvalue(1.0));

        messageInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onSendMessage();
        });

        initSocket();
        loadConfirmedAppointments();
    }

    private void loadConfirmedAppointments() {
        apiService.getAppointments(currentDoctorId).thenAccept(appointments -> {
            List<Appointment> confirmed = appointments.stream()
                    .filter(a -> "confirmed".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                patientsListContainer.getChildren().clear();
                for (Appointment appt : confirmed) {
                    patientsListContainer.getChildren().add(createContactNode(appt));
                }
            });
        });
    }

    private void initSocket() {
        try {
            URI uri = URI.create("http://localhost:4000");
            IO.Options options = IO.Options.builder().setTransports(new String[]{"websocket"}).build();
            socket = IO.socket(uri, options);

            socket.on(Socket.EVENT_CONNECT, args -> System.out.println("✅ Socket Connected"));

            socket.on("receive_message", args -> {
                JSONObject data = (JSONObject) args[0];
                Platform.runLater(() -> handleIncomingMessage(data));
            });

            socket.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleIncomingMessage(JSONObject data) {
        try {
            String room = data.optString("room");
            String senderId = data.optString("sender"); // This is the Patient's ID
            String content = data.optString("message");
            String timestamp = data.optString("timestamp", Instant.now().toString());

            if (activeAppointment == null) return;

            String currentRoomId = "room_" + activeAppointment.getPatientId() + "_" + currentDoctorId;

            // Ensure message is for this room and NOT sent by me
            if (room.equals(currentRoomId) && !senderId.equals(currentDoctorId)) {

                // --- FIX: Pass the 'senderId' to the new constructor ---
                // This prevents the app from thinking YOU sent it.
                ChatMessage msg = new ChatMessage(content, timestamp, senderId, false);

                appendMessage(msg);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || activeAppointment == null) return;

        // 1. Validate Patient ID (Fix for "Unknown" issue)
        String patientId = activeAppointment.getPatientId();
        if (patientId == null || patientId.isEmpty()) {
            System.err.println("❌ ERROR: Patient ID is missing. Cannot send message.");
            return;
        }

        String roomId = "room_" + patientId + "_" + currentDoctorId;

        // 2. Update UI (Optimistic)
        // For UI display, we use local time HH:mm
        String displayTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        appendMessage(new ChatMessage(text, displayTime, true));
        messageInputField.clear();

        // 3. Send to Server (FIXED DATE FORMAT)
        if (socket != null && socket.connected()) {
            JSONObject msgObj = new JSONObject();
            try {
                msgObj.put("room", roomId);
                msgObj.put("sender", currentDoctorId);
                msgObj.put("message", text);
                msgObj.put("type", "text");
                // --- FIX: Use ISO-8601 Standard ---
                msgObj.put("timestamp", Instant.now().toString());

                socket.emit("send_message", msgObj);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private HBox createContactNode(Appointment appt) {
        boolean isActive = isAppointmentActive(appt);

        HBox row = new HBox();
        row.getStyleClass().add("contact-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setPadding(new Insets(10));

        Circle statusDot = new Circle(5, isActive ? Color.web("#2ecc71") : Color.GRAY);

        VBox textBox = new VBox();
        Label nameLbl = new Label(appt.getPatientName());
        nameLbl.setStyle("-fx-font-weight: bold;");
        Label timeLbl = new Label(appt.getStartTime() + " - " + appt.getEndTime());
        textBox.getChildren().addAll(nameLbl, timeLbl);

        row.getChildren().addAll(statusDot, textBox);

        row.setOnMouseClicked(e -> {
            this.activeAppointment = appt;
            currentChatNameLabel.setText(appt.getPatientName());

            chatMessagesContainer.getChildren().clear();
            messageInputField.setDisable(!isActive);

            // Construct Room ID
            if (appt.getPatientId() == null || appt.getPatientId().isEmpty()) {
                System.out.println("❌ WARNING: Patient ID is empty. Chat will not work.");
                return;
            }

            String roomId = "room_" + appt.getPatientId() + "_" + currentDoctorId;
            System.out.println("Joining Room: " + roomId);

            if(socket != null) socket.emit("join_room", roomId);

            apiService.getChatHistory(roomId).thenAccept(messages -> {
                Platform.runLater(() -> {
                    for (ChatMessage msg : messages) {
                        appendMessage(msg);
                    }
                });
            });
        });

        return row;
    }

    // ... (Keep appendMessage, createBubble, isAppointmentActive as they were) ...
    private void appendMessage(ChatMessage msg) {
        HBox container = new HBox();
        container.setAlignment(msg.isSentByMe() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox();
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(msg.isSentByMe() ? "message-bubble-sent" : "message-bubble-received");

        Label textLbl = new Label(msg.getContent());
        textLbl.setWrapText(true);
        if(msg.isSentByMe()) {
            textLbl.setStyle("-fx-font-weight: bold;");
        }

        Label timeLbl = new Label(msg.getTimestamp());
        timeLbl.setFont(new Font(9));
        // Force color if CSS fails
        timeLbl.setTextFill(Color.web(msg.isSentByMe() ? "#e4e4e4" : "#6e7382"));

        bubble.getChildren().addAll(textLbl, timeLbl);
        container.getChildren().add(bubble);
        chatMessagesContainer.getChildren().add(container);
    }

    private boolean isAppointmentActive(Appointment appt) {
        // ... (Keep your existing date logic) ...
        try {
            ZonedDateTime utcDate = ZonedDateTime.parse(appt.getDate());
            LocalDateTime apptDateLocal = utcDate.withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();

            boolean isSameDay = apptDateLocal.toLocalDate().isEqual(now.toLocalDate());
            if (!isSameDay) return false;

            LocalTime startTime = LocalTime.parse(appt.getStartTime());
            LocalTime endTime = LocalTime.parse(appt.getEndTime());
            LocalTime nowTime = now.toLocalTime();

            return nowTime.isAfter(startTime.minusMinutes(1)) && nowTime.isBefore(endTime.plusMinutes(1));
        } catch (Exception e) { return false; }
    }
}