package com.example.camerabeautyandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;

public class CameraBeauty {
    private static final String TAG = CameraBeauty.class.getSimpleName();

    public interface ObserveCamera {
        void onError(String message);

        void onProcessImage(String message);

        void onComplete(String path);
    }

    private void observeError(String method, String message) {
        if (this.observeCamera == null) {
            return;
        }
        this.observeCamera.onError(method + "->" + message);
    }

    private void observeProcessImage(String message) {
        if (this.observeCamera == null) {
            return;
        }
        this.observeCamera.onProcessImage(message);
    }

    private void observeComplete(String path) {
        if (this.observeCamera == null) {
            return;
        }
        this.observeCamera.onComplete(path);
    }

    private ObserveCamera observeCamera;
    private Camera camera;
    private int cameraId = 1;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private int picHeight = 0;
    private Bitmap saveBitmap = null;
    private String imgPathMagic = null;

    private GPUImage gpuImage = null;
    private GPUImageFilterGroup magicFilterGroup = null;
    private GPUImageFilterGroup noMagicFilterGroup = null;
    private boolean isInMagic = false;
    private boolean isPreviewing = false;
    private View customCoverTopView;
    private GLSurfaceView glSurfaceView = null;
    private boolean fullScreenCamera = true;

    private Context context;

    public static CameraBeauty create(Context context) {
        return new CameraBeauty(context);
    }

    public CameraBeauty(Context context) {
        this.context = context;
    }

    private void init(@Nullable GLSurfaceView glSurfaceView) {
        this.gpuImage = new GPUImage(this.context);
        this.glSurfaceView = glSurfaceView;
        this.gpuImage.setGLSurfaceView(this.glSurfaceView);
        this.magicFilterGroup = new GPUImageFilterGroup();
        this.magicFilterGroup.addFilter(new GPUImageBeautyFilter());
        this.noMagicFilterGroup = new GPUImageFilterGroup();
        this.noMagicFilterGroup.addFilter(new GPUImageFilter());
        this.gpuImage.setFilter(this.isInMagic ? this.magicFilterGroup : this.noMagicFilterGroup);
        DisplayMetrics displayMetrics = this.context.getResources().getDisplayMetrics();
        this.screenWidth = displayMetrics.widthPixels;
        this.screenHeight = displayMetrics.heightPixels;
        this.customCoverTopView = new View(this.context);
        this.customCoverTopView.setBackgroundColor(Color.parseColor("#50000000"));
        Log.i(TAG, "screenWidth =" + this.screenWidth + "|" + "screenHeight =" + screenHeight);
    }

    private void initCamera(int cameraId) {
        try {
            if (this.gpuImage == null) {
                this.init(this.glSurfaceView);
            }
            if (this.camera == null) {
                this.cameraId = cameraId;
                this.camera = Camera.open(this.cameraId);
                this.setupCamera(this.camera);
                if (!this.isPreviewing) {
                    this.isPreviewing = true;
                    this.gpuImage.setUpCamera(this.camera, cameraId == 0 ? 90 : 270, cameraId > 0, false);
                }
            }
        } catch (Exception e) {
            this.observeError("initCamera", e.toString());
        }
    }

    private void setupCamera(Camera camera) {
        if (camera == null) {
            this.observeError("setupCamera", "Camera Null");
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null || this.glSurfaceView == null) {
            this.observeError("setupCamera", "Parameters Null");
            return;
        }
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        Camera.Size propSizeForHeight = CameraUtil.getInstance().getPropSizeForHeight(parameters.getSupportedPictureSizes(), 800);
        if (propSizeForHeight == null) {
            this.observeError("setupCamera", " Camera.Size Null");
            return;
        }

        Log.i(TAG, "pictureSize = " + propSizeForHeight.width + " | " + propSizeForHeight.height);
        parameters.setPictureSize(propSizeForHeight.width, propSizeForHeight.height);
        camera.setParameters(parameters);
        this.picHeight = this.screenWidth * propSizeForHeight.width / propSizeForHeight.height;
        if (!this.fullScreenCamera) {
            FrameLayout.LayoutParams layoutParams = this.changePreviewSize(camera, this.picHeight, this.screenWidth);
            this.glSurfaceView.setLayoutParams(layoutParams);
        }
    }

    private FrameLayout.LayoutParams changePreviewSize(Camera camera, int viewWidth, int viewHeight) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size closelySize = null;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            Log.e(TAG, "CameraPreview :" + supportedPreviewSize.width + " * " + supportedPreviewSize.height);
            if ((supportedPreviewSize.width == viewWidth) && (supportedPreviewSize.height == viewHeight)) {
                closelySize = supportedPreviewSize;
            }
        }
        if (closelySize == null) {
            float reqRatio = (float) viewWidth / viewHeight;
            float curRatio;
            float deltaRatio;
            float deltaRatioMin = Float.MAX_VALUE;
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                if (supportedPreviewSize.width < 1024) continue;
                curRatio = (float) (supportedPreviewSize.width) / supportedPreviewSize.height;
                deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    closelySize = supportedPreviewSize;
                }
            }
        }
        if (closelySize == null) {
            observeError("changePreviewSize", "closelySize null");
            return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        if (closelySize.height < screenWidth) {
            int shouldPreviewHeight = (screenWidth * closelySize.width) / closelySize.height;
            if (screenHeight < shouldPreviewHeight) {
                closelySize.width = screenHeight;
                closelySize.height = (screenHeight * closelySize.height) / closelySize.width;
            } else {
                closelySize.width = screenWidth;
                closelySize.height = shouldPreviewHeight;
            }
        }
        boolean hasBlankHeight = SizeUtilJava.px2dp(this.context, (float) (screenHeight - closelySize.width) / 2) > 44;
        if (hasBlankHeight) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.screenWidth, SizeUtilJava.dp2px(this.context, 44f));
            this.customCoverTopView.setLayoutParams(layoutParams);
        } else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(this.screenWidth, 0);
            this.customCoverTopView.setLayoutParams(params);
        }
        parameters.setPreviewSize(closelySize.width, closelySize.height);
        camera.setParameters(parameters);
        Log.i(TAG, "changePreviewSize: " + closelySize.height + "-" + closelySize.width);
        return new FrameLayout.LayoutParams(1280, 720);
    }


    public void cature() {
        this.observeProcessImage("The image is being processed");
        if (this.camera == null) {
            this.observeError("cature", "Camera Null");
            return;
        }
        try {
            this.camera.takePicture(null, null, (bytes, camera) -> {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                this.saveBitmap = CameraUtil.getInstance().setTakePicktrueOrientation(this.cameraId, bitmap);
                this.saveFile();
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            });
        } catch (Exception e) {
            this.observeError("cature", e.toString());
        }
    }

    public void releaseCamera() {
        if (this.gpuImage != null) {
            this.gpuImage.deleteImage();
        }
        if (this.camera != null) {
            this.isPreviewing = false;
            this.camera.setPreviewCallback(null);
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }

    private void saveFile() {
        bitmapSingle().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private Single<Bitmap> bitmapSingle() {
        return Single.create((SingleOnSubscribe<Bitmap>) emitter -> {
            try {
                GPUImage gpuImage = new GPUImage(this.context);
                gpuImage.setImage(this.saveBitmap);
                gpuImage.setFilter(this.isInMagic ? new GPUImageBeautyFilter() : new GPUImageFilter());
                Bitmap bitmapWithFilterApplied = gpuImage.getBitmapWithFilterApplied(this.saveBitmap);
                this.imgPathMagic = CreateFile.getFilename(this.context);
                String path = saveImage(this.saveBitmap, this.imgPathMagic);
                this.observeComplete(path);
                gpuImage.deleteImage();
                emitter.onSuccess(bitmapWithFilterApplied);
            } catch (Exception e) {
                e.printStackTrace();
                emitter.onError(e);
                this.observeError("bitmapSingle", e.toString());
            }
        });
    }

    public static String saveImage(Bitmap bitmap, String path) throws IOException {

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            OutputStream fos;
            fos = new FileOutputStream(path);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            path = "";
        }
        return path;
    }

    public void onResumeCameraBeauty() {
        this.initCamera(this.cameraId);
    }

    public void onPauseCameraBeauty() {
        this.releaseCamera();
    }

    private void switchCamera() {
        this.releaseCamera();
        this.cameraId = (this.cameraId + 1) % Camera.getNumberOfCameras();
        this.initCamera(this.cameraId);
    }

    public int getCameraId() {
        return this.cameraId;
    }

    public CameraBeauty setObserveCamera(ObserveCamera observeCamera) {
        this.observeCamera = observeCamera;
        return this;
    }

    public CameraBeauty setCustomViewTop(@Nullable RelativeLayout relativeLayout) {
        if (relativeLayout != null) {
            relativeLayout.removeAllViews();
            relativeLayout.addView(this.customCoverTopView);
        }
        return this;
    }

    private CameraBeauty switchMagic(boolean isInMagic) {
        this.isInMagic = isInMagic;
        this.gpuImage.setFilter(this.isInMagic ? magicFilterGroup : noMagicFilterGroup);
        return this;
    }

    public CameraBeauty setfullScreenCamera(boolean fullScreenCamera) {
        this.fullScreenCamera = fullScreenCamera;
        return this;
    }


    public CameraBeauty onCreateCameraBeauty(@Nullable GLSurfaceView glSurfaceView) {
        if (glSurfaceView == null) {
            this.observeError("onCreateCameraBeauty", "GLSurfaceView Null");
            return this;
        }
        this.init(glSurfaceView);
        return this;
    }
}
