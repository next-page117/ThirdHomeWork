package com.example.thirdhomework;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.thirdhomework.adapter.RecyclerViewAdapter;
import com.example.thirdhomework.controller.UsageDataController;
import com.example.thirdhomework.entity.AppItem;
import com.example.thirdhomework.entity.UsageData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyService extends Service {
    private UsageDataController usageDataController;
    private UsageDatabase usageDatabase;
    private UsageStatsManager usageStatsManager;
    private List<UsageData> usageDataList;
    Button openUsageSettingButton;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public MyService() {

    }
    //onCreate是在创建服务时才调用

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        usageStatsManager = (UsageStatsManager) getApplicationContext()
                .getSystemService(Context.USAGE_STATS_SERVICE);
        usageDatabase = UsageDatabase.getInstance(getApplicationContext());
        super.onCreate();
        //实现前台服务
        String ID = "Notification_ID";
        String NAME = "Notification_NAME";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);
        Notification notification = new Notification.Builder(this, ID)
                .setContentTitle("用户数据处理系统")
                .setContentText("正在处理用户数据")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        startForeground(1, notification);
    }

    //在每次服务启动的时候调用

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        usageDataController = new UsageDataController(usageDatabase, usageStatsManager, "monthly");
        if (usageDataController.getUsageStatsList("daily").size() == 0) {

            return 0;
        }else{
            //收集数据并加载到sqlite中
            usageDataController.collectData();
            //使用反射调用MainActivity中的Recycler初始化方法
            //刷新RecyclerView
            Class<?> mainActivityClass= MainActivity.class;
            Method initRecyclerListDataMethod;
            try {
                initRecyclerListDataMethod=mainActivityClass.getMethod("initRecyclerListData");
                initRecyclerListDataMethod.invoke(MainActivity.getInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void loadDatabase() {
        usageDataController.loadDatabase();
    }
}
