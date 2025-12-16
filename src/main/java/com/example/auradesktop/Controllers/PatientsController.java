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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PatientsController implements Initializable {

    @FXML
    private VBox patientsListContainer;
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatMessagesContainer;
    @FXML
    private TextField messageInputField;
    @FXML
    private Label currentChatNameLabel;

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
            if (event.getCode() == KeyCode.ENTER)
                onSendMessage();
        });

        initSocket();
        loadConfirmedAppointments();
    }

    // In PatientsController.java

    private void loadConfirmedAppointments() {
        apiService.getAppointments(currentDoctorId).thenAccept(appointments -> {
            Platform.runLater(() -> {
                patientsListContainer.getChildren().clear();

                if (appointments == null || appointments.isEmpty()) {
                    patientsListContainer.getChildren().add(new Label("No active patients found."));
                } else {
                    for (Appointment appt : appointments) {
                        // DEBUG: Print parsed values
                        System.out.println("📋 Appointment ID: " + appt.getId());
                        System.out.println("   Patient ID: " + appt.getPatientId());
                        System.out.println("   Patient Name: " + appt.getPatientName());
                        System.out.println("   Status: " + appt.getStatus());

                        // Only check status. The ID and Name are guaranteed by the server now.
                        if ("confirmed".equalsIgnoreCase(appt.getStatus())) {
                            patientsListContainer.getChildren().add(createContactNode(appt));
                        }
                    }
                }
            });
        });
    }

    private void initSocket() {
        try {
            URI uri = URI.create("http://localhost:4000");
            IO.Options options = IO.Options.builder().setTransports(new String[] { "websocket" }).build();
            socket = IO.socket(uri, options);

            socket.on(Socket.EVENT_CONNECT, args -> System.out.println("✅ Socket Connected"));
            socket.on(Socket.EVENT_DISCONNECT, args -> System.out.println("❌ Socket Disconnected"));

            // Log ALL incoming events for debugging
            socket.onAnyIncoming(args -> {
                if (args != null && args.length > 0) {
                    System.out.println("📡 Socket Incoming: " + args[0]);
                }
            });

            socket.on("receive_message", args -> {
                System.out.println("📩 Received message from socket!");
                JSONObject data = (JSONObject) args[0];
                System.out.println("   Data: " + data.toString());
                Platform.runLater(() -> handleIncomingMessage(data));
            });

            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // In PatientsController.java

    private void handleIncomingMessage(JSONObject data) {
        try {
            // 1. Extract basic fields
            String room = data.optString("room");
            String senderId = data.optString("sender");
            String content = data.optString("message", "");
            String timestamp = data.optString("timestamp", Instant.now().toString());

            System.out.println("🔍 Processing incoming message:");
            System.out.println("   Room: " + room);
            System.out.println("   Sender: " + senderId);
            System.out.println("   Content: " + content);

            // 2. Extract and Fix Image URL
            String rawImageUrl = data.optString("imageUrl", "");
            String finalImageUrl = "";

            if (!rawImageUrl.isEmpty()) {
                finalImageUrl = rawImageUrl.replace("10.0.2.2", "localhost");
            }

            // 3. Smart Type Detection
            String rawType = data.optString("type", "text");
            String type = (!finalImageUrl.isEmpty()) ? "image" : rawType;

            // 4. Validate context (Are we in the right chat?)
            if (activeAppointment == null) {
                System.out.println("   ❌ Rejected: No active appointment");
                return;
            }

            String currentRoomId = "room_" + activeAppointment.getPatientId() + "_" + currentDoctorId;
            System.out.println("   Expected Room: " + currentRoomId);
            System.out.println("   Doctor ID: " + currentDoctorId);

            // Ensure message is for this room and NOT sent by me
            boolean roomMatch = room.equals(currentRoomId);
            boolean notFromMe = !senderId.equals(currentDoctorId);

            System.out.println("   Room match: " + roomMatch);
            System.out.println("   Not from me: " + notFromMe);

            if (roomMatch && notFromMe) {
                ChatMessage msg;

                // 5. Create the correct object
                if ("image".equalsIgnoreCase(type) && !finalImageUrl.isEmpty()) {
                    msg = new ChatMessage(content, timestamp, senderId, false, finalImageUrl);
                } else {
                    msg = new ChatMessage(content, timestamp, senderId, false);
                }

                // 6. Update UI
                System.out.println("   ✅ Adding message to UI");
                appendMessage(msg);
            } else {
                System.out.println("   ❌ Message filtered out (room mismatch or sent by me)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty() || activeAppointment == null)
            return;

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
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                if (!msg.isSentByMe())
                    caption.setTextFill(Color.BLACK);
                bubble.getChildren().add(caption);
            }

        } else {
            // ---> TEXT RENDERER <---
            // Fallback: If for some reason the type is "image" but URL is missing, it will
            // print the text here.
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

    private void loadChatHistory(String roomId) {
        System.out.println("📜 Loading chat history for room: " + roomId);

        apiService.getChatHistory(roomId).thenAccept(messages -> {
            Platform.runLater(() -> {
                if (messages == null || messages.isEmpty()) {
                    System.out.println("No previous messages found.");
                    return;
                }

                System.out.println("📨 Loaded " + messages.size() + " previous messages");
                for (ChatMessage msg : messages) {
                    appendMessage(msg);
                }
            });
        }).exceptionally(ex -> {
            System.out.println("❌ Error loading chat history: " + ex.getMessage());
            return null;
        });
    }

    private HBox createContactNode(Appointment appt) {
        // 1. Run the strict check
        boolean isActive = isAppointmentActive(appt);

        HBox row = new HBox();
        row.getStyleClass().add("contact-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setPadding(new Insets(10));

        // 2. Visual Indicator (Green if Active, Gray if Inactive)
        Circle statusDot = new Circle(5, isActive ? Color.web("#2ecc71") : Color.GRAY);

        VBox textBox = new VBox();
        Label nameLbl = new Label(appt.getPatientName());
        nameLbl.setStyle("-fx-font-weight: bold;");

        // Show time window
        Label timeLbl = new Label(appt.getStartTime() + " - " + appt.getEndTime());
        textBox.getChildren().addAll(nameLbl, timeLbl);

        row.getChildren().addAll(statusDot, textBox);

        // 3. Click Logic: Only allow chat if Active
        row.setOnMouseClicked(e -> {
            if (!isActive) {
                System.out.println("⛔ Chat unavailable: Outside of appointment time.");
                return;
            }

            // Set the active appointment
            this.activeAppointment = appt;
            currentChatNameLabel.setText(appt.getPatientName());

            // Clear previous chat messages
            chatMessagesContainer.getChildren().clear();

            // Build the room ID
            String roomId = "room_" + appt.getPatientId() + "_" + currentDoctorId;
            System.out.println("🔗 Joining room: " + roomId);

            // Join the socket room to receive messages
            if (socket != null && socket.connected()) {
                try {
                    // Emit room ID directly as string - server expects this format
                    socket.emit("join_room", roomId);
                    System.out.println("✅ Joined room successfully: " + roomId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                System.out.println("❌ Socket not connected!");
            }

            // Load chat history
            loadChatHistory(roomId);
        });

        // 4. Disable row visually (Optional but recommended)
        if (!isActive) {
            row.setOpacity(0.5); // Dim the row
        }

        return row;
    }

    private boolean isAppointmentActive(Appointment appt) {
        // 1. Status Check
        if (appt == null || !"confirmed".equalsIgnoreCase(appt.getStatus())) {
            return false;
        }

        try {
            // 2. Date Check - Handle UTC timezone properly
            // The date comes as ISO format: "2025-12-15T22:00:00.000Z"
            String dateStr = appt.getDate();
            LocalDate apptDate;

            if (dateStr.contains("T") && dateStr.endsWith("Z")) {
                // Parse as UTC instant and convert to local date
                java.time.Instant instant = java.time.Instant.parse(dateStr);
                apptDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else {
                // Fallback: just take the date portion
                apptDate = LocalDate.parse(dateStr.substring(0, 10));
            }

            LocalDate today = LocalDate.now();

            // DEBUG: Print date comparison
            System.out.println("📅 Date check: Appointment=" + apptDate + ", Today=" + today);

            if (!apptDate.isEqual(today)) {
                System.out.println("❌ Date mismatch!");
                return false;
            }

            // 3. Time Window Check
            LocalTime now = LocalTime.now();

            // Parse "HH:mm" strings
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.parse(appt.getStartTime(), timeFormatter);
            LocalTime end = LocalTime.parse(appt.getEndTime(), timeFormatter);

            // DEBUG: Print time comparison
            System.out.println("⏰ Time check: Now=" + now.format(timeFormatter) +
                    ", Window=" + appt.getStartTime() + "-" + appt.getEndTime());

            // STRICT CHECK: Now >= Start AND Now <= End
            boolean isAfterStart = !now.isBefore(start);
            boolean isBeforeEnd = !now.isAfter(end);

            boolean isActive = isAfterStart && isBeforeEnd;
            System.out.println("✅ Is Active: " + isActive);

            return isActive;

        } catch (Exception e) {
            System.err.println("Date/Time logic error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
