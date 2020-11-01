package com.example.camerabeautyandroid;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class SaveFile {

    public static String saveImage(Bitmap bitmap, String path, CameraBeauty.Ratio ratio, CameraBeauty.Preview preview) {
        Log.i("TinhNv", "saveImageBitmap: " + bitmap.getWidth() + "*" + bitmap.getHeight());
        Bitmap scaledBitmap = null;
        try {

            if (preview == CameraBeauty.Preview.PREVIEW_1) {

                if (ratio == CameraBeauty.Ratio.RATIO_1_1) {
                    scaledBitmap = getResizedCropBitmap(bitmap);
                }
                if (ratio == CameraBeauty.Ratio.RATIO_FULL) {
                    scaledBitmap = getResizedFullBitmap(bitmap);
                }
                if (ratio == CameraBeauty.Ratio.RATIO_4_3) {
                    scaledBitmap = bitmap;
                }

            } else {
                if (ratio == CameraBeauty.Ratio.RATIO_1_1) {
                    scaledBitmap = getResizedCropBitmap(bitmap);
                }
                if (ratio == CameraBeauty.Ratio.RATIO_FULL) {
                    scaledBitmap = getResizedFullBitmap(bitmap);
                }
                if (ratio == CameraBeauty.Ratio.RATIO_16_9) {
                    //scaledBitmap = getResizedFullBitmap(bitmap, sizeHScreen);
                }
                if (ratio == CameraBeauty.Ratio.RATIO_4_3) {
                    scaledBitmap = bitmap;
                }
                if (scaledBitmap == null) {
                    scaledBitmap = bitmap;
                }
            }

            Log.i("TinhNv", "saveImage: " + "w=" + scaledBitmap.getWidth() + "         h=" + scaledBitmap.getHeight());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            OutputStream fos;
            fos = new FileOutputStream(path);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (Exception e) {
            Log.i("TinhNv", "saveImage: " + e.toString());
            e.printStackTrace();
            path = "";
        }
        return path;
    }

    private static Bitmap getResizedFullBitmap(Bitmap srcBmp) {
        Bitmap dstBmp;
        dstBmp = Bitmap.createBitmap(
                srcBmp,
                srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
                0,
                srcBmp.getHeight() / 2,
                srcBmp.getHeight()
        );
        srcBmp.recycle();
        return dstBmp;
    }

    private static Bitmap getResizedCropBitmap(Bitmap srcBmp) {
        Log.i("TinhNv", "getResizedCropBitmap: " + "w=" + srcBmp.getWidth() + "*" + "h=" + srcBmp.getHeight());
        Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()) {

            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    srcBmp.getWidth() / 2 - srcBmp.getHeight() / 2,
                    0,
                    srcBmp.getHeight(),
                    srcBmp.getHeight()
            );

        } else {
            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    srcBmp.getHeight() / 2 - srcBmp.getWidth() / 2,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );
        }
        srcBmp.recycle();
        return dstBmp;
    }

}
