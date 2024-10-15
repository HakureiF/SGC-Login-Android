package com.example.sgcdemo2.util;

import com.example.sgcdemo2.R;
import com.example.sgcdemo2.entity.BagPetVO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SeerState {
    private static Gson gson = new Gson();

    // SeerState
    public static boolean hasLogin = false;
    public static Integer mimiId = null;
    public static Integer suit = null;
    public static List<BagPetVO> pets;

    // BpState
    public static String phase;
    public static String type;
    public static Integer timeCount = 0;
    public static Integer banNum = 3;
    public static Integer banCount = 0;
    public static Integer pickCount = 0;
    public static Integer player1Suit;
    public static Integer player2Suit;
    public static List<BagPetVO> player1Pets;
    public static List<BagPetVO> player2Pets;


    public static final String HEAD_TAOMEE = "https://seerh5.61.com/resource/assets/pet/head/";
    public static final String HEAD_SGC = "https://cdn.imrightchen.live/img/elf-head/";
    public static final String SUIT_TAOMEE = "https://seerh5.61.com/resource/assets/item/cloth/suiticon/";
//    public static Map<String, Map<String, String>> specialHead;
    public static Map<String, String> specialHead;

    public static void resetBpState() {
        phase = null;
        type = null;
        timeCount = 0;
        banNum = 3;
        banCount = 0;
        pickCount = 0;
        player1Suit = 0;
        player2Suit = 0;
        player1Pets = null;
        player2Pets = null;
    }

    public static String handleHeadUrl(int id)
    {
        if (id > 5000)
        {
            return HEAD_SGC + id + ".png";
        } else {
            if (specialHead != null && specialHead.containsKey(String.valueOf(id))) {
                return HEAD_TAOMEE + specialHead.get(String.valueOf(id)) + ".png";
            }
            return HEAD_TAOMEE + id + ".png";
        }
    }

//    public static void getSpecialHeads() {
//        new Thread(() -> {
//            // 在后台线程中执行网络请求
//            OkHttpClient okHttpClient = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url("https://cdn.imrightchen.live/json/special_petheads.json")
//                    .build();
//            try (Response response = okHttpClient.newCall(request).execute()) {
//                String jsonResponse = response.body().string();
//                Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
//                specialHead = gson.fromJson(jsonResponse, type);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//    }

    public static void getSpecialHeads(String specialHeadStr) {
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        specialHead = gson.fromJson(specialHeadStr, type);
    }

    public static String checkBag(List<BagPetVO> pets) {
        if (pets == null) return "获取背包数据失败";
        if (pets.size() < 9) return "请至少在背包中携带9只精灵";
        for (int i=0; i<pets.size(); i++) {
            if (pets.get(i).getLevel() < 100) return "请勿携带不满级的精灵";
            for (int j=i+1; j<pets.size(); j++) {
                if (pets.get(i).getId() == pets.get(j).getId() && pets.get(i).getEffectID() == pets.get(j).getEffectID()) {
                    return "请勿携带相同精灵";
                }
            }
        }
        return null;
    }
}
