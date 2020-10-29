package com.example.thirdhomework.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thirdhomework.R;
import com.example.thirdhomework.entity.AppItem;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    private List<AppItem> appItemList;

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView appIcon;
        TextView appName;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon=itemView.findViewById(R.id.app_icon);
            appName=itemView.findViewById(R.id.app_name);
        }
    }
    public RecyclerViewAdapter(List<AppItem> appItemList){
        this.appItemList=appItemList;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.usage_recycler_item,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.MyViewHolder holder, int position) {
        AppItem appItem=appItemList.get(position);
        holder.appName.setText(appItem.getAppName());
        holder.appIcon.setImageDrawable(appItem.getAppIcon());
    }

    @Override
    public int getItemCount() {
        return appItemList.size();
    }
}
