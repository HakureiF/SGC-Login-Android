package com.example.sgcdemo2.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.sgcdemo2.R;
import com.example.sgcdemo2.entity.BagPetVO;
import com.example.sgcdemo2.func.OnDialogListener;
import com.example.sgcdemo2.net.SgcHttpClient;
import com.example.sgcdemo2.net.SgcWsHandler;
import com.example.sgcdemo2.net.SgcWsListener;
import com.example.sgcdemo2.util.SeerState;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BpDialogFragment extends DialogFragment {
    private static Gson gson = new Gson();

    OnDialogListener listener;
    private boolean clickAble;
    private View bpView;
    private CountDownTimer countDownTimer;

    private int screenWidth;
    private int screenHeight;


    public BpDialogFragment(int screenWidth, int screenHeight) {
        super();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MyDialogListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 获取当前Dialog的Window对象
        if (getDialog() != null && getDialog().getWindow() != null) {
            // 设置Dialog的宽度为屏幕宽度，高度为包裹内容
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_layout, null);
        bpView = view;

        syncGameState();
        initViews();

        builder.setView(view);
        return builder.create();
    }

    public void initViews() {
        ViewGroup viewGroup = bpView.findViewById(R.id.grid_layout);
        for (int i=0; i<viewGroup.getChildCount(); i++) {
            View subView = viewGroup.getChildAt(i);
            if (subView instanceof ImageView) {
                ImageView imageView = (ImageView) subView;
                String strId = getResources().getResourceEntryName(imageView.getId());
                if (strId.startsWith("player1PetHead") || strId.startsWith("player2PetHead")) {
                    imageView.setOnClickListener(this::onPetHeadClick);

                    ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                    layoutParams.width = screenHeight / 7;
                    layoutParams.height = screenHeight / 7;
                    imageView.setLayoutParams(layoutParams);

                    Glide.with(this).load(SeerState.handleHeadUrl(1)).into(imageView);
                }
            }
            if (subView instanceof Button) {
                Button button = (Button) subView;
                String strId = getResources().getResourceEntryName(button.getId());
                switch (strId) {
                    case "freshButton":
                        button.setText("刷新");
                        button.setOnClickListener(this::onFreshButtonClick);
                        break;
                    case "readyButton":
                        button.setOnClickListener(this::onReadyButtonClick);
                        break;
                    case "quitButton":
                        button.setText("退出");
                        button.setOnClickListener(this::onQuitButtonClick);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void initGame() {
        if (!SgcWsListener.online) return;
        new Thread(() -> {
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            SeerState.phase = sgcHttpClient.getString("/api/game-information/getPhase");
            if (SeerState.phase != null) {
                syncGameState();
            }
        }).start();
    }

    public void syncGameState() {
        System.out.println(bpView == null);
        if (!SgcWsListener.online || bpView == null) return;
        if (Objects.equals(SeerState.phase, "match")) {
            bpView.post(() -> {
                TextView phaseContet = bpView.findViewById(R.id.phaseContent);
                phaseContet.setText("匹配中");
            });
        } else {
            bpView.post(() -> {
                TextView phaseContet = bpView.findViewById(R.id.phaseContent);
                clickAble = false;
                if (Objects.equals(SeerState.phase, "PlayerBanElf")) {
                    phaseContet.setText("禁用");
                    clickAble = true;
                } else if (Objects.equals(SeerState.phase, "PlayerPickElfFirst")) {
                    phaseContet.setText("选择首发");
                    clickAble = true;
                } else if (Objects.equals(SeerState.phase, "PlayerPickElfRemain")) {
                    phaseContet.setText("选择出战");
                    clickAble = true;
                } else if (Objects.equals(SeerState.phase, "WaitingPeriodResult")) {
                    phaseContet.setText("等待结束");
                }
            });


            // type
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                SeerState.type = sgcHttpClient.getString("/api/game-information/getType");

                bpView.post(() -> {
                    TextView phaseContet = bpView.findViewById(R.id.phaseContent);
                    Button readyButton = bpView.findViewById(R.id.readyButton);
                    if (Objects.equals(SeerState.type, "Player1")) {
                        readyButton.setText("开始");
                    } else {
                        readyButton.setText("准备");
                    }
                    if (Objects.equals(SeerState.phase, "PreparationStage")) {
                        phaseContet.setText("等待准备");
                        readyButton.setEnabled(!Objects.equals(SeerState.type, "Player1"));
                    } else if (Objects.equals(SeerState.phase, "ReadyStage")) {
                        phaseContet.setText("等待开始");
                        readyButton.setEnabled(Objects.equals(SeerState.type, "Player1"));
                    }

                    // 精灵和套装都要获取到type后再加载
                    syncPetState();
                    new Thread(() -> {
                        Map<String, Object> suitResp = sgcHttpClient.getMap("/api/game-information/getPickSuit");
                        if (suitResp != null && suitResp.containsKey("Player1PickSuit") && suitResp.get("Player1PickSuit") != null &&
                                !Objects.equals(suitResp.get("Player1PickSuit"), "")) {
                            SeerState.player1Suit = Integer.parseInt((String) suitResp.get("Player1PickSuit"));
                            bpView.post(() -> {
                                ImageView player1SuitImg = bpView.findViewById(R.id.player1SuitImg);
                                Glide.with(bpView).load(SeerState.SUIT_TAOMEE + SeerState.player1Suit + ".png").into(player1SuitImg);
                            });

                        }
                        if (suitResp != null && suitResp.containsKey("Player2PickSuit") && suitResp.get("Player2PickSuit") != null &&
                                !Objects.equals(suitResp.get("Player2PickSuit"), "")) {
                            SeerState.player2Suit = Integer.parseInt((String) suitResp.get("Player2PickSuit"));
                            bpView.post(() -> {
                                ImageView player2SuitImg = bpView.findViewById(R.id.player2SuitImg);
                                Glide.with(bpView).load(SeerState.SUIT_TAOMEE + SeerState.player2Suit + ".png").into(player2SuitImg);
                            });
                        }
                    }).start();
                });

            }).start();
            // BanNum
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                SeerState.banNum = sgcHttpClient.getInteger("/api/conventional/getBanNum");
            }).start();
            // CountDown
            new Thread(() -> {
                SgcHttpClient sgcHttpClient = new SgcHttpClient();
                SeerState.timeCount = sgcHttpClient.getInteger("/api/game-information/getCountTime");
                bpView.post(() -> {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    countDownTimer = new CountDownTimer(1000L * SeerState.timeCount, 1000) {
                        @Override
                        public void onTick(long l) {
                            if (l > 0 && l < 10000) {
                                bpView.post(() -> {
                                    TextView timeCountView = bpView.findViewById(R.id.timeCount);
                                    timeCountView.setText("0" + l/1000);
                                });
                            } else if (l > 10000) {
                                bpView.post(() -> {
                                    TextView timeCountView = bpView.findViewById(R.id.timeCount);
                                    timeCountView.setText("" + l/1000);
                                });
                            } else {
                                bpView.post(() -> {
                                    TextView timeCountView = bpView.findViewById(R.id.timeCount);
                                    timeCountView.setText("00");
                                });
                            }
                        }

                        @Override
                        public void onFinish() {}
                    }.start();
                });
            }).start();
        }
    }

    public void syncPetState() {
        new Thread(() -> {
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            Map<String, Object> resp = sgcHttpClient.getMap("/api/conventional/getPetState");
            if (resp != null ) {
                if (resp.containsKey("Player1PetState") && resp.get("Player1PetState") != null) {
                    String dataStr = gson.toJson(resp.get("Player1PetState"));
                    Type type = new TypeToken<List<BagPetVO>>(){}.getType();
                    SeerState.player1Pets = gson.fromJson(dataStr, type);
                }
                if (resp.containsKey("Player2PetState") && resp.get("Player2PetState") != null) {
                    String dataStr = gson.toJson(resp.get("Player2PetState"));
                    Type type = new TypeToken<List<BagPetVO>>(){}.getType();
                    SeerState.player2Pets = gson.fromJson(dataStr, type);
                }

                bpView.post(() -> {
                    ViewGroup viewGroup = bpView.findViewById(R.id.grid_layout);
                    for (int i=0; i<viewGroup.getChildCount(); i++) {
                        View subView = viewGroup.getChildAt(i);
                        if (subView instanceof ImageView) {
                            ImageView imageView = (ImageView) subView;
                            String strId = getResources().getResourceEntryName(imageView.getId());
                            List<BagPetVO> pets = new ArrayList<>();
                            if (strId.contains("PetHead")) {
                                if (strId.startsWith("player1PetHead") && SeerState.player1Pets != null) {
                                    pets = SeerState.player1Pets;
                                }
                                if (strId.startsWith("player2PetHead") && SeerState.player2Pets != null) {
                                    pets = SeerState.player2Pets;
                                }
                                int petSeq = Integer.parseInt(strId.substring(14));
                                if (petSeq <= pets.size()) {
                                    BagPetVO petVO = pets.get(petSeq - 1);
                                    imageView.setTag(petVO.getState());
                                    switch (petVO.getState()) {
                                        case 0:
                                            imageView.clearColorFilter();
                                            break;
                                        case 1:
                                            imageView.setColorFilter(Color.parseColor("#80000000"), PorterDuff.Mode.SRC_ATOP);
                                            break;
                                        case 2:
                                            imageView.setColorFilter(Color.parseColor("#8000ff00"), PorterDuff.Mode.SRC_ATOP);
                                            break;
                                        case 3:
                                            imageView.setColorFilter(Color.parseColor("#800000ff"), PorterDuff.Mode.SRC_ATOP);
                                            break;
                                        default:
                                            break;
                                    }
                                    Glide.with(this).load(SeerState.handleHeadUrl(petVO.getId())).into(imageView);
                                }
                            }
                        }
                    }
                });
            }
        }).start();
    }

    public void setClickAble(boolean t) {
        this.clickAble = t;
    }

    public void onPetHeadClick(View img) {
        if (clickAble) {
            ImageView imgView = (ImageView) img;
            String strId = getResources().getResourceEntryName(imgView.getId());
            int petSeq = Integer.parseInt(strId.substring(14)) - 1;

            if (strId.startsWith(SeerState.type.toLowerCase())) { // 点击自己的精灵头像
                Integer state = SeerState.type.equals("Player1")? SeerState.player1Pets.get(petSeq).getState(): SeerState.player2Pets.get(petSeq).getState();
                if (SeerState.phase.equals("PlayerPickElfFirst")) {
                    if (state == 0) {
                        System.out.println("click to Pick First");
                        new AlertDialog.Builder(this.getContext())
                            .setTitle("确认操作")
                            .setMessage("确定首发此精灵？")
                            .setPositiveButton("确定", (dialog, which) -> {
//                                SeerState.pickCount ++;
                                if (SeerState.type.equals("Player1")) {
                                    SeerState.player1Pets.get(petSeq).setState(2);
                                    SgcWsHandler.sendMess("PickElfFirst" + SeerState.player1Pets.get(petSeq).getId());
                                } else {
                                    SeerState.player2Pets.get(petSeq).setState(2);
                                    SgcWsHandler.sendMess("PickElfFirst" + SeerState.player2Pets.get(petSeq).getId());
                                }
                                imgView.setColorFilter(Color.parseColor("#8000ff00"), PorterDuff.Mode.SRC_ATOP);
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .show();
                    }
                }
                if (SeerState.phase.equals("PlayerPickElfRemain") && SeerState.pickCount < 5) {
                    if (state == 0) {
                        System.out.println("click to Pick Remain");
                        if (SeerState.pickCount == 4) {
                            new AlertDialog.Builder(this.getContext())
                                .setTitle("确认操作")
                                .setMessage("确定出战精灵？")
                                .setPositiveButton("确定", (dialog, which) -> {
//                                    SeerState.pickCount ++;
                                    List<Integer> pickRemain = new ArrayList<>();
                                    if (SeerState.type.equals("Player1")) {
                                        SeerState.player1Pets.get(petSeq).setState(3);
                                        for (BagPetVO pet: SeerState.player1Pets) {
                                            if (pet.getState() == 3) pickRemain.add(pet.getId());
                                        }
                                    } else {
                                        SeerState.player2Pets.get(petSeq).setState(3);
                                        for (BagPetVO pet: SeerState.player2Pets) {
                                            if (pet.getState() == 3) pickRemain.add(pet.getId());
                                        }
                                    }
                                    SgcWsHandler.sendMess("PickElfRemain" + gson.toJson(pickRemain));
                                    imgView.setColorFilter(Color.parseColor("#800000ff"), PorterDuff.Mode.SRC_ATOP);
                                    SeerState.pickCount ++;
                                })
                                .setNegativeButton("取消", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();
                        } else {
                            SeerState.pickCount++;
                            if (SeerState.type.equals("Player1")) {
                                SeerState.player1Pets.get(petSeq).setState(3);
                            } else {
                                SeerState.player2Pets.get(petSeq).setState(3);
                            }
                            imgView.setColorFilter(Color.parseColor("#800000ff"), PorterDuff.Mode.SRC_ATOP);
                        }
                    }
                    if (state == 3) {
                        SeerState.pickCount--;
                        if (SeerState.type.equals("Player1")) {
                            SeerState.player1Pets.get(petSeq).setState(0);
                        } else {
                            SeerState.player2Pets.get(petSeq).setState(0);
                        }
                        imgView.clearColorFilter();
                    }
                }
            } else { // 点击对手的精灵头像
                Integer state = SeerState.type.equals("Player1")? SeerState.player2Pets.get(petSeq).getState(): SeerState.player1Pets.get(petSeq).getState();
                if (SeerState.phase.equals("PlayerBanElf")) {
                    if (state == 0) {
                        System.out.println("click to Ban");
                        if (SeerState.banCount == SeerState.banNum-1) {
                            new AlertDialog.Builder(this.getContext())
                                .setTitle("确认操作")
                                .setMessage("确定禁用精灵？")
                                .setPositiveButton("确定", (dialog, which) -> {
//                                    SeerState.banCount ++;
                                    List<Integer> ban = new ArrayList<>();
                                    if (SeerState.type.equals("Player1")) {
                                        SeerState.player2Pets.get(petSeq).setState(1);
                                        for (BagPetVO pet: SeerState.player2Pets) {
                                            if (pet.getState() == 1) ban.add(pet.getId());
                                        }
                                    } else {
                                        SeerState.player1Pets.get(petSeq).setState(1);
                                        for (BagPetVO pet: SeerState.player1Pets) {
                                            if (pet.getState() == 1) ban.add(pet.getId());
                                        }
                                    }
                                    SgcWsHandler.sendMess("PlayerBan" + gson.toJson(ban));
                                    imgView.setColorFilter(Color.parseColor("#80000000"), PorterDuff.Mode.SRC_ATOP);
                                })
                                .setNegativeButton("取消", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .show();
                        } else {
                            SeerState.banCount++;
                            if (SeerState.type.equals("Player1")) {
                                SeerState.player2Pets.get(petSeq).setState(1);
                            } else {
                                SeerState.player1Pets.get(petSeq).setState(1);
                            }
                            imgView.setColorFilter(Color.parseColor("#80000000"), PorterDuff.Mode.SRC_ATOP);
                        }
                    }
                    if (state == 1) {
                        SeerState.banCount--;
                        if (SeerState.type.equals("Player1")) {
                            SeerState.player2Pets.get(petSeq).setState(0);
                        } else {
                            SeerState.player1Pets.get(petSeq).setState(0);
                        }
                        imgView.clearColorFilter();
                    }
                }
            }
        }
    }

    public void onFreshButtonClick(View view) {
        Button freshButton = (Button) view;
        initGame();
    }

    public void onReadyButtonClick(View view) {
        Button readyButton = (Button) view;
        if (Objects.equals(SeerState.type, "Player1") && Objects.equals(SeerState.phase, "ReadyStage")) {
            SgcWsHandler.sendMess("start");
        } else if (Objects.equals(SeerState.type, "Player2") && Objects.equals(SeerState.phase, "PreparationStage")) {
            SgcWsHandler.sendMess("ready");
        }
    }

    public void onQuitButtonClick(View view) {
        Button quitButton = (Button) view;
        if (SeerState.phase == null) {
            listener.onDialogEvent("Close");
        }
        if (Objects.equals(SeerState.phase, "match")) {
            SgcWsHandler.sendMess("QuitMatch");
            listener.onDialogEvent("Close");
        } else {
            new AlertDialog.Builder(this.getContext())
                .setTitle("确认操作")
                .setMessage("确定退出对局？")
                .setPositiveButton("确定", (dialog, which) -> {
                    new Thread(() -> {
                        SgcHttpClient sgcHttpClient = new SgcHttpClient();
                        sgcHttpClient.get("/api/game-information/exitGame");
                    }).start();
                    listener.onDialogEvent("Close");
                    SgcWsHandler.closeWs();
                }).setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                }).show();
        }
    }
}
