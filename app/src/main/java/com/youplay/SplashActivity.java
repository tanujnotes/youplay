package com.youplay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private String sharedVideoId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ((MyApp) getApplication()).getNetComponent().inject(this);
        setContentView(R.layout.activity_splash);

        Intent intent = getIntent();
        try { // getting video id from youtube links
            String url = intent.getDataString();
            if (url.contains("youtu.be"))
                sharedVideoId = url.split("be/")[1];
            else
                sharedVideoId = intent.getData().getQueryParameter("v");
        } catch (Exception e) {
            e.printStackTrace();
        }

        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("shared_video_id", sharedVideoId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}