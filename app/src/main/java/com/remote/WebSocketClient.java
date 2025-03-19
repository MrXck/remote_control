package com.remote;

import com.remote.pojo.Message;
import com.remote.utils.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {

    private final String url;

    public WebSocketClient(String url) {
        this.url = url;
    }


    private WebSocket webSocket = null;
    private final OkHttpClient client = new OkHttpClient();

    // 连接 WebSocket
    public void connect(WebSocketListener listener) {
        Request request = new Request.Builder()
                .url(url)
            .build();
        webSocket = client.newWebSocket(request, listener);
    }

    // 发送消息
    public void sendMessage(Message message) {
        webSocket.send(JsonUtils.toJson(message));
    }

    // 关闭连接
    private void disconnect() {
        webSocket.close(1000, "正常关闭");
    }
}