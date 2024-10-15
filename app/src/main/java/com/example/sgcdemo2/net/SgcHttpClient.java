package com.example.sgcdemo2.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SgcHttpClient{
    public static String userId;
    public static String token;

    public static String host1;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static Gson gson = new Gson();
    private static OkHttpClient okHttpClient = new OkHttpClient();

    public Map<String, Object> get(String url) {
        Request request;
        if (userId != null && token != null) {
            request = new Request.Builder()
                    .url(host1 + url).header("seer-userid", userId).header("seer-token", token)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(host1 + url)
                    .build();
        }
        try (Response response = okHttpClient.newCall(request).execute()) {
            String jsonResponse = response.body().string();
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> res = gson.fromJson(jsonResponse, type);
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getString(String url) {
        Map<String, Object> res = get(url);
        return (String) res.get("data");
    }

    public Integer getInteger(String url) {
        Map<String, Object> res = get(url);
        Double temp = (Double) res.get("data");
        return temp.intValue();
    }

    public Map<String, Object> getMap(String url) {
        Map<String, Object> res = get(url);
        return (Map<String, Object>) res.get("data");
    }

    public Map<String, Object> post(String url, Object data) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = gson.toJson(data);
        System.out.println(json);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(host1 + url).header("seer-userid", userId).header("seer-token", token)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String jsonResponse = response.body().string();
            System.out.println(response.code());
            Type type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> res = gson.fromJson(jsonResponse, type);
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
