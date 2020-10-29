package com.example.camerabeautyandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

public class Per {

    public static final String[] READ_WRITE_CAMERA = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    public static boolean checkPermission(Context context, String... permission) {
        if (context == null || permission == null || permission.length == 0) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String s : permission) {
                if (context.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity, int code, String... permission) {
        if (activity == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(permission, code);
        }
    }

    public static void requestSetting(Activity context, int rqCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openPermissionsSetting(context, rqCode);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void openPermissionsSetting(Activity context, int rqCode) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityForResult(intent, rqCode);
    }

    public static boolean checkPerSystemSetting(Context context) {
        boolean retVal = true;
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context);
        }
        return retVal;
    }

}
