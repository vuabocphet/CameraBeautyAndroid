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
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;

import static com.example.camerabeautyandroid.SaveFile.saveImage;

public class CameraBeauty {

    private static final String TAG = "TinhNv";

    public enum Preview {
        PREVIEW_1,
        PREVIEW_2
    }

    public enum Ratio {
        RATIO_16_9,
        RATIO_4_3,
        RATIO_1_1,
        RATIO_FULL
    }

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
    private int cameraId = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private Bitmap saveBitmap = null;
    private String imgPathMagic = null;
    private Ratio ratio = Ratio.RATIO_FULL;
    private Preview preview;
    private GPUImage gpuImage = null;
    private GPUImageFilterGroup magicFilterGroup = null;
    private GPUImageFilterGroup noMagicFilterGroup = null;
    private boolean isInMagic = false;
    private boolean isPreviewing = false;
    private View customCoverTopView;
    private GLSurfaceView glSurfaceView = null;
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
        Log.i(TAG, "screenWidth =" + this.screenWidth + "|" + "screenHeight =" + screenHeight);
        this.customCoverTopView.setBackgroundColor(Color.parseColor("#50000000"));
        if (this.screenWidth <= 1080 && this.screenHeight <= 1920) {
            this.preview = Preview.PREVIEW_1;
            return;
        }
        this.preview = Preview.PREVIEW_2;
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
        Camera.Size sizePicture = bestSizePicture(parameters);
        Camera.Size sizePreview = bestSizePreview(parameters);
        parameters.setPictureSize(sizePicture.width, sizePicture.height);
        parameters.setPreviewSize(sizePreview.width, sizePreview.height);
        camera.setParameters(parameters);
    }

    private Camera.Size bestSizePreview(Camera.Parameters parameters) {
        Camera.Size bestSize;
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        bestSize = sizeList.get(0);
        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).width * sizeList.get(i).height) >
                    (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        return bestSize;
    }

    private Camera.Size bestSizePicture(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = parameters.getSupportedPictureSizes();
        bestSize = sizeList.get(0);
        for (int i = 0; i < sizeList.size(); i++) {
            if (sizeList.get(i).width > bestSize.width)
                bestSize = sizeList.get(i);
        }
        return bestSize;
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
        this.bitmapSingle().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private Single<Bitmap> bitmapSingle() {
        return Single.create(emitter -> {
            try {
                GPUImage gpuImage = new GPUImage(this.context);
                gpuImage.setImage(this.saveBitmap);
                gpuImage.setFilter(this.isInMagic ? new GPUImageBeautyFilter() : new GPUImageFilter());
                Bitmap bitmapWithFilterApplied = gpuImage.getBitmapWithFilterApplied(this.saveBitmap);
                this.imgPathMagic = CreateFile.getFilename(this.context);
                String path = saveImage(saveBitmap, this.imgPathMagic, this.getRatio(), this.getPreview());
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

    public void onResumeCameraBeauty() {
        this.initCamera(this.cameraId);
    }

    public void onPauseCameraBeauty() {
        this.releaseCamera();
    }

    public void switchCamera() {
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

    public void updateSizeCamera() {
        this.initCamera(this.cameraId);
    }

    public Camera getCamera() {
        return camera;
    }

    public Ratio getRatio() {
        return ratio;
    }

    public void setRatio(Ratio ratio) {
        this.ratio = ratio;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public Preview getPreview() {
        return preview;
    }
}
