package com.example.auradesktop.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    private void loadView(String fxmlPath, ActionEvent event) {
        try {
            // Try to replace only the center content if the current root is a BorderPane (Main.fxml approach)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent rootCandidate = stage.getScene().getRoot();
            if (rootCandidate instanceof BorderPane) {
                // load the new content and set it in the center
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent content = loader.load();
                ((BorderPane) rootCandidate).setCenter(content);
            } else {
                // fallback: replace whole scene root
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void goHome(ActionEvent event) {
        loadView("/com/example/auradesktop/Home.fxml", event);
    }



    @FXML
    private MediaView mediaView;

    public void initialize() {
        playIntroVideo();
    }

    private void playIntroVideo() {
        try {
            String videoPath = getClass().getResource("jetbrains://idea/navigate/reference?project=Aura-Health-Companion-Desktop&path=com/example/auradesktop/videos/Modern A Letter Startup Company Logo (video-converter.com).mp4").toExternalForm();

            Media media = new Media(videoPath);
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video

            mediaView.setMediaPlayer(mediaPlayer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
