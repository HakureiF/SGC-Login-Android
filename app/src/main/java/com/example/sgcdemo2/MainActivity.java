package com.example.sgcdemo2;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.sgcdemo2.dialog.BpDialogFragment;
import com.example.sgcdemo2.entity.BagPetVO;
import com.example.sgcdemo2.entity.mess.MessBody;
import com.example.sgcdemo2.entity.mess.WebViewEvent;
import com.example.sgcdemo2.func.OnBpDialogListener;
import com.example.sgcdemo2.func.OnMessageReceivedListener;
import com.example.sgcdemo2.func.OnWebViewListner;
import com.example.sgcdemo2.js.AndroidtoJs;
import com.example.sgcdemo2.util.AesUtil;
import com.example.sgcdemo2.util.ResourceReader;
import com.example.sgcdemo2.util.SeerState;
import com.example.sgcdemo2.net.SgcHttpClient;
import com.example.sgcdemo2.net.SgcWsHandler;
import com.example.sgcdemo2.net.SgcWsListener;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements OnMessageReceivedListener,
        OnBpDialogListener, OnWebViewListner {
    private static Gson gson = new Gson();

    private DrawerLayout drawerLayout;
    private BpDialogFragment bpDialogFragment;
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main);


        NavigationView navigationView = findViewById(R.id.navigation_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_match) {
                    startMatch();
                }
                if (itemId == R.id.nav_create) {

                }
                if (itemId == R.id.nav_fresh) {
                    runOnUiThread(() -> {
                        WebView webView = findViewById(R.id.webView);
                        webView.reload();
                        SgcWsHandler.closeWs();
                        SeerState.resetBpState();
                    });
                }
                item.setChecked(true);
                drawerLayout.closeDrawers();
                return true;
            });
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_match, R.id.nav_create)
                    .setOpenableLayout(drawerLayout)
                    .build();
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化 WebView
        WebView webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        Context context = this;

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                String rewriteScript = ResourceReader.readTextFileFromResource(context, R.raw.rewritescript);
                String loginscript = ResourceReader.readTextFileFromResource(context, R.raw.loginscript);
                String funcscript = ResourceReader.readTextFileFromResource(context, R.raw.funcscript);
                webView.evaluateJavascript(
                        "document.querySelectorAll('audio, video').forEach(media => media.muted = true);",
                        null
                );
                webView.evaluateJavascript(rewriteScript, null);
                webView.evaluateJavascript(loginscript, null);
                webView.evaluateJavascript(funcscript, null);
            }
        });


        webView.addJavascriptInterface(new AndroidtoJs(this), "androidtojs");
        // 加载网页
        webView.loadUrl("https://seerh5.61.com/");

        checkAnnouncement();
        String specialHeadStr = ResourceReader.readTextFileFromResource(context, R.raw.special_petheads);
        SeerState.getSpecialHeads(specialHeadStr);
    }

    @Override
    public void onBpDialogEvent(String event) {
        if (event.equals("Close")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
        }
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("token")) {
            String token = message.split(":")[1];
            System.out.println(token);
            SgcHttpClient.token = token;
            SgcHttpClient.userId = AesUtil.encrypt("seeraccount" + SeerState.mimiId);
            runOnUiThread(this::afterHand);
        } else if (message.equals("onMatch")) {
            SeerState.resetBpState();

            // 设置套装
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                Map<String, Object> respSuit = sgcHttpClient.get("/api/game-information/setConventionalSuit?suitId=" + SeerState.suit);
                if (respSuit != null) {
                    if ((Double) respSuit.get("code") == 201) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), (String) respSuit.get("message"), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_SHORT).show());
                }
            }).start();

            // 设置精灵背包
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                Map<String, Object> respPets = sgcHttpClient.post("/api/conventional/freshBag", SeerState.pets);
                if (respPets != null) {
                    if ((Double) respPets.get("code") == 201) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), (String) respPets.get("message"), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_SHORT).show());
                }
            }).start();
            bpDialogFragment.initGame();
        } else if (message.equals("SuccessQuitMatch")) {
            SgcWsHandler.closeWs();
        } else if (message.equals("RacePlayerNotFound")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "匹配通道目前用作比赛，不允许未参赛选手使用", Toast.LENGTH_SHORT).show());
        } else if (message.equals("RacePlayerMaxCount")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "你已达比赛到最大次数", Toast.LENGTH_SHORT).show());
        } else if (message.equals("ReadyStage")) {
            bpDialogFragment.initGame();
        } else if (message.equals("PlayerBanElf")) {
            bpDialogFragment.initGame();
        } else if (message.equals("PlayerPickElfFirst")) {
            bpDialogFragment.initGame();
        } else if (message.equals("PlayerPickElfRemain")) {
            bpDialogFragment.initGame();
        } else if (message.equals("WaitingPeriodResult")) {
            bpDialogFragment.initGame();
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                Map<String, Object> resp = sgcHttpClient.getMap("/api/conventional/getPetState");
                if (resp != null ) {
                    runOnUiThread(() -> {
                        Map<Integer, BagPetVO> addBag = new HashMap<>();
                        List<BagPetVO> pets;
                        if (Objects.equals(SeerState.type, "Player1")) {
                            String dataStr = gson.toJson(resp.get("Player1PetState"));
                            Type type = new TypeToken<List<BagPetVO>>(){}.getType();
                            pets = gson.fromJson(dataStr, type);
                        } else {
                            String dataStr = gson.toJson(resp.get("Player2PetState"));
                            Type type = new TypeToken<List<BagPetVO>>(){}.getType();
                            pets = gson.fromJson(dataStr, type);
                        }
                        // 先放入首发
                        for (BagPetVO pet: pets) {
                            if (pet.getState() == 2) {
                                addBag.put(0, pet);
                                addBag.put(pet.getCatchTime(), pet);
                            }
                        }
                        // 再放入其他出战
                        Integer count = 1;
                        for (BagPetVO pet: pets) {
                            if (pet.getState() == 3) {
                                addBag.put(count, pet);
                                addBag.put(pet.getCatchTime(), pet);
                                count ++;
                            }
                        }
                        System.out.println(gson.toJson(addBag));
                        WebViewEvent<Map<Integer, BagPetVO>> event = new WebViewEvent<>("addToBagFull", addBag);
                        WebView webView = findViewById(R.id.webView);
                        webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", s -> {
                            if (Objects.equals(SeerState.type, "Player1")) {
                                runOnUiThread(() -> {
                                    WebViewEvent<Object> createRoomEvent = new WebViewEvent<>("createRoom", "");
                                    webView.evaluateJavascript("handle(" + gson.toJson(createRoomEvent) + ")", null);
                                });
                            }
                        });
                    });
                }
            }).start();
        } else if (message.contains("RoomId")) {
            Integer roomId = Integer.parseInt(message.substring(6));
            runOnUiThread(() -> {
                WebViewEvent<Integer> event = new WebViewEvent<>("joinRoom", roomId);
                WebView webView = findViewById(R.id.webView);
                webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", null);
            });
        } else if (message.equals("shutRoom")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
            SgcWsHandler.closeWs();
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "对局结束", Toast.LENGTH_SHORT).show());
        } else if (message.equals("quitRoom")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
            SgcWsHandler.closeWs();
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "对局结束", Toast.LENGTH_SHORT).show());
        } else if (message.equals("offLine")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
            SgcWsHandler.closeWs();
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "对方退出或掉线", Toast.LENGTH_SHORT).show());
        }
    }

    private boolean unLogin(String mess) {
        if (mess.contains("\"type\":\"unLogin\"")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "请先登录游戏", Toast.LENGTH_SHORT).show());
        }
        return mess.contains("\"type\":\"unLogin\"");
    }

    @Override
    public void onWebViewMess(String mess) {
        if (mess.contains("\"type\":\"isWinner\"")) {
            Type type = new TypeToken<MessBody<Boolean, Object>>(){}.getType();
            MessBody<Boolean, Object> messBody = gson.fromJson(mess, type);
            SgcWsHandler.sendMess("isWinner" + messBody.getData());
        } else if (mess.contains("\"type\":\"fightOverClick\"")) {
            SgcWsHandler.sendMess("endGame");
        } else if (mess.contains("\"type\":\"roomIdCreated\"")) {
            Type type = new TypeToken<MessBody<Integer, Object>>(){}.getType();
            MessBody<Integer, Object> messBody = gson.fromJson(mess, type);
            SgcWsHandler.sendMess("RoomId" + messBody.getData());
        }
    }

    private void showBpDialog() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        bpDialogFragment = new BpDialogFragment(screenWidth, screenHeight);
        bpDialogFragment.show(getSupportFragmentManager(), "BpDialog");
    }


    private void checkAnnouncement() {
        SgcHttpClient.host1 = "https://www.hakureif.site:8080";
        new Thread(() -> {
            // 在后台线程中执行网络请求
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            System.out.println(sgcHttpClient.getMap("/api/announcement/getLoginerAnnouncement"));
        }).start();
    }


    private void startMatch() {
        if (SgcWsListener.online) {
            showBpDialog();
        } else {
            SgcWsHandler.start(this);
        }
    }

    private void afterHand() {
        WebView webView = findViewById(R.id.webView);
        WebViewEvent<Object> event = new WebViewEvent<>("getBag", "match");
        System.out.println("handle(" + gson.toJson(event) + ")");
        webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", s -> {
            if (!unLogin(s)) {
                System.out.println(s);
                Type type = new TypeToken<MessBody<List<Integer>, BagPetVO>>(){}.getType();
                MessBody<List<Integer>, BagPetVO> res = gson.fromJson(s, type);
                String checkBag = SeerState.checkBag(res.getData2());
                if (checkBag != null) {
                    SgcWsHandler.closeWs();
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), checkBag, Toast.LENGTH_SHORT).show());
                } else {
                    SeerState.pets = res.getData2();
                    handleGetBag(res);
                }
            }
        });
    }

    private void handleGetBag(MessBody<List<Integer>, BagPetVO> bagData) {
        if (bagData.getSignal().equals("match")) {
            SgcHttpClient.host1 = "https://www.hakureif.site:8080";
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                Map<String, Object> body = new HashMap<>();
                body.put("bagInfo", bagData.getData2());
                body.put("matchGame", true);
                Map<String, Object> resp = sgcHttpClient.post("/api/conventional/verifyBag", body);
                if (resp != null && resp.containsKey("code")) {
                    if ((Double) resp.get("code") == 200) {
                        // 背包校验完成，校验套装
                        runOnUiThread(() -> {
                            WebView webView = findViewById(R.id.webView);
                            WebViewEvent<Object> event = new WebViewEvent<>("getSuit", "match");
                            webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", s -> {
                                Type type = new TypeToken<MessBody<Integer, Object>>(){}.getType();
                                MessBody<Integer, Object> resSuit = gson.fromJson(s, type);
                                SeerState.suit = resSuit.getData();
                                Map<String, Object> bodySuit = new HashMap<>();
                                bodySuit.put("suitId", resSuit.getData());
                                bodySuit.put("matchGame", true);
                                new Thread(() -> {
                                    SgcHttpClient sgcHttpClient2 = new SgcHttpClient();
                                    Map<String, Object> respSuit = sgcHttpClient2.post("/api/conventional/verifySuit", bodySuit);
                                    if (respSuit != null) {
                                        if ((Double) respSuit.get("code") == 200) {
                                            SgcWsHandler.sendMess("JoinMatch");
                                            SeerState.phase = "match";
                                            runOnUiThread(this::showBpDialog);
                                        }
                                        if ((Double) respSuit.get("code") == 201) {
                                            SgcWsHandler.closeWs();
                                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), (String) respSuit.get("message"), Toast.LENGTH_SHORT).show());
                                        }
                                    } else {
                                        SgcWsHandler.closeWs();
                                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_SHORT).show());
                                    }
                                }).start();
                            });
                        });
                    }
                    if ((Double) resp.get("code") == 201) {
                        SgcWsHandler.closeWs();
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), (String) resp.get("message"), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    SgcWsHandler.closeWs();
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "未知错误", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }
}