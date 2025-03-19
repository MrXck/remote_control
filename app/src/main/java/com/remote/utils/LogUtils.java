package com.remote.utils;

import android.os.Handler;
import android.os.Looper;

public class LogUtils {

    public static void log(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
