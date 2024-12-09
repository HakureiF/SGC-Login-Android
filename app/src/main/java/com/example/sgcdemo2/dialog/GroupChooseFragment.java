package com.example.sgcdemo2.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.sgcdemo2.MainActivity;
import com.example.sgcdemo2.R;
import com.example.sgcdemo2.entity.BagPetVO;
import com.example.sgcdemo2.entity.RaceGroupVO;
import com.example.sgcdemo2.entity.adapter.RaceGroupVOAdapter;
import com.example.sgcdemo2.func.OnDialogListener;
import com.example.sgcdemo2.net.SgcHttpClient;
import com.example.sgcdemo2.net.SgcWsHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class GroupChooseFragment extends DialogFragment {
    OnDialogListener listener;
    ListView groupsView;

    private View groupChooseView;
    private RaceGroupVOAdapter adapter;
    private List<RaceGroupVO> groupList;

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
        View view = inflater.inflate(R.layout.groups_layout, null);
        groupChooseView = view;
        initList();

        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (MainActivity.modMark.equals("Create")) {
            System.out.println("停止创建对局");
            SgcWsHandler.closeWs();
        }
    }

    private void initList() {
        groupsView = groupChooseView.findViewById(R.id.groups_list);

        new Thread(() -> {
            SgcHttpClient sgcHttpClient = new SgcHttpClient();
            Map<String, Object> res = sgcHttpClient.get("/api/race-group/searchGroup?group=match");
            Gson gson = new Gson();
            Type type = new TypeToken<List<RaceGroupVO>>(){}.getType();
            groupList = gson.fromJson(gson.toJson(res.get("data")), type);
            System.out.println(groupList.toString());
            adapter = new RaceGroupVOAdapter(groupsView.getContext(), groupList);
            groupChooseView.post(() -> {
                groupsView.setAdapter(adapter);
                groupsView.setOnItemClickListener((parent, view, position, id) -> {
                    RaceGroupVO group = groupList.get(position);
                    listener.onDialogEvent("Group" + group.getGroupId());
                });
            });
        }).start();
    }
}
