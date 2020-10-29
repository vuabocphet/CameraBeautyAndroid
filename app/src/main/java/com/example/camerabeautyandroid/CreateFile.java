package com.example.camerabeautyandroid;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreateFile {
    public static String getFilename(Context context) {
        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(new Date());
        File storageDir = context.getExternalFilesDir("EcoAppLock");
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        try {
            File file = File.createTempFile(
                    "img" + format,
                    ".jpg",
                    storageDir
            );
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getFodel(Context context) {
        File ecoAppLock = context.getExternalFilesDir("EcoAppLock");
        if (ecoAppLock == null) {
            return "";
        }
        return ecoAppLock.getAbsolutePath();
    }
}
