package com.example.auradesktop.models;

import com.google.gson.annotations.SerializedName;
import com.example.auradesktop.UserSession;

public class ChatMessage {

    @SerializedName("message")
    private String content;

    @SerializedName("sender")
    private String senderId;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("type")
    private String type; // "text" or "image"

    @SerializedName(value = "imageUrl", alternate={"image_url", "image"})
    private String imageUrl;

    // Internal flag for UI (not in DB)
    private boolean isSentByMe;

    // --- 1. Constructor for Sending (UI -> Server) ---
    // Used when YOU send a text message
    public ChatMessage(String content, String timestamp, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
        this.senderId = UserSession.getInstance().getDoctorId();
        this.type = "text";
    }

    // --- 2. Constructor for Receiving Text ---
    // Used when receiving text from Patient
    public ChatMessage(String content, String timestamp, String senderId, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.isSentByMe = isSentByMe;
        this.type = "text";
    }

    // --- 3. Constructor for Receiving Images ---
    // Used when receiving an image from Patient
    public ChatMessage(String content, String timestamp, String senderId, boolean isSentByMe, String imageUrl) {
        this.content = content; // Can be a caption or empty
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.isSentByMe = isSentByMe;
        this.type = "image";     // <--- Set Type
        this.imageUrl = imageUrl; // <--- Store URL
    }

    // --- Getters ---
    public String getContent() { return content; }
    public String getSenderId() { return senderId; }
    public String getType() {
        // FIX: If we have an image URL, this IS an image message,
        // regardless of what the 'type' field says.
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return "image";
        }
        return type;
    }
// In ChatMessage.java

    public String getImageUrl() {
        if (imageUrl == null) return null;

        // FIX: Android sends '10.0.2.2', but Desktop needs 'localhost'
        return imageUrl.replace("10.0.2.2", "localhost");
    }
    public String getTimestamp() {
        try {
            if (timestamp != null && timestamp.length() > 10) {
                // Simple parser assuming ISO format (2025-12-12T10:00...)
                return timestamp.substring(11, 16);
            }
        } catch (Exception e) {
            // ignore
        }
        return timestamp != null ? timestamp : "";
    }

    // --- CRITICAL LOGIC: Left vs Right Bubble ---
    public boolean isSentByMe() {
        if (isSentByMe) return true;
        // Check sender ID against logged-in Doctor ID
        String myId = UserSession.getInstance().getDoctorId();
        return senderId != null && senderId.equals(myId);
    }
}