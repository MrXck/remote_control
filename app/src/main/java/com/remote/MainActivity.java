package com.remote;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.remote.rtc.WebRTCManager;
import com.remote.utils.DeviceIdUtil;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    public static WebRTCManager webRTCManager = new WebRTCManager();
    public static WebSocketManager webSocketManager;
    private String id;
    public static TextView code;
    public static TextView password;
    private PowerManager.WakeLock wakeLock;

    public static String generateValidateCode4String(int length) {
        Random rdm = new Random();
        String hash1 = Integer.toHexString(rdm.nextInt());
        return hash1.substring(0, length);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        webRTCManager.localView = findViewById(R.id.local_view);
//        webRTCManager.remoteView = findViewById(R.id.remote_view);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::WakeLockTag"
        );
        wakeLock.acquire(10 * 60 * 1000L);

        code = findViewById(R.id.code);
        password = findViewById(R.id.password);

        View button = findViewById(R.id.generate_password);

        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        String string = sharedPref.getString("password", generateValidateCode4String(8));
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("password", string);
        editor.apply();

        password.setText(string);

        button.setOnClickListener(view -> {
            String passwordCode = generateValidateCode4String(8);
            password.setText(passwordCode);
            editor.putString("password", passwordCode);
            editor.apply();
        });


        id = DeviceIdUtil.getDeviceId(context);
        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    1);
        }
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

//        Button button = findViewById(R.id.button);
//
//        button.setOnClickListener((View v) -> webRTCManager.createOffer(webRTCManager.localPeerConnection));
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
        webSocketManager = new WebSocketManager("wss://www.yuumi.cc:8080", id);
//        webSocketManager = new WebSocketManager("ws://192.168.182.161:8080", id);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }
}