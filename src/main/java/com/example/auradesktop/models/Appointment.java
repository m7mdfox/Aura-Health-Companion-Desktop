package com.example.auradesktop.models;

import com.google.gson.annotations.SerializedName;

public class Appointment {

    // Maps JSON "_id" to Java "id"
    @SerializedName("_id")
    private String id;

    // Maps JSON "appointment_date" to Java "date"
    @SerializedName("appointment_date")
    private String date;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("status")
    private String status;

    @SerializedName("type")
    private String type;

    // Maps JSON "patient_id" (which is an Object because of .populate)
    @SerializedName("patient_id")
    private PatientInfo patient;

    // --- Inner Class for Patient Info ---

    public static class PatientInfo {
        @SerializedName("_id")
        private String id;

        // --- FIX: Map database "full_name" to Java "name" ---
        @SerializedName("full_name")
        private String name;

        public String getName() { return name; }
        public String getId() { return id; }
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getType() { return type; }

    // --- Helpers ---
    public String getPatientName() {
        return (patient != null) ? patient.getName() : "Unknown";
    }

    public String getPatientId() {
        return (patient != null) ? patient.getId() : "";
    }
}