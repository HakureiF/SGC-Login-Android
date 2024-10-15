package com.example.sgcdemo2.js;

import android.webkit.JavascriptInterface;

import com.example.sgcdemo2.func.OnWebViewListner;
import com.example.sgcdemo2.util.SeerState;
import com.google.gson.Gson;

public class AndroidtoJs {
    private static Gson gson = new Gson();

    private OnWebViewListner listner;

    public AndroidtoJs(OnWebViewListner listner) {
        this.listner = listner;
    }

    @JavascriptInterface
    public void hasLogin(String mimi) {
        System.out.println(mimi + "登录游戏");
        SeerState.hasLogin = true;
        SeerState.mimiId = Integer.parseInt(mimi);
    }

    @JavascriptInterface
    public void recvData(String s) {
        System.out.println(s);
        listner.onWebViewMess(s);
    }
}
