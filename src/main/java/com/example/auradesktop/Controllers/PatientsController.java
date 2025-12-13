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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PatientsController implements Initializable {

    @FXML private VBox patientsListContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField messageInputField;
    @FXML private Label currentChatNameLabel;

    private Appointment activeAppointment;
    private Socket socket;
    private DoctorApiService apiService;
    private String currentDoctorId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        apiService = new DoctorApiService();
        currentDoctorId = UserSession.getInstance().getDoctorId();

        // Auto-scroll logic
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

    // In PatientsController.java

    private void handleIncomingMessage(JSONObject data) {
        try {
            // 1. Extract basic fields
            String room = data.optString("room");
            String senderId = data.optString("sender");
            String content = data.optString("message", "");
            String timestamp = data.optString("timestamp", Instant.now().toString());

            // 2. Extract and Fix Image URL
            String rawImageUrl = data.optString("imageUrl", "");
            String finalImageUrl = "";

            if (!rawImageUrl.isEmpty()) {
                // CRITICAL FIX: Android sends "10.0.2.2", but your PC (JavaFX)
                // needs "localhost" to see the file.
                finalImageUrl = rawImageUrl.replace("10.0.2.2", "localhost");
            }

            // 3. Smart Type Detection
            // If we have a valid URL, treat it as an image, regardless of what 'type' says
            String rawType = data.optString("type", "text");
            String type = (!finalImageUrl.isEmpty()) ? "image" : rawType;

            // 4. Validate context (Are we in the right chat?)
            if (activeAppointment == null) return;

            String currentRoomId = "room_" + activeAppointment.getPatientId() + "_" + currentDoctorId;

            // Ensure message is for this room and NOT sent by me
            if (room.equals(currentRoomId) && !senderId.equals(currentDoctorId)) {
                ChatMessage msg;

                // 5. Create the correct object
                if ("image".equalsIgnoreCase(type) && !finalImageUrl.isEmpty()) {
                    // Use Constructor 3 (Image)
                    msg = new ChatMessage(content, timestamp, senderId, false, finalImageUrl);
                } else {
                    // Use Constructor 2 (Text)
                    msg = new ChatMessage(content, timestamp, senderId, false);
                }

                // 6. Update UI
                appendMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || activeAppointment == null) return;

        String patientId = activeAppointment.getPatientId();
        if (patientId == null || patientId.isEmpty()) {
            System.err.println("❌ ERROR: Patient ID is missing.");
            return;
        }

        String roomId = "room_" + patientId + "_" + currentDoctorId;
        String displayTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // Add to UI
        appendMessage(new ChatMessage(text, displayTime, true));
        messageInputField.clear();

        if (socket != null && socket.connected()) {
            JSONObject msgObj = new JSONObject();
            try {
                msgObj.put("room", roomId);
                msgObj.put("sender", currentDoctorId);
                msgObj.put("message", text);
                msgObj.put("type", "text");
                msgObj.put("timestamp", Instant.now().toString());
                socket.emit("send_message", msgObj);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void appendMessage(ChatMessage msg) {
        HBox container = new HBox();
        container.setAlignment(msg.isSentByMe() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = new VBox();
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(msg.isSentByMe() ? "message-bubble-sent" : "message-bubble-received");

        // --- 1. RENDER CONTENT (Text vs Image) ---
        // Check if it's an image message with a valid URL
        if ("image".equalsIgnoreCase(msg.getType()) && msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {

            // ---> IMAGE RENDERER <---
            try {
                // FIX 1: Swap Android Emulator IP (10.0.2.2) with localhost for Desktop
                String fixedUrl = msg.getImageUrl().replace("10.0.2.2", "localhost");

                // Load in background (true)
                Image img = new Image(fixedUrl, true);
                ImageView imageView = new ImageView(img);

                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                // Optional: Open browser on click
                imageView.setOnMouseClicked(e -> System.out.println("Image clicked: " + fixedUrl));

                bubble.getChildren().add(imageView);
            } catch (Exception e) {
                bubble.getChildren().add(new Label("[Image Error]"));
            }

            // ---> CAPTION LOGIC (FIX 2) <---
            // Only add the caption if it is NOT the default "Sent an image" placeholder
            String content = msg.getContent();
            if (content != null && !content.isEmpty() && !content.trim().equalsIgnoreCase("Sent an image")) {
                Label caption = new Label(content);
                if (!msg.isSentByMe()) caption.setTextFill(Color.BLACK);
                bubble.getChildren().add(caption);
            }

        } else {
            // ---> TEXT RENDERER <---
            // Fallback: If for some reason the type is "image" but URL is missing, it will print the text here.
            Label textLbl = new Label(msg.getContent());
            textLbl.setWrapText(true);
            if (msg.isSentByMe()) {
                textLbl.setStyle("-fx-font-weight: bold;");
            } else {
                textLbl.setTextFill(Color.BLACK);
            }
            bubble.getChildren().add(textLbl);
        }

        // --- 2. RENDER TIMESTAMP ---
        Label timeLbl = new Label(msg.getTimestamp());
        timeLbl.setFont(new Font(9));
        timeLbl.setTextFill(Color.web(msg.isSentByMe() ? "#e4e4e4" : "#6e7382"));

        bubble.getChildren().add(timeLbl);
        container.getChildren().add(bubble);
        chatMessagesContainer.getChildren().add(container);
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

            if (appt.getPatientId() == null) return;
            String roomId = "room_" + appt.getPatientId() + "_" + currentDoctorId;

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

    private boolean isAppointmentActive(Appointment appt) {
        // ... (Keep existing date check logic) ...
        return true; // Simplified for brevity, use your existing logic
    }
}