package com.example.auradesktop.Controllers;

import com.example.auradesktop.models.ChatContact; // Make sure package name matches your project (Models vs models)
import com.example.auradesktop.models.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class PatientsController implements Initializable {

    // --- FXML Bindings ---
    @FXML private VBox patientsListContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField messageInputField;
    @FXML private Label currentChatNameLabel;
    @FXML private Label currentChatStatusLabel;

    // --- State ---
    private ChatContact activeContact;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Auto-scroll to bottom
        chatMessagesContainer.heightProperty().addListener((obs, old, val) -> chatScrollPane.setVvalue(1.0));

        // Enter key listener
        messageInputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onSendMessage();
        });

        // --- BACKEND DEVELOPER: REMOVE THIS TEST DATA LATER ---
        loadTestData();
    }

    // ==========================================================
    //                 API FOR BACKEND DEVELOPER
    // ==========================================================

    public void loadContactList(List<ChatContact> contacts) {
        patientsListContainer.getChildren().clear();
        for (ChatContact contact : contacts) {
            HBox contactNode = createContactNode(contact);
            patientsListContainer.getChildren().add(contactNode);
        }
    }

    public void loadChatMessages(List<ChatMessage> messages) {
        chatMessagesContainer.getChildren().clear();

        // Add "Today" label separator
        Label dateLabel = new Label("Today");
        dateLabel.setTextFill(Color.web("#b2b2b2"));
        dateLabel.setFont(new Font(10));
        dateLabel.setMaxWidth(Double.MAX_VALUE);
        dateLabel.setAlignment(Pos.CENTER);
        chatMessagesContainer.getChildren().add(dateLabel);

        for (ChatMessage msg : messages) {
            appendMessage(msg);
        }
    }

    public void appendMessage(ChatMessage msg) {
        HBox container = new HBox();
        container.setAlignment(msg.isSentByMe() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        VBox bubble = createBubble(msg);
        container.getChildren().add(bubble);

        chatMessagesContainer.getChildren().add(container);
    }

    // ==========================================================
    //                 UI GENERATION LOGIC
    // ==========================================================

    @FXML
    private void onSendMessage() {
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        String time = new SimpleDateFormat("HH:mm").format(new Date());

        // 1. Update UI immediately
        ChatMessage myMsg = new ChatMessage(text, time, true);
        appendMessage(myMsg);
        messageInputField.clear();

        // 2. TODO: SEND TO BACKEND HERE
    }

    // --- Sidebar Item Builder ---
    private HBox createContactNode(ChatContact contact) {
        HBox row = new HBox();
        row.getStyleClass().add("contact-item");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setPadding(new Insets(10));

        // Avatar Area
        AnchorPane avatarPane = new AnchorPane();
        Circle baseCircle = new Circle(20, Color.TRANSPARENT);
        baseCircle.setStroke(Color.web("#e1e1e1"));
        Circle statusDot = new Circle(5, contact.isOnline() ? Color.web("#2ecc71") : Color.GRAY);
        statusDot.setStroke(Color.WHITE);
        AnchorPane.setRightAnchor(statusDot, 0.0);
        AnchorPane.setTopAnchor(statusDot, 0.0);
        avatarPane.getChildren().addAll(baseCircle, statusDot);

        // Text Area
        VBox textBox = new VBox();
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox topRow = new HBox(5);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(contact.getName());
        nameLbl.setStyle("-fx-font-weight: bold;");
        // NOTE: I removed hardcoded colors here so CSS can control it if needed.

        Label timeLbl = new Label(contact.getTime());
        timeLbl.setTextFill(Color.web("#9da3ae"));
        timeLbl.setFont(new Font(10));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(nameLbl, spacer, timeLbl);

        HBox bottomRow = new HBox();
        Label msgLbl = new Label(contact.getLastMessage());
        msgLbl.setTextFill(Color.web("#6e7382"));

        Pane spacer2 = new Pane();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        bottomRow.getChildren().addAll(msgLbl, spacer2);

        // Unread Badge
        if (contact.getUnreadCount() > 0) {
            Label badge = new Label(String.valueOf(contact.getUnreadCount()));
            badge.setStyle("-fx-background-color: #0944ef; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10;");
            badge.setMinSize(18, 18);
            badge.setAlignment(Pos.CENTER);
            bottomRow.getChildren().add(badge);
        }

        textBox.getChildren().addAll(topRow, bottomRow);
        row.getChildren().addAll(avatarPane, textBox);

        // Click Event
        row.setOnMouseClicked(e -> {
            this.activeContact = contact;
            currentChatNameLabel.setText(contact.getName());
            currentChatStatusLabel.setText(contact.isOnline() ? "Online" : "Offline");
            currentChatStatusLabel.setTextFill(contact.isOnline() ? Color.web("#2ecc71") : Color.GRAY);

            // Clear chat for demo
            chatMessagesContainer.getChildren().clear();
        });

        return row;
    }

    // --- Message Bubble Builder ---
    private VBox createBubble(ChatMessage msg) {
        VBox bubble = new VBox();
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(msg.isSentByMe() ? "message-bubble-sent" : "message-bubble-received");

        Label textLbl = new Label(msg.getContent());
        textLbl.setWrapText(true);

        // --- CHANGE MADE: REMOVED Manual Color Setting ---
        // textLbl.setTextFill(...) was removed here.
        // Now CSS (.message-bubble-received .label) will make text dark,
        // and (.message-bubble-sent .label) will make text white.

        if(msg.isSentByMe()) {
            textLbl.setStyle("-fx-font-weight: bold;");
        }

        Label timeLbl = new Label(msg.getTimestamp());
        timeLbl.setFont(new Font(9));
        // We keep the time color soft gray, but ensure it's visible
        timeLbl.setTextFill(Color.web(msg.isSentByMe() ? "#e4e4e4" : "#6e7382"));
        timeLbl.setAlignment(Pos.BOTTOM_RIGHT);
        timeLbl.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(timeLbl, new Insets(5, 0, 0, 0));

        bubble.getChildren().addAll(textLbl, timeLbl);
        return bubble;
    }

    private void loadTestData() {
        List<ChatContact> contacts = new ArrayList<>();
        contacts.add(new ChatContact("1", "Patient Name", "Awesome!", "16:45", 2, true));
        contacts.add(new ChatContact("2", "Dr. Sarah", "Please check file.", "12:30", 0, false));
        loadContactList(contacts);

        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(new ChatMessage("Hello, are the results ready?", "15:42", false));
        msgs.add(new ChatMessage("Yes, sending them now.", "15:44", true));
        loadChatMessages(msgs);
    }
}