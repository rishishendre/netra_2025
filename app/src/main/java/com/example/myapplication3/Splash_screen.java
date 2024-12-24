package com.example.myapplication3;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Splash_screen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        VideoView videoView = findViewById(R.id.videoView);
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(currentMode==Configuration.UI_MODE_NIGHT_YES){
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.netra_splashscreen_dark);
            videoView.setVideoURI(videoUri);

        }
        else{
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.netra_splashscreen_light);
            videoView.setVideoURI(videoUri);


        }
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Navigate to the next activity
                startActivity(new Intent(Splash_screen.this, Get_Started.class));
                finish();
            }
        });
        videoView.start();
    }
}