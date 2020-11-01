package com.example.camerabeautyandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import static android.util.Log.e;

public class MainActivity extends AppCompatActivity implements CameraBeauty.ObserveCamera {

    private RelativeLayout relativeLayout;
    private GLSurfaceView glSurfaceView;
    private CameraBeauty cameraBeauty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.relativeLayout = findViewById(R.id.custom_cover_top);
        this.glSurfaceView = findViewById(R.id.surfaceView);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        boolean b = Per.checkPermission(this, Per.READ_WRITE_CAMERA);
        this.cameraBeauty = CameraBeauty.create(this)
                .setObserveCamera(this)
                .setfullScreenCamera(false)
                .onCreateCameraBeauty(this.glSurfaceView)
                .setCustomViewTop(this.relativeLayout);

        if (!b) {
            Per.requestPermissions(this, 123, Per.READ_WRITE_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123 && Per.checkPermission(this, Per.READ_WRITE_CAMERA)) {
            this.cameraBeauty.onResumeCameraBeauty();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.cameraBeauty == null || !Per.checkPermission(this, Per.READ_WRITE_CAMERA)) {
            return;
        }
        this.cameraBeauty.onResumeCameraBeauty();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.cameraBeauty == null || !Per.checkPermission(this, Per.READ_WRITE_CAMERA)) {
            return;
        }
        this.cameraBeauty.onPauseCameraBeauty();
    }

    @Override
    public void onError(String message) {
        e("TinhNv", ": " + message);
    }

    @Override
    public void onProcessImage(String message) {
        Log.i("TinhNv", "onProcessImage: " + message);
    }

    @Override
    public void onComplete(String path) {
        Log.i("TinhNv", "onComplete: " + path);
    }

    public void capture(View view) {
        if (this.cameraBeauty == null || !Per.checkPermission(this, Per.READ_WRITE_CAMERA)) {
            return;
        }
        this.cameraBeauty.cature();
    }
}