package com.example.sgcdemo2.entity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.sgcdemo2.R;
import com.example.sgcdemo2.entity.RaceGroupVO;

import java.util.List;

public class RaceGroupVOAdapter extends BaseAdapter {
    private Context context;
    private List<RaceGroupVO> dataList;

    public RaceGroupVOAdapter(Context context, List<RaceGroupVO> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int i) {
        return dataList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        RaceGroupVO group = dataList.get(position);
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(group.getGroupName());

        return convertView;
    }
}
