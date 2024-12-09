package com.example.sgcdemo2.net;

import com.example.sgcdemo2.func.OnMessageReceivedListener;
import com.example.sgcdemo2.util.SeerState;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class SgcWsHandler {
    private static WebSocket webSocket;


    public static void startWs(OnMessageReceivedListener listener) {
        if (SeerState.mimiId == null) {
            return;
        }
        Request request = new Request.Builder()
                .url("wss://www.hakureif.site:8080/loginer?version=1.1.7&userid=seeraccount" + SeerState.mimiId)
                .build();
        webSocket = new OkHttpClient().newWebSocket(request, new SgcWsListener(listener));
    }

    public static void sendMess(String text) {
        webSocket.send(text);
    }

    public static void closeWs() {
        if (SgcWsListener.online) {
            webSocket.close(1000, "close");
        }
    }
}
