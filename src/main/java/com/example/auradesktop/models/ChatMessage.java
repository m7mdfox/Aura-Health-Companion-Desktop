package com.example.auradesktop.models;

public class ChatMessage {
    private String content;
    private String timestamp;
    private boolean isSentByMe; // true if I sent it, false if I received it

    public ChatMessage(String content, String timestamp, boolean isSentByMe) {
        this.content = content;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
    }

    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public boolean isSentByMe() { return isSentByMe; }
}