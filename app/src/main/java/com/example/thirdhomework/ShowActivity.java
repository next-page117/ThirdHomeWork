package com.example.thirdhomework;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.thirdhomework.controller.UsageDataController;
import com.example.thirdhomework.entity.AppItem;
import com.example.thirdhomework.entity.UsageData;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class ShowActivity extends AppCompatActivity {
    private String appName;
    private int flag;

    private ImageView imageView;


    private CollapsingToolbarLayout collapsingToolbarLayout;
    //数据库接收数据
    private List<UsageData> usageDataList=new ArrayList<>();
    //数据库数据按名称和UsageData存储
    private Map<String,List<UsageData>> usageDataMap=new HashMap<>();
    //最长使用时间应用列表，可显示多个
    private List<UsageData> mostTotalTimeAppList=new ArrayList<>();
    //最长使用时间应用列表的个数,默认1
    private int mostTotalTimeAppCount=1;
    //最多登录次数的应用列表，可显示多个
    private List<UsageData> mostLaunchCountAppList=new ArrayList<>();
    //最多登录次数的应用个数，默认1
    private int mostLaunchCountAppCount=1;
    //数据库操作实例
    private UsageDatabase usageDatabase;
    //时间格式化,显示日月年
    DateFormat dateFormat=new SimpleDateFormat();
    //时间格式化,显示时分
    DateFormat dateFormat1=new SimpleDateFormat("HH小时mm分");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        Intent intent=getIntent();
        appName=intent.getStringExtra("appName");
        flag=intent.getIntExtra("flag",0);

        collapsingToolbarLayout=findViewById(R.id.collapsing_toolbar);
        imageView = findViewById(R.id.show_image_view);

        collapsingToolbarLayout.setTitle(appName);

        //获取数据库操作实例
        usageDatabase=UsageDatabase.getInstance(getApplicationContext());
        //初始化时间格式化时区
        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT"));
        initUsageDataMap();

        switch (flag){
            case 0:
                imageView.setImageResource(R.drawable.frequent_app);
                initMostTotalTimeAppList();
                break;
            case 1:
                imageView.setImageResource(R.drawable.long_app);
                initMostLaunchCountAppList();
                break;
        }
    }
    //初始化usageDataMap
    public void initUsageDataMap() {
        //操作数据库获得usageDataList
        new Thread(() -> {
            usageDataList = usageDatabase.usageDao().getAll();
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
        //初始化usageDataMap,相乘用户名和其usageDataList的映射
        for(UsageData usageData:usageDataList){
            String appChineseName=usageData.getAppChineseName();
            if(!usageDataMap.containsKey(appChineseName)){
                List<UsageData> usageDataListInMap=new ArrayList<>();
                usageDataListInMap.add(usageData);
                usageDataMap.put(appChineseName,usageDataListInMap);
            }else{
                usageDataMap.get(appChineseName).add(usageData);
            }
        }
    }

    //得到用户最长使用时间应用列表,存放在mostTotalTimeAppList中
    public void initMostTotalTimeAppList(){
        int m=usageDataMap.size();
        Map<String,Long> usedTotalTimeMap=new HashMap<>();
        for(Map.Entry<String,List<UsageData>> entry:usageDataMap.entrySet()){
            long usedTotalTime=0;
            for(UsageData usageData:entry.getValue()){
                usedTotalTime+=usageData.usedTime;
            }
            usedTotalTimeMap.put(entry.getKey(),usedTotalTime);
        }
        //从中选取使用时长最大的几个元素
        mostTotalTimeAppCount=3;
        //忽略查找的app名称，即已经找出的最大值set
        Set<String> ignoreApp=new HashSet<>();
        while(mostTotalTimeAppCount-->0){
            long maxTotalTime=Long.MIN_VALUE;
            String maxTotalTimeAppName="";
            for(Map.Entry<String,Long> entry:usedTotalTimeMap.entrySet()){
                if(!ignoreApp.contains(entry.getKey())) {
                    if(entry.getValue()>maxTotalTime){
                        maxTotalTime=entry.getValue();
                        maxTotalTimeAppName=entry.getKey();
                    }
                }
            }
            ignoreApp.add(maxTotalTimeAppName);
            UsageData usageData=new UsageData();
            usageData.setAppChineseName(maxTotalTimeAppName);
            usageData.setUsedTime(maxTotalTime);
            mostTotalTimeAppList.add(usageData);
        }
        //测试数据
        for(UsageData usageData:mostTotalTimeAppList){
            System.out.println(dateFormat1.format(new Date(usageData.getUsedTime())));
        }
    }

    //得到用户最多使用时间次数应用列表,存放在mostLaunchCountAppList中
    public void initMostLaunchCountAppList(){
        int m=usageDataMap.size();
        Map<String,Long> launchCountMap=new HashMap<>();
        for(Map.Entry<String,List<UsageData>> entry:usageDataMap.entrySet()){
            long launchCount=0;
            for(UsageData usageData:entry.getValue()){
                launchCount+=usageData.appLunchCount;
            }
            launchCountMap.put(entry.getKey(),launchCount);
        }
        //从中选取使用时长最大的几个元素
        mostLaunchCountAppCount=1;
        //忽略查找的app名称，即已经找出的最大值set
        Set<String> ignoreApp=new HashSet<>();
        while(mostLaunchCountAppCount-->0){
            long maxLaunchCount=Long.MIN_VALUE;
            String maxLaunchCountAppName="";
            for(Map.Entry<String,Long> entry:launchCountMap.entrySet()){
                if(!ignoreApp.contains(entry.getKey())) {
                    if(entry.getValue()>maxLaunchCount){
                        maxLaunchCount=entry.getValue();
                        maxLaunchCountAppName=entry.getKey();
                    }
                }
            }
            ignoreApp.add(maxLaunchCountAppName);
            UsageData usageData=new UsageData();
            usageData.setAppChineseName(maxLaunchCountAppName);
            usageData.setAppLunchCount(maxLaunchCount);
            mostLaunchCountAppList.add(usageData);
        }
    }
}