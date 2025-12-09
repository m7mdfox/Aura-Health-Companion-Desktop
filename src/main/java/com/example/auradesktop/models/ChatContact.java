package com.example.auradesktop.models;

public class ChatContact {
    private String id;
    private String name;
    private String lastMessage;
    private String time;
    private int unreadCount;
    private boolean isOnline;

    public ChatContact(String id, String name, String lastMessage, String time, int unreadCount, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
    }
    // Getters
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isOnline() { return isOnline; }
    public String getId() { return id; }
}