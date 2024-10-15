package com.example.sgcdemo2.entity.mess;

public class WebViewEvent<T> {
    private String type;
    private String signal;
    private T data;

    public WebViewEvent(String type, String signal) {
        this.type = type;
        this.signal = signal;
    }

    public WebViewEvent(String type, T data) {
        this.type = type;
        this.data = data;
    }

    public WebViewEvent(String type, String signal, T data) {
        this.type = type;
        this.signal = signal;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
