package com.example.thirdhomework;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;


import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.thirdhomework.adapter.RecyclerViewAdapter;
import com.example.thirdhomework.controller.UsageDataController;
import com.example.thirdhomework.entity.AppItem;
import com.example.thirdhomework.entity.UsageData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private UsageDatabase usageDatabase;
    private final String TAG = "databaseTest";
    private static List<UsageData> usageDataList;
    private UsageStatsManager usageStatsManager;
    private UsageDataController usageDataController;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private StaggeredGridLayoutManager layoutManager;
    private static List<AppItem> appItemList = new ArrayList<>();
    private PackageManager packageManager;
    private EditText searchContent;
    private static MainActivity mainActivityInstance;
    Button openUsageSettingButton;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivityInstance=this;
        //开启数据访问许可
        //startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

        //获取数据库实例
        usageDatabase = UsageDatabase.getInstance(this);
        //获取usageStatsManager实例
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        searchContent = findViewById(R.id.search_content);
        ImageView imageView = findViewById(R.id.search_icon);
        imageView.setImageResource(R.drawable.serach);
        Button btn1 = findViewById(R.id.insert1);
        btn1.setOnClickListener(this);
        Button btn2 = findViewById(R.id.queryAll);
        btn2.setOnClickListener(this);
        Button btn3 = findViewById(R.id.start_service);
        btn3.setOnClickListener(this);
        Button btn4 = findViewById(R.id.deleteAll);
        btn4.setOnClickListener(this);
        Button btn5 = findViewById(R.id.queryByName);
        btn5.setOnClickListener(this);
        Button btn6 = findViewById(R.id.deleteByName);
        btn6.setOnClickListener(this);
        Button btn7 = findViewById(R.id.search);
        btn7.setOnClickListener(this);
        Button btn8 = findViewById(R.id.show_events);
        btn8.setOnClickListener(this);
        Button btn9 = findViewById(R.id.collect_one_day_data);
        btn9.setOnClickListener(this);


        openUsageSettingButton = findViewById(R.id.OpenUsageSettingButton);

        usageDataController = new UsageDataController(usageDatabase, usageStatsManager, "weekly");

        //为recyclerView指定adapter
        recyclerView = findViewById(R.id.recycler_view);

        // use a linear layout manager
        layoutManager = new StaggeredGridLayoutManager(
                2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        // specify an adapter (see also next example)
        mAdapter = new RecyclerViewAdapter(appItemList);
        recyclerView.setAdapter(mAdapter);
        initRecyclerListData();
        //开启服务，须在最后开启
        getApplicationContext().startForegroundService(new Intent(getApplicationContext(), MyService.class));
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void initRecyclerListData(){
        new Thread(() -> {
            usageDataList=usageDatabase.usageDao().getAll();
            synchronized (Thread.currentThread()) { //先加锁再等待
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
        initAppItems();
        mAdapter.notifyDataSetChanged();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initAppItems() {
        if(usageDataList==null){
            return;
        }
        appItemList.clear();
        packageManager = getApplicationContext().getPackageManager();
        Stream<UsageData> usageDataStream = usageDataList.stream();
        usageDataList = usageDataStream.sorted(
                (o1, o2) -> Long.compare(o2.getAppLunchCount(), o1.getAppLunchCount()))
                .filter(x -> x.getUsedTime() > 0).collect(Collectors.toList());
        HashSet<String> appNameSet = new HashSet<>();
        for (UsageData usageData : usageDataList) {
            if (!appNameSet.contains(usageData.getAppName())) {
                appNameSet.add(usageData.getAppName());
                AppItem appItem = new AppItem();
                try {
                    appItem.setAppIcon(packageManager.getApplicationIcon(usageData.getAppName()));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                appItem.setAppName(usageData.getAppChineseName());
                appItem.setAppUniqueName(usageData.getAppName());
                appItemList.add(appItem);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.insert1: {
                usageDataController.loadDatabase();
                break;
            }
            case R.id.queryAll: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDataList = usageDatabase.usageDao().getAll();
                        for (UsageData usageData : usageDataList) {
                            Log.e(TAG, usageData.getId() + "," + usageData.getAppName() + "," + usageData.getFirstStartTime() + ","
                                    + usageData.getLastStartTime() + "," + usageData.getUsedTime() + "," + usageData.getAppLunchCount()
                                    + "," + usageData.getAppChineseName());
                        }
                    }
                }).start();
                break;
            }
            case R.id.queryByName: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDataList = usageDatabase.usageDao().findByName("知乎");
                        for (UsageData usageData : usageDataList) {
                            Log.e(TAG, usageData.getId() + "," + usageData.getAppName() + "," + usageData.getFirstStartTime() + ","
                                    + usageData.getLastStartTime() + "," + usageData.getUsedTime() + "," + usageData.getAppLunchCount()
                                    + "," + usageData.getAppChineseName());
                        }
                    }
                }).start();
                break;
            }

            case R.id.deleteAll: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usageDatabase.usageDao().deleteAll();
                    }
                }).start();
                break;
            }
            case R.id.deleteByName: {
                new Thread(() -> {
                    /*long endTime=System.currentTimeMillis()-3600*24*1000*2;
                    usageDataList=usageDatabase.usageDao().getAll();
                    for(UsageData usageData:usageDataList){
                        if(usageData.getLastStartTime()<endTime||usageData.getUsedTime()==0){
                            usageDatabase.usageDao().delete(usageData);
                        }
                    }*/
                    usageDatabase.usageDao().deleteByName("知乎");
                    usageDatabase.usageDao().deleteByName("知乎");
                }).start();
                break;
            }
            case R.id.start_service: {
                getApplicationContext().startForegroundService(new Intent(getApplicationContext(), MyService.class));
                break;
            }
            case R.id.search: {
                Editable s = searchContent.getText();
                new Thread(() -> {
                    usageDataList=usageDatabase.usageDao().findByName(s.toString());
                    synchronized (Thread.currentThread()) { //先加锁再等待
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
                if(usageDataList.size()==0){
                    Toast.makeText(this, "应用不存在，请重新输入", Toast.LENGTH_LONG).show();
                    break;
                }
                initAppItems();
                mAdapter.notifyDataSetChanged();
                break;
            }
            case R.id.show_events: {
                try {
                    usageDataController.dealEvents();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
            case R.id.collect_one_day_data: {
                usageDataController.collectData();
                break;
            }
        }
    }
    public static MainActivity getInstance(){
        return mainActivityInstance;
    }
}