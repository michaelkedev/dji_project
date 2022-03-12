package com.dji.GSDemo.GoogleMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.net.sip.SipSession;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.GSDemo.GoogleMap.fragment.BoundingBox;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.logic.album.model.DJIAlbumPullErrorType;
import dji.midware.data.model.P3.C;
import dji.midware.data.model.P3.Ca;
import dji.midware.data.model.P3.Pa;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;


public class cameraActivity extends AppCompatActivity implements View.OnClickListener {

    private Socket socket;

    private TextureView textureFPV;
    private Button btnBack, btnRecord, btnShoot, btnSwitchMode, btnStartStream, btnStopStream;
    private TextView textViewCameraMode, textViewIP;
    private ImageView mImgView;

    private DJICodecManager mCodeManager;
    private SettingsDefinitions.CameraMode mCameraMode;
    private Boolean mIsCameraRecording;
    private VideoFeeder.VideoDataListener mVideoDataListener;

    private int mRecordingSec;
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
        mImgView = findViewById(R.id.imageView);

        btnBack = findViewById(R.id.btn_back);
        btnRecord = findViewById(R.id.btn_record);
        btnShoot = findViewById(R.id.btn_shoot);
        btnSwitchMode = findViewById(R.id.btn_switchMode);
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

//    Handler h = new Handler();
//
//    Runnable updateData = new Runnable() {
//        @Override
//        public void run() {
//            textureFPV.setVisibility(View.INVISIBLE);
//            mImgView.setVisibility(View.VISIBLE);
//
//            Bitmap bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888); //Get DJI Camera Data
//            ByteBuffer buf = ByteBuffer.wrap(mCodeManager.getRgbaData(1920, 1080));
//
////            String boundingBoxJson =
//            if (buf != null){
//                bitmap.copyPixelsFromBuffer(buf);
//                Canvas canvas = new Canvas(bitmap);
//                Paint paint = new Paint();
//                paint.setStyle(Paint.Style.STROKE);
//                canvas.drawRect(100, 100, 200, 200, paint);
//                mImgView.setImageBitmap(bitmap);
//            }
//
////            h.postDelayed(r,500);
//            h.post(updateData);
//        }
//    };
    private ArrayList<BoundingBox> getBoundingBoxes (JSONArray array){
        ArrayList<BoundingBox> boundingBoxes = new ArrayList<>();
        try{
            //        BoundingBox current = new Gson().fromJson(array.getJSONObject(i).toString(), BoundingBox.class);
            for(int i=0;i<array.length();i++){
                BoundingBox current = new Gson().fromJson(array.getJSONObject(i).toString(), BoundingBox.class);
                boundingBoxes.add(current);
            }
        }catch (Exception e){
            Log.d("cameraActivity_error", e.toString());
        }
        return boundingBoxes;
    }
//    private void updateImgView(Bitmap bitmap, ){
//        runOnUiThread(updateData);
//    }
    private void initListener(){
//        Set texture view listener
        textureFPV.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void
//Create a DJI Decoder when surfaceTexture is available
            onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

                if(mCodeManager == null){
                    mCodeManager = new DJICodecManager(cameraActivity.this, surface, width, height);
                    Log.i("initListener", "CodeManager is null");

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
//                Log.i("initListener", "update");
            }
        });

        btnBack.setOnClickListener(this);
        btnShoot.setOnClickListener(this);
        btnRecord.setOnClickListener(this);
        btnSwitchMode.setOnClickListener(this);
        btnStartStream.setOnClickListener(this);
        btnStopStream.setOnClickListener(this);

//        The callback for receiving the raw H264 video data for camera live view

        mVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int i) {
                if(mCodeManager != null){
                    mCodeManager.sendDataToDecoder(bytes, i);
                }
            }
        };

//        Once the camera is connected and receive video data, it will show on the textureFPV

        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mVideoDataListener);

//        Log.i("OnReceive", "rm Instance");
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
        btnStartStream.setVisibility(View.INVISIBLE);
        btnStopStream.setVisibility(View.VISIBLE);
        textureFPV.setVisibility(View.INVISIBLE);
        mImgView.setVisibility(View.VISIBLE);

        new Thread(){
            @Override
            public void run() {
                String ipAddress = "10.10.11.62";
                final int port = 8888;
//120 - 130 ms（受拍攝環境干擾和移動設備性能影響）。
                while(true){
                    try{
                        ByteArrayOutputStream image = new ByteArrayOutputStream();

                        int photoWidth = 1920, photoHeight = 1080;

                        while (btnStopStream.getVisibility()==View.VISIBLE){
                            Log.i("send_image", "loop");

                            socket = new Socket(ipAddress,port);

//                            socket.setSoTimeout(50);//set Time out

                            if(socket.isConnected()){
                                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                                DataInputStream dis = new DataInputStream(socket.getInputStream());

                                Bitmap bitmap = Bitmap.createBitmap(photoWidth, photoHeight, Bitmap.Config.ARGB_8888); //Create a bitmap
                                ByteBuffer buf = ByteBuffer.wrap(mCodeManager.getRgbaData(photoWidth, photoHeight)); // Put current image into bitmap

                                if(buf != null){
                                    bitmap.copyPixelsFromBuffer(buf);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, image);

                                    byte[] imageBytes = image.toByteArray();

                                    int imageSize = imageBytes.length;
                                    dos.writeBytes(Integer.toString(imageBytes .length)+"\n");
                                    dos.flush();

                                    dos.write(imageBytes);


                                    //Todo : Don't close socket

                                    final ExecutorService service = Executors.newSingleThreadExecutor();

                                    final String msg = dis.readLine();
                                    final JSONArray array = new JSONArray(msg);

                                    Log.d("draw", array.toString());

                                    dis.close();
                                    dos.flush();
                                    dos.close();
                                    buf.clear();
                                    image.reset();
                                    socket.close();
                                    Log.d("socket", "Socket Close");
                                    service.submit(new Runnable(){
                                        @Override
                                        public void run() {
                                            Log.d("drawImageView", "Draw");
                                            ArrayList<BoundingBox> boundingBoxes = getBoundingBoxes(array);

                                            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                                            String[] classes = {"Head", "Helmet", "Unknown", "Person"};
                                            final Canvas canvas = new Canvas(mutableBitmap);
                                            final Paint paint = new Paint();
//                                        Paint
                                            paint.setStyle(Paint.Style.STROKE);
                                            paint.setTextSize(40);
                                            paint.setStrokeWidth(3);
                                            for (BoundingBox b:boundingBoxes) {
                                                float floatX1 = (float)b.getX1()-10f;
                                                float floatY1 = (float)b.getY1();
                                                if(b.getId()==0){
                                                    paint.setColor(Color.RED);
                                                }else {
                                                    paint.setColor(Color.GREEN);
                                                }
                                                canvas.drawText(classes[b.getId()], floatX1, floatY1, paint);
                                                canvas.drawRect(b.getX1(), b.getY1(), b.getX2(), b.getY2(), paint);
                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mImgView.setImageBitmap(mutableBitmap);
                                                }
                                            });
                                        }
                                    });
                            }

                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        Log.e("socketGGUnsupport", e.toString());
                        break;
                    } catch (UnknownHostException e) {
                        Log.e("socketGGUnknown", e.toString());
                        break;
                    } catch (IOException e) {
                        Log.e("socketGGIOE", e.toString());
                        break;
                    }catch (JsonIOException e){
                        Log.e("socketGGJSON", e.toString());
                        break;
                    } catch (JSONException e) {
                        Log.e("socketGGJSON", e.toString());
                        break;
                    }catch (NullPointerException e){
                        Log.e("socketGGNULL", e.toString());
                        break;
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
            case R.id.btn_switchMode:
                final Camera camera = getCamera();
                setCameraMode(camera, SettingsDefinitions.CameraMode.SHOOT_PHOTO);
            case R.id.btn_Stream:
                showToast("Images are being transmitted");
                videoStream();
                break;
            case R.id.btn_stopStream:
                stopStream();
                break;
            default:
                break;
        }
    }
    private void streamError(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertMessage("INTERNET ERROR", "Please Check Your Internet");
                textureFPV.setVisibility(View.VISIBLE);
                mImgView.setVisibility(View.INVISIBLE);
                btnStopStream.setVisibility(View.GONE);
                btnStartStream.setVisibility(View.VISIBLE);
            }
        });
    }
    private void stopStream() {
        Log.d("socketClose", "Stop Stream");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textureFPV.setVisibility(View.VISIBLE);
                mImgView.setVisibility(View.INVISIBLE);
                btnStopStream.setVisibility(View.GONE);
                btnStartStream.setVisibility(View.VISIBLE);
            }
        });
            try{
                if(socket.isConnected())
                    socket.close();
                else
                    Log.d("socketClose", "Socket is not connected");
            } catch (IOException e) {
                Log.e("socketClose", e.toString());
            } catch (NullPointerException e){
                Log.e("socketClose", e.toString());
            }
    }
    private void alertMessage(String title, String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder dialog = new AlertDialog.Builder(cameraActivity.this);
                dialog.setCancelable(false);
                dialog.setTitle(title);
                dialog.setMessage(msg);
                dialog.setNegativeButton("GOT IT.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                final AlertDialog alert = dialog.create();
                alert.show();
            }
        });
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