package com.example.thirdhomework;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.ColumnInfo;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.example.thirdhomework.entity.UsageData;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class SecondActivity extends AppCompatActivity {
    private String appName;
    private String appUniqueName;
    private UsageDatabase usageDatabase;
    private static List<UsageData> usageDataList;

    private TextView app_Name;
    private TextView first_start_time;
    private TextView last_start_time;
    private TextView used_total_time;
    private TextView app_launch_count;
    private TextView used_total_time_of_day;

    private CollapsingToolbarLayout collapsingToolbarLayout;

    private DateFormat dateFormat=new SimpleDateFormat();
    private DateFormat dateFormat1=new SimpleDateFormat("HH时mm分ss秒");
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent intent=getIntent();
        appName=intent.getStringExtra("appName");
        appUniqueName=intent.getStringExtra("appUniqueName");

        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(appName);

        usageDatabase=UsageDatabase.getInstance(getApplicationContext());
        usageDataList=new ArrayList<>();

//        app_Name=findViewById(R.id.app_name_2);
        first_start_time=findViewById(R.id.first_start_time_2);
        last_start_time=findViewById(R.id.last_start_time_2);
        used_total_time=findViewById(R.id.used_total_time_2);
        app_launch_count=findViewById(R.id.app_launch_count_2);
        used_total_time_of_day=findViewById(R.id.used_total_time_of_day_2);

        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT"));

        displayData();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void displayData(){
        new Thread(()->{
            usageDataList=usageDatabase.usageDao().findByUniqueName(appUniqueName);
            synchronized (Thread.currentThread()) { //唤醒主线程
                Thread.currentThread().notify();
            }
        }).start();
        synchronized (Thread.currentThread()) { //先加锁再等待
            try {
                Thread.currentThread().wait(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        usageDataList=usageDataList.stream().filter((o)-> o.lastStartTime>0)
                .sorted(((o1, o2) -> Long.compare(o1.lastStartTime,o2.lastStartTime)))
                .collect(Collectors.toList());
        long totalTime=0;
        long appLaunchCount=0;
        long usedTotalTimeOfDay=0;

        for(UsageData usageData:usageDataList){
            totalTime+=usageData.usedTime;
            appLaunchCount+=usageData.appLunchCount;
            usedTotalTimeOfDay=Math.max(usedTotalTimeOfDay,usageData.getUsedTime());
        }
//        app_Name.setText(appName);
        first_start_time.setText(dateFormat.format(new Date(usageDataList.get(0).getLastStartTime())));
        last_start_time.setText(dateFormat.format(
                new Date(usageDataList.get(usageDataList.size()-1).getLastStartTime())));
        used_total_time.setText(dateFormat1.format(new Date(totalTime)));
        app_launch_count.setText(appLaunchCount+"次");
        used_total_time_of_day.setText(dateFormat1.format(new Date(usedTotalTimeOfDay)));
    }
}