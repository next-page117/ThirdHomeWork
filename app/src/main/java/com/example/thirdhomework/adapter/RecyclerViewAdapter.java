package com.example.thirdhomework.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thirdhomework.MainActivity;
import com.example.thirdhomework.MyApplication;
import com.example.thirdhomework.R;
import com.example.thirdhomework.SecondActivity;
import com.example.thirdhomework.entity.AppItem;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    private List<AppItem> appItemList;

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView appIcon;
        TextView appName;
        RecyclerView recyclerView;
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
        //处理点击事件
        final MyViewHolder viewHolder=new MyViewHolder(view);
        viewHolder.appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position=viewHolder.getAdapterPosition();
                AppItem appItem=appItemList.get(position);
                Intent intent =new Intent(MyApplication.getContext(),SecondActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("appName",appItem.appName);
                intent.putExtra("appUniqueName",appItem.getAppUniqueName());
                MyApplication.getContext().startActivity(intent);
            }
        });

        return viewHolder;
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
