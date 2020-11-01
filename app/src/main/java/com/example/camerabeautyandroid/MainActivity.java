package com.example.camerabeautyandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.ChangeScroll;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.RelativeLayout;

import static android.util.Log.e;

public class MainActivity extends AppCompatActivity implements CameraBeauty.ObserveCamera {

    private RelativeLayout relativeLayout;
    private GLSurfaceView glSurfaceView;
    private CameraBeauty cameraBeauty;
    private ConstraintLayout csLayout;
    private Transition transition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.relativeLayout = findViewById(R.id.custom_cover_top);
        this.glSurfaceView = findViewById(R.id.surfaceView);
        this.csLayout = findViewById(R.id.csLayout);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        boolean b = Per.checkPermission(this, Per.READ_WRITE_CAMERA);
        this.cameraBeauty = CameraBeauty.create(this)
                .setObserveCamera(this)
                .setfullScreenCamera(false)
                .onCreateCameraBeauty(this.glSurfaceView);

        if (!b) {
            Per.requestPermissions(this, 123, Per.READ_WRITE_CAMERA);
        }
        this.transition =new ChangeBounds();
        transition.setDuration(250);
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                e("TAG", "onTransitionStart: " );
                glSurfaceView.setVisibility(View.INVISIBLE);
                cameraBeauty.onPauseCameraBeauty();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                e("TAG", "onTransitionEnd: " );
                glSurfaceView.setVisibility(View.VISIBLE);
                glSurfaceView.setBackground(new ColorDrawable(Color.TRANSPARENT));
                e("TAG", "onTransitionEnd:W "+glSurfaceView.getWidth());
                e("TAG", "onTransitionEnd:H "+glSurfaceView.getHeight());
                cameraBeauty.updateSizeCamera();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                e("TAG", "onTransitionCancel: " );
            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });
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

    public void on1(View view) {
        glSurfaceView.setVisibility(View.INVISIBLE);
        ConstraintSet set = new ConstraintSet();
        TransitionManager.beginDelayedTransition(this.csLayout,transition);
        set.clone(csLayout);
        cameraBeauty.setRatio(CameraBeauty.Ratio.RATIO_16_9);
        set.setDimensionRatio(glSurfaceView.getId(), "9:16");
        set.applyTo(csLayout);

    }

    public void on2(View view) {
        cameraBeauty.setRatio(CameraBeauty.Ratio.RATIO_1_1);
        glSurfaceView.setVisibility(View.INVISIBLE);
        ConstraintSet set = new ConstraintSet();
        TransitionManager.beginDelayedTransition(this.csLayout,transition);
        set.clone(csLayout);
        set.setDimensionRatio(glSurfaceView.getId(), "1:1");
        set.applyTo(csLayout);
    }

    public void on3(View view) {
        glSurfaceView.setVisibility(View.INVISIBLE);
        TransitionManager.beginDelayedTransition(this.csLayout,transition);
        ConstraintSet set = new ConstraintSet();
        set.clone(csLayout);
        cameraBeauty.setRatio(CameraBeauty.Ratio.RATIO_4_3);
        set.setDimensionRatio(glSurfaceView.getId(), "3:4");
        set.applyTo(csLayout);
    }

    public void on4(View view) {
        glSurfaceView.setVisibility(View.INVISIBLE);
        TransitionManager.beginDelayedTransition(this.csLayout,transition);
        ConstraintSet set = new ConstraintSet();
        set.clone(csLayout);
        cameraBeauty.setRatio(CameraBeauty.Ratio.RATIO_FULL);
        set.setDimensionRatio(glSurfaceView.getId(), "0:0");
        set.applyTo(csLayout);
    }

    public void doi(View view) {
        cameraBeauty.switchCamera();
    }
}