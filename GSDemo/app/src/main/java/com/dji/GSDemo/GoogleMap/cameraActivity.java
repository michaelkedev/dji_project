package com.dji.GSDemo.GoogleMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;


public class cameraActivity extends AppCompatActivity implements View.OnClickListener {

    private TextureView textureFPV;
    private Button btnBack, btnRecord, btnShoot, btnSwitchMode, btnStartStream, btnStopStream;
    private TextView textViewCameraMode, textViewIP, textViewTemp;

    private DJICodecManager mCodeManager;
    private SettingsDefinitions.CameraMode mCameraMode;
    private Boolean mIsCameraRecording;
    private VideoFeeder.VideoDataListener mVideoDataListener;

    private int mRecordingSec;
    private int tv_wid, tv_hei;
    private byte[] rgba;
    private List<String> missingPermission = new ArrayList<>();

    private static final int REQUEST_PERMISSION_CODE = 12345;

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.READ_PHONE_STATE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        initUI();
        initListener();
        initCamera();

        checkAndRequestPermissions();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        removeListener();
    }

    private void initUI(){

        textureFPV = findViewById(R.id.texture_fpv);

        btnBack = findViewById(R.id.btn_back);
        btnRecord = findViewById(R.id.btn_record);
        btnShoot = findViewById(R.id.btn_shoot);
        btnSwitchMode = findViewById(R.id.btn_swtichMode);
        btnStartStream = findViewById(R.id.btn_Stream);
        btnStopStream = findViewById(R.id.btn_stopStream);

        textViewIP = findViewById(R.id.tv_ip);
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
    private void initListener(){
        textureFPV.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                tv_hei=height;
                tv_wid=width;
                if(mCodeManager == null){
                    mCodeManager = new DJICodecManager(cameraActivity.this, surface, width, height);
                }
                else{
                    mCodeManager.getRgbaData(width, height);
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
        btnSwitchMode.setOnClickListener(this);
        btnStartStream.setOnClickListener(this);
        btnStopStream.setOnClickListener(this);

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
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void record(){
        final  Camera camera = getCamera();
        if(camera!=null){
            if(mCameraMode!= SettingsDefinitions.CameraMode.RECORD_VIDEO){
                setCameraMode(camera, SettingsDefinitions.CameraMode.RECORD_VIDEO);
                if(mCameraMode != SettingsDefinitions.CameraMode.RECORD_VIDEO)
                    showToast("Not Record Mode. ");
            }
            if(mIsCameraRecording){
                stopRecording(camera);
                showToast("Start Recording.");
            }
            else{
                startRecording(camera);
                showToast("Stop Recording.");
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
                    Log.i("Record","Successful");
                }
                else{
                    showToast("Error : "+ djiError.getDescription());
                    Log.e("Record","Error :"+ djiError.getDescription());
                }
            }
        });
    }

    private void takePicture(){
        final Camera camera = getCamera();
        if(camera != null){
//            Turns the mode into "SHOOT_PHOTO"
            if(mCameraMode!=SettingsDefinitions.CameraMode.SHOOT_PHOTO){
                setCameraMode(camera, SettingsDefinitions.CameraMode.SHOOT_PHOTO);

                if(mCameraMode != SettingsDefinitions.CameraMode.SHOOT_PHOTO){
                    showToast("Not SHOOT Mode. Please Try Again.");
                    Log.w("Shoot","Not SHOOT Mode.");
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
                                if(djiError == null){
                                    showToast("Successful !");
                                    Log.i("Shoot", "Successful");

                                    Context context = cameraActivity.this;
                                    downloadPicture(context);
                                }
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
    //Download picture to internal devices
    private void downloadPicture(Context context){
        new Thread(){
            @Override
            public void run() {
                try {
                    if(ContextCompat.checkSelfPermission(context , android.Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED){

                        //Todo : PhotoWidth & PhotoHeight must to be check
                        int photoWidth = 1920, photoHeight = 1080;

                        Bitmap bitmap = Bitmap.createBitmap(photoWidth, photoHeight, Bitmap.Config.ARGB_8888);
//                ByteBuffer buf = ByteBuffer.wrap(mCodeManager.getRgbaData(photoHeight, photoWidth));
                        ByteBuffer buf = ByteBuffer.wrap(mCodeManager.getRgbaData(photoWidth, photoHeight));
                        bitmap.copyPixelsFromBuffer(buf);


                        OutputStream fos;
                        Date c = Calendar.getInstance().getTime();
                        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
                        String fileName = df.format(c);
                        String mimeType = "image/jpg";

//                Over sdk version 29
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentResolver resolver = getContentResolver();
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName+".jpg");
                            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                        } else {
                            Log.i("sdkVersion", "else");
                            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                            File image = new File(imagesDir, fileName + ".jpg");
                            fos = new FileOutputStream(image);
                        }
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        Objects.requireNonNull(fos).close();
                    }
                    else{
                        showToast("Please checkout your permission.");
                        Log.e("Saving", "Please checkout your permission.");
                    }
                }catch (Error | FileNotFoundException e){
                    showToast(e.toString());
                    e.printStackTrace();
                    Log.e("Saving", "Error"+e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private void videoStream(){
        btnStartStream.setVisibility(View.GONE);
        btnStopStream.setVisibility(View.VISIBLE);
        new Thread(){
            @Override
            public void run() {
//                String ipAddress = "10.10.131.190";
                String ipAddress = "10.10.11.62";
                final int port = 8888;
                textViewIP.setText(ipAddress+":"+Integer.toString(port));

//120 - 130 ms（受拍攝環境干擾和移動設備性能影響）。
                while(true){
                    try{
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                        int photoWidth = 1920, photoHeight = 1080;

                        while (btnStopStream.getVisibility()==View.VISIBLE){
                            Log.i("send_image", "loop");
                            Socket socket = new Socket(ipAddress,port);

                            socket.setSoTimeout(30);

                            DataOutputStream streamDO = new DataOutputStream(socket.getOutputStream());
                            BufferedWriter streamBW = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                            Bitmap bitmap = Bitmap.createBitmap(photoWidth, photoHeight, Bitmap.Config.ARGB_8888);
                            ByteBuffer buf = ByteBuffer.wrap(mCodeManager.getRgbaData(photoWidth, photoHeight));
                            if(buf == null){
                                Log.e("Err", "buf == null");
                                continue;
                            }
                            bitmap.copyPixelsFromBuffer(buf);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteStream);

                            byte[] imageBytes = byteStream.toByteArray();

                            int imageSize = imageBytes .length;
                            streamBW.write(Integer.toString(imageBytes .length)+"\n");
                            streamBW.flush();

                            Log.i("send_image", Integer.toString(imageBytes .length)+"\n");
                            streamDO.write(imageBytes);
                            streamDO.flush();
                            byteStream.reset();
                            socket.close();
                            Log.i("send_image", "socket close");
                        }
                    } catch (UnsupportedEncodingException e) {
                        Log.i("send_image_Unsupported", e.toString());
                    } catch (UnknownHostException e) {
                        Log.i("send_image_Unknown", e.toString());
                    } catch (IOException e) {
                        Log.i("send_image_IOE", e.toString());
                    }
                }
            }
        }.start();
    }

    private void updateCameraMode(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String cameraModeString = cameraModeToString(mCameraMode);
                textViewCameraMode.setText("Mode : "+cameraModeString);
                if(mIsCameraRecording){
                    btnRecord.setText("Stop");
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
                return "UNKOWN";
            default:
                return "Nothing";
        }
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
                showToast("Taking picture.");
                takePicture();
                break;
            case R.id.btn_record:
                showToast("Recording");
                record();
                break;
            case R.id.btn_swtichMode:
                final Camera camera = getCamera();
                setCameraMode(camera, SettingsDefinitions.CameraMode.SHOOT_PHOTO);
            case R.id.btn_Stream:
                showToast("Images are being transmitted");
                videoStream();
                break;
            case R.id.btn_stopStream:
                stopStream();
            default:
                break;
        }
    }
    private void stopStream() {
            btnStopStream.setVisibility(View.GONE);
            btnStartStream.setVisibility(View.VISIBLE);
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