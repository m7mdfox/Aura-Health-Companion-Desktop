package com.example.auradesktop.Controllers;

import javafx.fxml.FXML;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class LoginController {

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
