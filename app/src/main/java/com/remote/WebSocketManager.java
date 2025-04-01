package com.remote;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.remote.pojo.Message;
import com.remote.rtc.WebRTCManager;
import com.remote.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class WebSocketManager {
    private final OkHttpClient okHttpClient = new OkHttpClient();;
    private final Request request;
    private WebSocket webSocket;
    private final MyWebSocketListener listener = new MyWebSocketListener();
    private int reconnectAttempts = 0;
    private final int maxReconnectAttempts = 5;
    private final long initialReconnectDelay = 1000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable reconnectRunnable;
    private final String id;

    public WebSocketManager(String url, String id) {
        this.request = new Request.Builder()
                .url(url)
                .build();
        this.id = id;
    }

    public void connect() {
        okHttpClient.newWebSocket(request, listener);
        reconnectAttempts = 0;
    }

    // 发送消息
    public void sendMessage(Message message) {
        webSocket.send(JsonUtils.toJson(message));
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) return;

        long delay = (long) (initialReconnectDelay * Math.pow(2, reconnectAttempts));
        reconnectAttempts++;

        handler.postDelayed(reconnectRunnable = () -> {
            Log.d("WebSocket", "尝试第 " + reconnectAttempts + " 次重连");
            connect();
        }, delay);
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "正常关闭");
        }
        handler.removeCallbacks(reconnectRunnable);
        reconnectAttempts = 0;
    }

    public class MyWebSocketListener extends WebSocketListener {

        private LinkedBlockingQueue<Map> queue = new LinkedBlockingQueue<>();

        private void connect(LocalSocket localSocket) {
            while (true) {
                try {
                    localSocket.connect(new LocalSocketAddress("my_click_socket"));
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    continue;
                }
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response) {
            WebSocketManager.this.webSocket = webSocket;
            Map<String, Object> map = new HashMap<>();
//        map.put("type", "join");
//        map.put("roomId", 111);
            map.put("type", "getCode");
            map.put("mac", WebSocketManager.this.id);
            webSocket.send(JsonUtils.toJson(map));
            // 连接成功
            log("连接已打开");
            HandlerThread serverThread = new HandlerThread("WorkerThread");
            serverThread.start();
            Handler handler = new Handler(serverThread.getLooper());
            handler.post(() -> {
                while (true) {
                    Map data = queue.poll();
                    if (data == null) {
                        continue;
                    }
                    try (LocalSocket localSocket = new LocalSocket(); DataOutputStream output = new DataOutputStream(localSocket.getOutputStream())) {
                        connect(localSocket);
                        Integer typeInt = (Integer) data.get("typeInt");
                        output.writeInt(typeInt);
                        output.writeDouble((Double) data.get("id"));
                        output.writeDouble((Double) data.get("x"));
                        output.writeDouble((Double) data.get("y"));
                        if (typeInt == 4 || typeInt == 2) {
                            output.writeDouble((Double) data.get("x1"));
                            output.writeDouble((Double) data.get("y1"));
                        }
                        output.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            // 将文本消息转换为 ByteString（UTF-8 编码）
            ByteString byteString = ByteString.encodeUtf8(text);
            // 转发到二进制处理方法
            onMessage(webSocket, byteString);
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
                    if ("click".equals(data.get("type"))) {
                        data.put("typeInt", 1);
                        queue.offer(data);
                    } else if (data.get("type").equals("clickNotContinue")) {
                        data.put("typeInt", 5);
                        queue.offer(data);
                    } else if (data.get("type").equals("drag")) {
                        data.put("typeInt", 2);
                        queue.offer(data);
                    } else if (data.get("type").equals("end")) {
                        data.put("typeInt", 3);
                        queue.offer(data);
                    } else if ("continue".equals(data.get("type"))) {
                        data.put("typeInt", 4);
                        queue.offer(data);
                    } else if ("back".equals(data.get("type"))) {
                        data.put("typeInt", 6);
                        queue.offer(data);
                    } else if ("home".equals(data.get("type"))) {
                        data.put("typeInt", 7);
                        queue.offer(data);
                    } else if ("recent".equals(data.get("type"))) {
                        data.put("typeInt", 8);
                        queue.offer(data);
                    }
                    break;
                case "vailPassword":
                    Map ddd = message.getData();
                    String password = (String) ddd.get("password");
                    if ("admin".equals(password)) {
                        ddd.put("type", "vailSuccess");
                        webSocket.send(JsonUtils.toJson(ddd));
                        webRTCManager.createOffer(webRTCManager.localPeerConnection);
                    } else {
                        ddd.put("type", "vailFail");
                        webSocket.send(JsonUtils.toJson(ddd));
                    }
                    break;
                case "endLink":
                    break;
                case "getCode":
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "upLine");
                    map.put("code", message.getData().get("code"));
                    webSocket.send(JsonUtils.toJson(map));
                    break;
            }

            log("收到消息: " + string);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            // 连接关闭
            log("连接关闭: " + reason);
            scheduleReconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
            // 连接失败
            log("连接失败: " + t.getMessage());
            log("重新连接");
            scheduleReconnect(); // 触发重连
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
}
