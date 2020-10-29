package com.example.thirdhomework;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.thirdhomework.controller.UsageDataController;
import com.example.thirdhomework.entity.UsageData;

import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private UsageDatabase usageDatabase;
    private final String TAG="databaseTest";
    private List<UsageData> usageDataList;
    private UsageStatsManager usageStatsManager;
    private UsageDataController usageDataController;

    Button openUsageSettingButton;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //开启服务
        startService(new Intent(this,MyService.class));
        usageDatabase=UsageDatabase.getInstance(this);
        usageStatsManager=(UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
        Button btn1=findViewById(R.id.insert1);
        btn1.setOnClickListener(this);
        Button btn2=findViewById(R.id.query1);
        btn2.setOnClickListener(this);
        Button btn3=findViewById(R.id.display1);
        btn3.setOnClickListener(this);
        Button btn4=findViewById(R.id.deleteAll);
        btn4.setOnClickListener(this);

        openUsageSettingButton=findViewById(R.id.OpenUsageSettingButton);
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
                    //usageDataController.dataDeal();
                    usageDataController.dealEvents();
                }
                break;
            }
        }
    }
}