package com.example.auradesktop.Controllers;

import com.example.auradesktop.UserSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class LoginController {
    @FXML private MediaView mediaView;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel; // Add this to your FXML to show errors

    private final String API_URL = "http://localhost:4000/api/doctors/login";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public void initialize() {
       // playIntroVideo();
        // Optional: Allow pressing "Enter" to login
        passwordField.setOnAction(event -> handleLogin());
    }

    //private void playIntroVideo() {
        //try {
            // FIXED: Use relative path. Ensure the video is in resources/com/example/auradesktop/videos/
            // If the file name has spaces, URI encoding might be needed, but simple names are safer.
            //String videoPath = getClass().getResource("/com/example/auradesktop/videos/Modern A Letter Startup Company Logo (video-converter.com).mp4").toExternalForm();

            //Media media = new Media(videoPath );
           // MediaPlayer mediaPlayer = new MediaPlayer(media);

           // mediaPlayer.setAutoPlay(true);
           // mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            //mediaView.setMediaPlayer(mediaPlayer);

       // } catch (Exception e) {
         //   System.err.println("Video failed to load: " + e.getMessage());
      //  }
   // }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        disableControls(true); // Prevent double clicking

        // 1. Create JSON Payload
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        String jsonBody = gson.toJson(data);

        // 2. Build Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // 3. Send Async Request
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::processResponse)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showError("Server error: " + e.getMessage());
                        disableControls(false);
                    });
                    return null;
                });
    }

    private void processResponse(HttpResponse<String> response) {
        Platform.runLater(() -> {
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 200) {
                // Success!
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    String token = json.get("token").getAsString();
                    JsonObject doctorObj = json.get("doctor").getAsJsonObject();

                    // Save to Session
                    UserSession session = UserSession.getInstance();
                    session.setToken(token);
                    session.setDoctorId(doctorObj.get("_id").getAsString());
                    session.setDoctorName(doctorObj.get("name").getAsString());

                    System.out.println("Login Successful for: " + session.getDoctorName());

                    // Navigate to Dashboard
                    goToDashboard();

                } catch (Exception e) {
                    showError("Error parsing server response.");
                    e.printStackTrace();
                }
            } else {
                // Login Failed (400, 404, etc)
                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    String errorMsg = json.has("error") ? json.get("error").getAsString() : "Login failed";
                    showError(errorMsg);
                } catch (Exception e) {
                    showError("Invalid credentials.");
                }
            }
            disableControls(false);
        });
    }

    private void goToDashboard() {
        try {
            // Load your main dashboard FXML here
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auradesktop/Home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Could not load Dashboard.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red;");
        } else {
            System.out.println("Error: " + message);
        }
    }

    private void disableControls(boolean disable) {
        loginButton.setDisable(disable);
        emailField.setDisable(disable);
        passwordField.setDisable(disable);
    }
}