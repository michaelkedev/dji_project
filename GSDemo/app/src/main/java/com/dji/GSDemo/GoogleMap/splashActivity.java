package com.dji.GSDemo.GoogleMap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

public class splashActivity extends AppCompatActivity {

    Handler handler;
    private ImageView ivLogo;
    private AlphaAnimation alphaAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initUI();
        ivLogo.setAlpha(0.1f);

        animation();

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(splashActivity.this, ConnectionActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }

    private void initUI(){
        ivLogo = findViewById(R.id.iv_logo);
    }

    private void animation(){

        alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(3000);
        alphaAnimation.setFillAfter(true);

        ivLogo.setImageResource(R.drawable.drone);
        ivLogo.setAlpha(1.0f);
        ivLogo.startAnimation(alphaAnimation);
    }
}