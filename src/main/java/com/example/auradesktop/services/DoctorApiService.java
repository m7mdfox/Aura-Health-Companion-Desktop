package com.example.auradesktop.services;

import com.example.auradesktop.models.Appointment;
import com.example.auradesktop.models.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DoctorApiService {

    private final String BASE_URL = "http://localhost:4000/api";
    private final HttpClient client;
    private final Gson gson;

    public DoctorApiService() {
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.gson = new Gson();
    }

    public CompletableFuture<List<Appointment>> getAppointments(String doctorId) {
        String endpoint = BASE_URL + "/appointments/doctor/" + doctorId;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // 1. PRINT THE RAW JSON
                    System.out.println("DEBUG JSON: " + response.body());

                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), new TypeToken<ArrayList<Appointment>>(){}.getType());
                    }
                    return new ArrayList<Appointment>();
                });
    }
    // ... keep the other methods (updateStatus, getChatHistory) as they were ...
    public CompletableFuture<Boolean> updateStatus(String appointmentId, String status) {
        String json = "{\"status\": \"" + status + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/appointments/" + appointmentId + "/status")) // Verify this route!
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200);
    }

    public CompletableFuture<List<ChatMessage>> getChatHistory(String roomId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/chat/history/" + roomId))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), new TypeToken<List<ChatMessage>>(){}.getType());
                    }
                    return new ArrayList<>();
                });
    }
}