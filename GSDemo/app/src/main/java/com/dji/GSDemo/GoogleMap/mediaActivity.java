package com.dji.GSDemo.GoogleMap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

public class mediaActivity extends AppCompatActivity  implements View.OnClickListener{
    private Button btnDownload;
    private MediaManager mMediaManager;
    private MediaFile mMediaFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
    }
    public void initUI(){
        btnDownload = findViewById(R.id.btn_Download);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_Download:
                download();
                break;
            default:
                break;
        }
    }
    public void bind(MediaFile mediaFile){
        mMediaFile = mediaFile;
    }
    private void unsetMediaManager(){
    }
    private void download(){
        File downloadDir = new File(getExternalFilesDir(null)+"/dji_media");
    }

}