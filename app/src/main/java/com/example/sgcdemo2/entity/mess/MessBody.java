package com.example.sgcdemo2.entity.mess;

import java.util.List;

public class MessBody<T, K> {
    private String type;
    private String signal;
    private T data;
    private List<K> data2;

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

    public List<K> getData2() {
        return data2;
    }

    public void setData2(List<K> data2) {
        this.data2 = data2;
    }
}
