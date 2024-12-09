package com.example.sgcdemo2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
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
import com.example.sgcdemo2.dialog.GroupChooseFragment;
import com.example.sgcdemo2.dialog.JoinGameFragment;
import com.example.sgcdemo2.entity.BagPetVO;
import com.example.sgcdemo2.entity.mess.MessBody;
import com.example.sgcdemo2.entity.mess.WebViewEvent;
import com.example.sgcdemo2.func.OnDialogListener;
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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements OnMessageReceivedListener,
        OnDialogListener, OnWebViewListner {
    private static Gson gson = new Gson();

    public static String modMark = "";

    private DrawerLayout drawerLayout;
    private BpDialogFragment bpDialogFragment;
    private GroupChooseFragment groupChooseFragment;
    private JoinGameFragment joinGameFragment;
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        hideSystemUI();

        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.main);


        NavigationView navigationView = findViewById(R.id.navigation_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_match) {
                    modMark = "Match";
                    startConnect();
                }
                if (itemId == R.id.nav_create) {
                    modMark = "Create";
                    startConnect();
                }
                if (itemId == R.id.nav_join) {
                    modMark = "Join";
                    startConnect();
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
    protected void onResume() {
        super.onResume();
        hideSystemUI(); // 当界面恢复时重新调用，确保状态栏继续隐藏
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    /**
     * 接受bp窗口事件
     * @param event
     */
    @Override
    public void onDialogEvent(String event) {
        if (event.equals("Close")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
        } else if (event.startsWith("Group")) {
            String groupId = event.substring(5);
            afterHandRoomCreate(groupId);
        } else if (event.startsWith("JoinGame")) {
            String gameId = event.substring(8);
            afterHandRoomJoin(gameId);
        }
    }

    /**
     * 接受websocket消息
     * @param message
     */
    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("token")) {
            String token = message.split(":")[1];
            System.out.println(SeerState.mimiId);
            SgcHttpClient.token = token;
            SgcHttpClient.userId = AesUtil.encrypt("seeraccount" + SeerState.mimiId);
            SgcWsHandler.sendMess("tokenGot" + modMark);
        } else if (message.equals("matchJoin")) {
            runOnUiThread(this::afterHandMatch);
        } else if (message.equals("gameCreate")) {
            runOnUiThread(this::showGroupChooseDialog);
        } else if (message.equals("gameJoin")) {
            runOnUiThread(this::joinGameDialog);
        } else if (message.equals("onMatch")) {
            setSuitPets();
            bpDialogFragment.initGame();
        } else if (message.equals("SuccessQuitMatch")) {
            SgcWsHandler.closeWs();
        } else if (message.equals("RacePlayerNotFound")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "匹配通道目前用作比赛，不允许未参赛选手使用", Toast.LENGTH_SHORT).show());
        } else if (message.equals("RacePlayerMaxCount")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "你已达比赛到最大次数", Toast.LENGTH_SHORT).show());
        } else if (message.equals("All members are present")) {
            setSuitPets();
            bpDialogFragment.initGame();
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
        }  else if (message.equals("endGame")) {
            bpDialogFragment.dismiss();
            SeerState.resetBpState();
            SgcWsHandler.closeWs();
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "对局被关闭", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 游戏是否登录
     * @param mess
     * @return
     */
    private boolean unLogin(String mess) {
        if (mess.contains("\"type\":\"unLogin\"")) {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "请先登录游戏", Toast.LENGTH_SHORT).show());
        }
        return mess.contains("\"type\":\"unLogin\"");
    }

    /**
     * 接受webview消息
     * @param mess
     */
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

    /**
     * 读取公告
     */
    private void checkAnnouncement() {
        SgcHttpClient.host1 = "https://www.hakureif.site:8080";
        new Thread(() -> {
            // 在后台线程中执行网络请求
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            System.out.println(sgcHttpClient.getMap("/api/announcement/getLoginerAnnouncement"));
            // TODO 展示公告
        }).start();
    }

    /**
     * 展示bp窗口
     */
    private void showBpDialog() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        bpDialogFragment = new BpDialogFragment(screenWidth, screenHeight);
        bpDialogFragment.show(getSupportFragmentManager(), "BpDialog");
    }

    /**
     * 展示选择比赛组窗口
     */
    public void showGroupChooseDialog() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        groupChooseFragment = new GroupChooseFragment();
        groupChooseFragment.show(getSupportFragmentManager(), "GroupChooseDialog");
    }

    public void joinGameDialog() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        joinGameFragment = new JoinGameFragment();
        joinGameFragment.show(getSupportFragmentManager(), "GroupChooseDialog");
    }

    /**
     * 开启匹配模式
     */
    private void startConnect() {
        if (SgcWsListener.online) {
            showBpDialog();
        } else {
            SgcWsHandler.startWs(this);
        }
    }

    /**
     * websocket连接建立匹配后执行
     */
    private void afterHandMatch() {
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
                    handleGetBagMatch(res);
                }
            }
        });
    }

    private void afterHandRoomCreate(String groupId) {
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
                    handleGetBagRoomCreate(res, groupId);
                }
            }
        });
    }

    private void afterHandRoomJoin(String gameId) {
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
                    handleGetBagRoomJoin(res, gameId);
                }
            }
        });
    }

    /**
     * 校验背包，然后校验套装
     * @param bagData
     */
    private void handleGetBagMatch(MessBody<List<Integer>, BagPetVO> bagData) {
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
                            Type type = new TypeToken<MessBody<Integer, Object>>() {
                            }.getType();
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


    private void handleGetBagRoomCreate(MessBody<List<Integer>, BagPetVO> bagData, String groupId) {
        SgcHttpClient.host1 = "https://www.hakureif.site:8080";
        new Thread(() -> {
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            Map<String, Object> body = new HashMap<>();
            body.put("bagInfo", bagData.getData2());
            body.put("matchGame", false);
            body.put("groupId", groupId);
            Map<String, Object> resp = sgcHttpClient.post("/api/conventional/verifyBag", body);
            if (resp != null && resp.containsKey("code")) {
                if ((Double) resp.get("code") == 200) {
                    // 背包校验完成，校验套装
                    runOnUiThread(() -> {
                        WebView webView = findViewById(R.id.webView);
                        WebViewEvent<Object> event = new WebViewEvent<>("getSuit", "match");
                        webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", s -> {
                            Type type = new TypeToken<MessBody<Integer, Object>>() {
                            }.getType();
                            MessBody<Integer, Object> resSuit = gson.fromJson(s, type);
                            SeerState.suit = resSuit.getData();
                            Map<String, Object> bodySuit = new HashMap<>();
                            bodySuit.put("suitId", resSuit.getData());
                            bodySuit.put("matchGame", false);
                            bodySuit.put("groupId", groupId);
                            new Thread(() -> {
                                SgcHttpClient sgcHttpClient2 = new SgcHttpClient();
                                Map<String, Object> respSuit = sgcHttpClient2.post("/api/conventional/verifySuit", bodySuit);
                                if (respSuit != null) {
                                    if ((Double) respSuit.get("code") == 200) {
                                        SgcHttpClient sgcHttpClient3 = new SgcHttpClient();
                                        Map<String, Object> respRoom = sgcHttpClient3.getMap("/api/game-information/generateConventionalGame?groupId=" + groupId);
                                        String gameId = (String) respRoom.get("gameId");
                                        SeerState.gameId = gameId;
                                        runOnUiThread(() -> {
                                            // 获取ClipboardManager
                                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                            // 创建ClipData对象
                                            ClipData clip = ClipData.newPlainText("label", gameId);
                                            // 将ClipData放入剪贴板
                                            clipboard.setPrimaryClip(clip);
                                            Toast.makeText(this, "房间号已复制！", Toast.LENGTH_SHORT).show();
                                            modMark = "";
                                            groupChooseFragment.dismiss();
                                            showBpDialog();
                                        });
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

    private void handleGetBagRoomJoin(MessBody<List<Integer>, BagPetVO> bagData, String gameId) {
        SgcHttpClient.host1 = "https://www.hakureif.site:8080";
        new Thread(() -> {
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            Map<String, Object> body = new HashMap<>();
            body.put("bagInfo", bagData.getData2());
            body.put("matchGame", false);
            body.put("gameId", gameId);
            Map<String, Object> resp = sgcHttpClient.post("/api/conventional/verifyBag", body);
            if (resp != null && resp.containsKey("code")) {
                if ((Double) resp.get("code") == 200) {
                    // 背包校验完成，校验套装
                    runOnUiThread(() -> {
                        WebView webView = findViewById(R.id.webView);
                        WebViewEvent<Object> event = new WebViewEvent<>("getSuit", "match");
                        webView.evaluateJavascript("handle(" + gson.toJson(event) + ")", s -> {
                            Type type = new TypeToken<MessBody<Integer, Object>>() {
                            }.getType();
                            MessBody<Integer, Object> resSuit = gson.fromJson(s, type);
                            SeerState.suit = resSuit.getData();
                            Map<String, Object> bodySuit = new HashMap<>();
                            bodySuit.put("suitId", resSuit.getData());
                            bodySuit.put("matchGame", false);
                            bodySuit.put("gameId", "game" + gameId);
                            new Thread(() -> {
                                SgcHttpClient sgcHttpClient2 = new SgcHttpClient();
                                Map<String, Object> respSuit = sgcHttpClient2.post("/api/conventional/verifySuit", bodySuit);
                                if (respSuit != null) {
                                    if ((Double) respSuit.get("code") == 200) {
                                        SgcHttpClient sgcHttpClient3 = new SgcHttpClient();
                                        Map<String, String> joinBody = new HashMap<>();
                                        joinBody.put("gameId", gameId);
                                        Map<String, Object> respJoin = sgcHttpClient3.post("/api/game-information/joinConventionalGame", joinBody);

                                        runOnUiThread(() -> {
                                            modMark = "";
                                            joinGameFragment.dismiss();
                                            showBpDialog();
                                        });
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

    /**
     * 设置套装和背包精灵
     */
    private void setSuitPets() {
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
    }
}