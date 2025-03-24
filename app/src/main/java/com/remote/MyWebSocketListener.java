package com.remote;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.remote.pojo.Message;
import com.remote.rtc.WebRTCManager;
import com.remote.utils.JsonUtils;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MyWebSocketListener extends WebSocketListener {

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "join");
        map.put("roomId", 111);
        webSocket.send(JsonUtils.toJson(map));
        // 连接成功
        log("连接已打开");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        // 收到文本消息
        log("收到消息: " + text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        WebRTCManager webRTCManager = MainActivity.webRTCManager;

        byte[] byteArray = bytes.toByteArray();

        String string = new String(byteArray, StandardCharsets.UTF_8);
        Message message = JsonUtils.fromJson(string);

        String type = message.getType();

        switch (type) {
            case "offer":
                webRTCManager.handlerOffer(message.getSdp());
                break;
            case "answer":
                webRTCManager.handlerAnswer(message.getSdp(), webRTCManager.localPeerConnection);
                break;
            case "candidate":
                webRTCManager.handlerCandidate(message.getCandidate(), webRTCManager.localPeerConnection);
                break;
            case "action":
                Map data = message.getData();
                log("收到操作: " + data);

                if (data.get("type").equals("click")) {
                    new Thread(() -> {
                        try (LocalSocket socket = new LocalSocket();
                             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                            socket.connect(new LocalSocketAddress("my_click_socket"));
                            output.writeInt(1);
                            output.writeDouble((Double) data.get("x"));
                            output.writeDouble((Double) data.get("y"));
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else if (data.get("type").equals("drag")) {
                    new Thread(() -> {
                        try (LocalSocket socket = new LocalSocket();
                             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                            socket.connect(new LocalSocketAddress("my_click_socket"));
                            output.writeInt(2);
                            output.writeDouble((Double) data.get("x"));
                            output.writeDouble((Double) data.get("y"));
                            output.writeDouble((Double) data.get("x1"));
                            output.writeDouble((Double) data.get("y1"));
                            output.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }




        }

        log("收到消息: " + string);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        // 连接关闭
        log("连接关闭: " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        // 连接失败
        log("连接失败: " + t.getMessage());
    }

    public void log(String message) {
        // 使用 Log 或更新 UI（注意线程切换）
        runOnUiThread(() -> {
            Log.e("ZN", message);
        });
    }

    private void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}