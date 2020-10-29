package com.example.thirdhomework;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.thirdhomework.adapter.RecyclerViewAdapter;
import com.example.thirdhomework.controller.UsageDataController;
import com.example.thirdhomework.entity.AppItem;
import com.example.thirdhomework.entity.UsageData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private UsageDatabase usageDatabase;
    private final String TAG="databaseTest";
    private static List<UsageData> usageDataList;
    private UsageStatsManager usageStatsManager;
    private UsageDataController usageDataController;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private static List<AppItem> appItemList=new ArrayList<>();

    Button openUsageSettingButton;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

        //开启服务
        startService(new Intent(this,MyService.class));
        //获取数据库实例
        usageDatabase=UsageDatabase.getInstance(this);
        //获取usageStatsManager实例
        usageStatsManager=(UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
        Button btn1=findViewById(R.id.insert1);
        btn1.setOnClickListener(this);
        Button btn2=findViewById(R.id.query1);
        btn2.setOnClickListener(this);
        Button btn3=findViewById(R.id.display1);
        btn3.setOnClickListener(this);
        Button btn4=findViewById(R.id.deleteAll);
        btn4.setOnClickListener(this);
        Button btn5=findViewById(R.id.queryByName);
        btn5.setOnClickListener(this);
        openUsageSettingButton=findViewById(R.id.OpenUsageSettingButton);


        initAppItems();
        synchronized (Thread.currentThread()) { //先加锁再等待
            try {
                Thread.currentThread().wait(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //为recyclerView指定adapter
        recyclerView=findViewById(R.id.recycler_view);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an adapter (see also next example)
        mAdapter = new RecyclerViewAdapter(appItemList);
        recyclerView.setAdapter(mAdapter);

    }
    private void initAppItems() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                usageDataList=usageDatabase.usageDao().getAll();
                HashSet<String> appNameSet=new HashSet<>();
                for(UsageData usageData:usageDataList){
                    if(!appNameSet.contains(usageData.getAppName())){
                        appNameSet.add(usageData.getAppName());
                        AppItem appItem=new AppItem();
                        try {
                            appItem.setAppIcon(getApplicationContext().getPackageManager()
                                    .getApplicationIcon(usageData.getAppName()));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        appItem.setAppName(usageData.getAppName());
                        appItemList.add(appItem);
                    }
                }
                synchronized (Thread.currentThread()) {
                    Thread.currentThread().notify();//主线程唤起
                }
            }
        }).start();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.insert1:{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UsageData usageData1 =new UsageData(1,"app1",0,10,10);
                        UsageData usageData2 =new UsageData(2,"app2",0,10,10);
                        UsageData usageData3 =new UsageData(3,"app3",0,10,10);
                        usageDatabase.usageDao().insertAll(usageData1, usageData2, usageData3);
                    }
                }).start();
                break;
            }
            case R.id.query1:{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDataList =usageDatabase.usageDao().getAll();
                        for (UsageData usageData : usageDataList) {
                            Log.e(TAG, usageData.getId() + "," + usageData.getAppName() + "," + usageData.getFirstStartTime() + ","
                                    + usageData.getLastStartTime() + "," + usageData.getUsedTime());
                        }
                    }
                }).start();
                break;
            }
            case R.id.queryByName:{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDataList =usageDatabase.usageDao().findByName("com.example.thirdhomework");
                        for (UsageData usageData : usageDataList) {
                            Log.e(TAG, usageData.getId() + "," + usageData.getAppName() + "," + usageData.getFirstStartTime() + ","
                                    + usageData.getLastStartTime() + "," + usageData.getUsedTime());
                        }
                    }
                }).start();
                break;
            }

            case R.id.deleteAll:{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDatabase.usageDao().deleteAll();
                    }
                }).start();
                break;
            }
            case R.id.display1:{
                usageDataController=new UsageDataController(usageDatabase,usageStatsManager,"monthly");
                if(usageDataController.getUsageStatsList("daily").size()==0){
                    Toast.makeText(this,"请打开数据访问权限",Toast.LENGTH_LONG);
                    openUsageSettingButton.setVisibility(View.VISIBLE);
                    openUsageSettingButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    });
                }else{
                    usageDataController.dataDeal();
                }
                break;
            }
        }
    }
}