package com.remote.utils;

import android.content.Context;
import android.view.Surface;
import android.view.WindowManager;

public class RotationUtils {
    public static int getDeviceRotationDegrees(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:   return 0;
            case Surface.ROTATION_90:  return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: return 0;
        }
    }
}