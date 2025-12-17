package com.example.auradesktop.models;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.example.auradesktop.utils.MongoDateDeserializer;
import com.example.auradesktop.utils.MongoObjectIdDeserializer;

public class Appointment {

    @SerializedName("_id")
    @JsonAdapter(MongoObjectIdDeserializer.class)
    private String id;

    // Handles both plain string and MongoDB ObjectId format {"$oid": "..."}
    @SerializedName("patient_id")
    @JsonAdapter(MongoObjectIdDeserializer.class)
    private String patientId;

    // FIX 2: This matches the new JSON field from the server
    @SerializedName("patient_name")
    private String patientName;

    // FIX 3: Receive email as well if you need it
    @SerializedName("patient_email")
    private String patientEmail;

    // Handles MongoDB date format {"$date": "..."}
    @SerializedName("appointment_date")
    @JsonAdapter(MongoDateDeserializer.class)
    private String date;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("status")
    private String status;

    @SerializedName("notes")
    private String notes;

    // --- Simple Getters (No more inner classes) ---

    public String getPatientName() {
        return (patientName != null) ? patientName : "Unknown";
    }

    public String getPatientId() {
        return patientId;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return (notes != null) ? notes : "";
    }

    /**
     * Helper method for calendar display - returns "HH:mm - HH:mm" format
     */
    public String getTimeRange() {
        return startTime + " - " + endTime;
    }
}