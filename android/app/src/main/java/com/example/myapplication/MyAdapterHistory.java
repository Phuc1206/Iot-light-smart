package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MyAdapterHistory extends ArrayAdapter<History> {
    Activity context;
    int idLayout;
    ArrayList<History> mylist;

    public MyAdapterHistory(Activity context, int idLayout, ArrayList<History> mylist) {
        super(context, idLayout, mylist);
        this.context = context;
        this.idLayout = idLayout;
        this.mylist = mylist;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater myflagter = context.getLayoutInflater();
        convertView = myflagter.inflate(idLayout,null);
        History myHis = mylist.get(position);
        TextView status = convertView.findViewById(R.id.status);
        TextView time = convertView.findViewById(R.id.his_time);

        status.setText(myHis.getStatus());
        time.setText(myHis.getTime());
        return convertView;
    }
}
