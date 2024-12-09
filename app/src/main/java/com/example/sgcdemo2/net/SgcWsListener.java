package com.example.sgcdemo2.net;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sgcdemo2.MainActivity;
import com.example.sgcdemo2.func.OnMessageReceivedListener;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SgcWsListener extends WebSocketListener {
    public static boolean online = false;

    private OnMessageReceivedListener listener;

    public SgcWsListener(OnMessageReceivedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        super.onClosed(webSocket, code, reason);
        online = false;
        System.out.println("ws closed");
        System.out.println(MainActivity.modMark);
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        super.onClosing(webSocket, code, reason);
        System.out.println("ws closing");
        System.out.println(MainActivity.modMark);
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        System.out.println(t.getMessage());
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        super.onMessage(webSocket, text);
        System.out.println(text);
        listener.onMessageReceived(text);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        super.onOpen(webSocket, response);
        online = true;
    }
}
