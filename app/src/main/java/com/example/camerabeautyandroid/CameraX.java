package com.example.camerabeautyandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraX {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview imagePreview;
    private ImageCapture imageCapture;
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private CameraXCallBack cameraXCallBack;
    private RelativeLayout cameraXPreviewView;
    private PreviewView previewView;

    private GPUImage gpuImage = null;
    private GPUImageFilterGroup magicFilterGroup = null;
    private GPUImageFilterGroup noMagicFilterGroup = null;
    private boolean isInMagic = false;

    private boolean isPreviewing = false;

    private Bitmap saveBitmap = null;

    public static CameraX create() {
        return new CameraX();
    }

    public CameraX setCameraXCallBack(CameraXCallBack cameraXCallBack) {
        this.cameraXCallBack = cameraXCallBack;
        return this;
    }

    public CameraX setCameraXPreviewView(RelativeLayout cameraXPreviewView) {
        if (cameraXPreviewView == null) {
            if (this.cameraXCallBack == null) {
                return this;
            }
            this.cameraXCallBack.onErrorCameraX();
            return this;
        }
        this.cameraXPreviewView = cameraXPreviewView;
        this.previewView = new PreviewView(this.cameraXPreviewView.getContext());
        this.previewView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View view = new View(this.context);
        view.setBackgroundColor(Color.BLACK);
        view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.cameraXPreviewView.addView(previewView);
        this.cameraXPreviewView.addView(view);
        return this;
    }

    public CameraX setContext(Context context) {
        this.context = context;
        return this;
    }

    public CameraX setLifecycleOwner(LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner == null) {
            if (this.cameraXCallBack == null) {
                return this;
            }
            this.cameraXCallBack.onErrorCameraX();
            return this;
        }
        this.lifecycleOwner = lifecycleOwner;
        return this;
    }

    public CameraX startCameraX() {
        if (this.context == null) {
            if (this.cameraXCallBack == null) {
                return this;
            }
            this.cameraXCallBack.onErrorCameraX();
            return this;
        }
        if (!Per.checkPermission(this.context, Per.READ_WRITE_CAMERA)) {
            if (this.cameraXCallBack == null) {
                return this;
            }
            this.cameraXCallBack.onErrorCameraX();
            return this;
        }
        try {
            this.cameraXPreviewView.post(this::createAll);
        } catch (Exception e) {
            if (this.cameraXCallBack == null) {
                return this;
            }
            this.cameraXCallBack.onErrorCameraX();
        }
        return this;
    }

    private void createAll() {
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this.context);
        this.imagePreview = this.createPreview();
        this.imageCapture = this.createImageCapture();
        final CameraSelector build = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        this.cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                processCameraProvider.unbindAll();
                processCameraProvider.bindToLifecycle(
                        this.lifecycleOwner,
                        build,
                        this.imagePreview,
                        this.imageCapture);
                this.previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
                this.imagePreview.setSurfaceProvider(this.previewView.getSurfaceProvider());
                this.imagePreview.
                gpuImage = new GPUImage(this.context);
                gpuImage.setGLSurfaceView();

            } catch (Exception e) {
                e.printStackTrace();
                if (this.cameraXCallBack == null) {
                    return;
                }
                this.cameraXCallBack.onErrorCameraX();
            }
        }, ContextCompat.getMainExecutor(this.context));
    }


    private Preview createPreview() {
        return new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
    }

    private ImageCapture createImageCapture() {
        return new ImageCapture.Builder()
                .build();
    }

    @SuppressLint("CheckResult")
    public void takePictureRX() {
        if (!Per.checkPermission(this.context, Per.READ_WRITE_CAMERA)) {
            if (cameraXCallBack == null) {
                return;
            }
            cameraXCallBack.onErrorCameraX();
            return;
        }
        this.previewView.post(() -> Single.create((SingleOnSubscribe<Void>) emitter -> {
            try {
                this.takePicture();
            } catch (Exception e) {
                if (cameraXCallBack == null) {
                    return;
                }
                cameraXCallBack.onErrorCameraX();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    public void takePicture() {
        if (!Per.checkPermission(this.context, Per.READ_WRITE_CAMERA)) {
            if (cameraXCallBack == null) {
                return;
            }
            cameraXCallBack.onErrorCameraX();
            return;
        }
        File file = new File(CreateFile.getFilename(this.context));
        ImageCapture.OutputFileOptions.Builder outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file);
        this.imageCapture.takePicture(outputFileOptions.build(), ContextCompat.getMainExecutor(this.context), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                if (cameraXCallBack == null) {
                    return;
                }
                cameraXCallBack.onSuccessCaptureCameraX();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (cameraXCallBack == null) {
                    return;
                }
                cameraXCallBack.onErrorCameraX();
            }
        });
    }
}
