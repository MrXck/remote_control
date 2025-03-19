package com.remote;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.remote.rtc.WebRTCManager;

public class MainActivity extends AppCompatActivity {

    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    public static WebRTCManager webRTCManager = new WebRTCManager();
    public static WebSocketClient webSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webRTCManager.localView = findViewById(R.id.local_view);
        webRTCManager.remoteView = findViewById(R.id.remote_view);
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

    private void startWebsocket() {
        webSocketClient = new WebSocketClient("wss://www.yuumi.cc:8080");
        webSocketClient.connect(new MyWebSocketListener());
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
        } else {
            Log.e("ScreenCapture", "权限被拒绝");
        }
    }
}