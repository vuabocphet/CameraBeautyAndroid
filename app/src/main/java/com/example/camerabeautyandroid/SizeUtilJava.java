package com.example.camerabeautyandroid;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

public class SizeUtilJava {

    public static int px2dp(@Nullable Context context, float pxValue) {
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        return (int) (pxValue / density + 0.5f);
    }


    public static int dp2px(@Nullable Context context, float dipValue) {
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        return (int) (dipValue * density + 0.5f);
    }

}
