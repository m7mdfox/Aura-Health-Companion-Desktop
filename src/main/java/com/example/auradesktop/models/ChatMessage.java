package com.example.auradesktop.models;

import com.google.gson.annotations.SerializedName;
import com.example.auradesktop.UserSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessage {

    // Matches MongoDB field "message"
    @SerializedName("message")
    private String content;

    // Matches MongoDB field "sender"
    @SerializedName("sender")
    private String senderId;

    // Matches MongoDB field "timestamp"
    @SerializedName("timestamp")
    private String timestamp;

    // Matches MongoDB field "type" (text/image)
    @SerializedName("type")
    private String type;

    // Matches MongoDB field "imageUrl" (optional)
    @SerializedName("imageUrl")
    private String imageUrl;

    // Internal flag for UI (not in DB)
    private boolean isSentByMe;

    // --- Constructor for Sending (UI -> Server) ---
    public ChatMessage(String content, String timestamp, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
        this.senderId = UserSession.getInstance().getDoctorId();
        this.type = "text";
    }

    public ChatMessage(String content, String timestamp, String senderId, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.senderId = senderId; // <--- Sets the ACTUAL sender (The Patient)
        this.isSentByMe = isSentByMe;
        this.type = "text";
    }

    // --- Getters ---
    public String getContent() { return content; }
    public String getSenderId() { return senderId; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }

    public String getTimestamp() {
        // Try to format the ISO string (2025-12-12T10:00:00) to HH:mm
        try {
            if (timestamp != null && timestamp.length() > 10) {
                // Simple parser assuming ISO format
                String timePart = timestamp.substring(11, 16);
                return timePart;
            }
        } catch (Exception e) {
            // ignore
        }
        return timestamp != null ? timestamp : "";
    }

    // --- CRITICAL LOGIC: Left vs Right Bubble ---
    public boolean isSentByMe() {
        // If we set it manually (when typing), return true
        if (isSentByMe) return true;

        // Otherwise check the ID against the logged-in Doctor
        String myId = UserSession.getInstance().getDoctorId();
        return senderId != null && senderId.equals(myId);
    }
}