package com.remote;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundServiceWithNotification();

        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data = intent.getParcelableExtra("data", Intent.class);
            } else {
                // 添加弃用警告抑制
                //noinspection deprecation
                data = intent.getParcelableExtra("data");
            }
            if (data != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MainActivity.webRTCManager.startScreenCapture(data);
                } else {
                    mediaProjection = MainActivity.webRTCManager.mediaProjection;
                    MainActivity.webRTCManager.startScreenCapture(data);
                }
            }
        }
        return START_STICKY;
    }

    private void startForegroundServiceWithNotification() {
        String channelId = "screen_capture_channel";
        Notification notification = buildNotification(channelId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }
    }

    private Notification buildNotification(String channelId) {
        NotificationChannel channel = new NotificationChannel(channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Screen capture in progress");
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        return new Notification.Builder(this, channelId).setContentTitle("屏幕捕获中").setContentText("正在录制屏幕内容").setSmallIcon(android.R.drawable.ic_menu_camera) // 必须设置有效图标
                .setOngoing(true).build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}