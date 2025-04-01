package com.remote;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.remote.rtc.WebRTCManager;
import com.remote.utils.DeviceIdUtil;

public class MainActivity extends AppCompatActivity {

    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    public static WebRTCManager webRTCManager = new WebRTCManager();
    public static WebSocketManager webSocketManager;
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webRTCManager.localView = findViewById(R.id.local_view);
        webRTCManager.remoteView = findViewById(R.id.remote_view);
        id = DeviceIdUtil.getDeviceId(getApplicationContext());
        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    1);
        } else {

            try {
                webRTCManager.initialize(getApplicationContext());
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage());
            }

            mediaProjectionManager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            screenCaptureLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleScreenCaptureResult
            );
            startScreenCapture();

            startWebsocket();

            Button button = findViewById(R.id.button);

            button.setOnClickListener((View v) -> webRTCManager.createOffer(webRTCManager.localPeerConnection));
        }
    }

    private boolean isAccessibilityEnabled() {
        ComponentName expectedComponentName = new ComponentName(this, MyAccessibilityService.class);
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(expectedComponentName.flattenToString());
    }

    private void startWebsocket() {
//        webSocketManager = new WebSocketManager("wss://www.yuumi.cc:8080", id);
        webSocketManager = new WebSocketManager("ws://192.168.182.161:8080", id);
        webSocketManager.connect();
    }

    private void startScreenCapture() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        screenCaptureLauncher.launch(captureIntent);
    }

    private void handleScreenCaptureResult(androidx.activity.result.ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            } else {
                webRTCManager.mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
            }

            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            serviceIntent.putExtra("resultCode", result.getResultCode());
            serviceIntent.putExtra("data", data);

            // 启动屏幕捕获服务或处理数据
            // 启动前台服务
            startForegroundService(serviceIntent);
            if (!isAccessibilityEnabled()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        } else {
            Log.e("ScreenCapture", "权限被拒绝");
        }
    }
}