package com.dji.GSDemo.GoogleMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.OnClick;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.midware.data.model.P3.Ca;
import dji.midware.data.model.P3.S;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;

public class cameraActivity extends AppCompatActivity implements View.OnClickListener {
    private TextureView textureFPV;

    private Button btnBack, btnRecord, btnShoot;
    private TextView textViewCameraMode, textViewDebug;

    private DJICodecManager mCodeManager;
    private SettingsDefinitions.CameraMode mCameraMode;
    private Boolean mIsCameraRecording;
    private int mRecordingSec;
    private VideoFeeder.VideoDataListener mVideoDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        initUI();
        initListener();
        initCamera();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        removeListener();;
    }

    private void initUI(){
        textureFPV = findViewById(R.id.texture_fpv);

        btnBack = findViewById(R.id.btn_back);
        btnRecord = findViewById(R.id.btn_record);
        btnShoot = findViewById(R.id.btn_shoot);

        textViewDebug = findViewById(R.id.textView_debug);
        textViewCameraMode = findViewById(R.id.textView_camera_mode);

    }

    private void initCamera(){
        Camera camera = getCamera();
        if(camera!=null){
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(@NonNull SystemState systemState) {
                    mCameraMode = systemState.getMode();
                    mIsCameraRecording = systemState.isRecording();
                    mRecordingSec = systemState.getCurrentVideoRecordingTimeInSeconds();
                    updateCameraMode();
                }
            });
            showToast("Init Camera.");
        }
    }

    private void record(){
        showToast("Record.");
        final  Camera camera = getCamera();
        if(camera!=null){
            if(mCameraMode!= SettingsDefinitions.CameraMode.RECORD_VIDEO){
                setCameraMode(camera, SettingsDefinitions.CameraMode.RECORD_VIDEO);
                if(mCameraMode != SettingsDefinitions.CameraMode.RECORD_VIDEO)
                    showToast("Not Record Mode. ");
            }
//            updateCameraMode();
            textViewDebug.setText("Debug : Recording , Mode"+cameraModeToString(mCameraMode));
            if(mIsCameraRecording){
                stopRecording(camera);
                showToast("Start Recording.");
            }
            else{
                startRecording(camera);
                showToast("Stop Recroding.");
            }
        }
    }
    private void startRecording(Camera camera){
        getCamera().startRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError==null){
                    showToast("Successful to Start Recording. ");
                }
                else{
                    showToast("Error : "+ djiError.getDescription());
                }
            }
        });
    }

    private void stopRecording(Camera camera){
        getCamera().stopRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError==null){
                    showToast("Successful to Stop Recording. ");
                }
                else{
                    showToast("Error : "+ djiError.getDescription());
                }
            }
        });
    }
    private void takePicture(){
        showToast("take picture.");
        final Camera camera = getCamera();
        if(camera != null){
//            Turns the mode into "SHOOT_PHOTO"
            if(mCameraMode!=SettingsDefinitions.CameraMode.SHOOT_PHOTO){
                setCameraMode(camera, SettingsDefinitions.CameraMode.SHOOT_PHOTO);

//                updateCameraMode();
                textViewDebug.setText("Debug : take Picture()"+cameraModeToString(mCameraMode));

                if(mCameraMode != SettingsDefinitions.CameraMode.SHOOT_PHOTO){
                    showToast("Not Under SHOOT Mode. Try Again.");
                    return;
                }
            }

            camera.setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError == null){
                        camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError == null)
                                    showToast("Successful !");
                                else
                                    showToast("Error : "+djiError.getDescription());
                            }
                        });
                    }
                }
            });
        }
        else
            showToast("Empty");
    }

    private void setCameraMode(Camera camera, final SettingsDefinitions.CameraMode cameraMode){
        camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError == null)
                {
                    showToast("Successful Current Mode : " + cameraModeToString(cameraMode) );
//                    updateCameraMode();
                }
                else
                    showToast("Error : "+ djiError.getDescription());
            }
        });
    }
    private void showToast(String s) {
        Context context = getApplicationContext();
        CharSequence text = s;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private void updateCameraMode(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String cameraModeString = cameraModeToString(mCameraMode);
                textViewCameraMode.setText("Mode : "+cameraModeString);
                if(mIsCameraRecording){
                    btnRecord.setText("Stop");
                    textViewDebug.setText("Debug : Recording , "+mRecordingSec + " sec");
                }
                else{
                    btnRecord.setText("Start");
                }
            }
        });
    }
    private String cameraModeToString(SettingsDefinitions.CameraMode cameraMode){
        switch (cameraMode){
            case SHOOT_PHOTO:
                return "Shoot Mode";
            case RECORD_VIDEO:
                return "Record Mode";
            case PLAYBACK:
                return "Playback Mode";
            case MEDIA_DOWNLOAD:
                return "Media Download Mode";
            case BROADCAST:
                return "Broadcast Mode";
            case UNKNOWN:
                return "!!!!!!!!!!!UNKOWN!!!!!!!!!!!";
            default:
                return "Nothing";
        }
    }
    private void initListener(){

        textureFPV.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                if(mCodeManager == null){
                    mCodeManager = new DJICodecManager(cameraActivity.this, surface, width, height);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                if(mCodeManager != null){
                    mCodeManager.cleanSurface();
                    mCodeManager = null ;
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });

        btnBack.setOnClickListener(this);
        btnShoot.setOnClickListener(this);
        btnRecord.setOnClickListener(this);

        mVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int i) {
                if(mCodeManager != null){
                    mCodeManager.sendDataToDecoder(bytes, i);
                }
            }
        };

        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mVideoDataListener);

    }


    private void removeListener(){

        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mVideoDataListener);

        Camera camera = getCamera();
        if(camera!=null)
            camera.setSystemStateCallback(null);
    }
    public void onClick(View v){
        switch(v.getId()){
            case R.id.btn_back:
                back();
                break;
            case R.id.btn_shoot:
                textViewDebug.setText("Debug : Shooting .");
                showToast("shoot");
                takePicture();
                break;
            case R.id.btn_record:
                record();
                break;
            default:
                break;
        }
    }
//    Back to waypoint1 page
    private void back(){
        startActivity(cameraActivity.this, Waypoint1Activity.class);
        this.finish();
    }
    public static void startActivity(Context context, Class activity) {
        Intent intent = new Intent(context, activity);
        context.startActivity(intent);
    }

    public static Camera getCamera(){
        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if(product != null && product.isConnected()){
            return product.getCamera();
        }
        return null;
    }
}