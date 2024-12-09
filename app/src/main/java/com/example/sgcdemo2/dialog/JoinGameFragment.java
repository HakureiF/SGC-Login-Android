package com.example.sgcdemo2.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.sgcdemo2.MainActivity;
import com.example.sgcdemo2.R;
import com.example.sgcdemo2.func.OnDialogListener;
import com.example.sgcdemo2.net.SgcWsHandler;

public class JoinGameFragment extends DialogFragment {
    OnDialogListener listener;
    private View joinGameView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MyDialogListener");
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
        View view = inflater.inflate(R.layout.joingame_layout, null);
        joinGameView = view;

        ViewGroup viewGroup = joinGameView.findViewById(R.id.join_game);

        Button button = joinGameView.findViewById(R.id.join_button);
        button.setOnClickListener(this::onJoinButtonClick);

        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (MainActivity.modMark.equals("Join")) {
            System.out.println("停止加入");
            SgcWsHandler.closeWs();
        }
    }

    public void onJoinButtonClick(View view) {
        System.out.println("join game");
        EditText editText = joinGameView.findViewById(R.id.game_id_edittext);
        listener.onDialogEvent("JoinGame" + editText.getText());
    }
}
