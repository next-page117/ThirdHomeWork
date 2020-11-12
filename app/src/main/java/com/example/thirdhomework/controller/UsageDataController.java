package com.example.thirdhomework.controller;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;

import android.content.pm.ApplicationInfo;

import android.content.pm.PackageManager;
import android.os.Build;

import android.util.Log;


import androidx.annotation.RequiresApi;



import com.example.thirdhomework.MyApplication;
import com.example.thirdhomework.UsageDatabase;
import com.example.thirdhomework.dao.EventDao;
import com.example.thirdhomework.entity.EventData;
import com.example.thirdhomework.entity.UsageData;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class UsageDataController {
    private static final String TAG = UsageDataController.class.getSimpleName();

    private DateFormat dateFormat = new SimpleDateFormat();

    private static DateFormat dateFormat1 = new SimpleDateFormat("HH:mm:ss");

    private UsageDatabase usageDatabase;

    private String interval;

    private static final long ONE_DAY_IN_MILLIS=24*3600*1000;

    private UsageStatsManager usageStatsManager;

    private static List<UsageStats> usageStatsList;

    private List<EventData> eventDataList=new ArrayList<>();

    private static HashMap<String, Integer> intervals = new HashMap<>();

    private List<UsageData> usageDataList;

    private List<UsageEvents.Event> eventList=new ArrayList<>();

    private long endTimeInMillis;

    static {
        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT"));
        intervals.put("daily", 0);
        intervals.put("weekly", 1);
        intervals.put("monthly", 2);
        intervals.put("yearly", 3);
    }

    public UsageDataController(UsageDatabase usageDatabase, UsageStatsManager usageStatsManager, String interval) {
        this.usageDatabase = usageDatabase;
        this.usageStatsManager = usageStatsManager;
        this.interval = interval;
    }

    //获取用户使用数据
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<UsageStats> getUsageStatsList(String interval) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        endTimeInMillis = System.currentTimeMillis();
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(intervals.get(interval),
                cal.getTimeInMillis(), endTimeInMillis);
        return usageStatsList;
    }


    //处理用户事件数据
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dealEvents(){
        new Thread(() -> {
            eventDataList = usageDatabase.eventDao().getAll();
            synchronized (Thread.currentThread()) {
                Thread.currentThread().notify();
            }
        }).start();
        synchronized (Thread.currentThread()) {
            try {
                Thread.currentThread().wait(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long maxTimeStamp=0;
        for(EventData eventData:eventDataList){
            maxTimeStamp=Math.max(maxTimeStamp,eventData.timeStamp);
        }
        //初始化eventList
        eventList.clear();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        long temp = cal.getTimeInMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(cal.getTimeInMillis(), System.currentTimeMillis());
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event nextEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(nextEvent);
            if(nextEvent.getTimeStamp()>maxTimeStamp&&nextEvent.getEventType()==2&&!nextEvent.getPackageName().contains("miui")
                    &&!nextEvent.getPackageName().contains("android")){
                eventList.add(nextEvent);
            }
        }
        //去除前后相同app名称的event
        String appName,preAppName="";
        for(int i=0;i<eventList.size();){
            UsageEvents.Event event=eventList.get(i);
            appName=event.getPackageName();
            if(!preAppName.equals(appName)) {
                preAppName=appName;
                i++;
            }else{
                eventList.remove(event);
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void collectData() {
        //收集statis数据
        usageStatsList = getUsageStatsList("daily");
        new Thread(() -> {
            usageDataList = usageDatabase.usageDao().getAll();
            synchronized (Thread.currentThread()) {
                Thread.currentThread().notify();
            }
        }).start();
        synchronized (Thread.currentThread()) {
            try {
                Thread.currentThread().wait(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //获取当前数据库中的app对应的最大时间
        HashMap<String, Long> usageDataLastTimeMap = new HashMap<>();
        for (UsageData usageData : usageDataList) {
            String appName = usageData.getAppName();
            if (!usageDataLastTimeMap.containsKey(appName)
                    || usageDataLastTimeMap.get(appName) != null
                    && usageData.getLastStartTime() > usageDataLastTimeMap.get(appName)) {
                usageDataLastTimeMap.put(appName, usageData.getLastStartTime());
            }
        }
        //通过最大时间过滤从UsageStats搜集到的数据
        usageStatsList = usageStatsList.stream()    //获取stream流
                .filter((o) ->/*!usageDataLastTimeMap.containsKey(o.getPackageName()) ||*/  //可用户加载新安装的用户数据
                        usageDataLastTimeMap.containsKey(o.getPackageName())
                        && o.getLastTimeUsed()>usageDataLastTimeMap.get(o.getPackageName()))
                .filter((o) -> o.getTotalTimeInForeground() > 0)  //过滤使用时间为0的数据
                .filter((o)->o.getLastTimeStamp()<=endTimeInMillis-ONE_DAY_IN_MILLIS)  //过滤时间间隔距离当前太小的数据
                .sorted(((o1, o2) -> Long.compare(o1.getLastTimeUsed(), o2.getLastTimeUsed())))  //排序
                .collect(Collectors.toList());  //收集
        for (UsageStats usageStats : usageStatsList) {
            Log.e("App name", usageStats.getPackageName());
            Log.e("start time stamp", dateFormat.format(new Date(usageStats.getFirstTimeStamp())));
            Log.e("last time used", dateFormat.format(new Date(usageStats.getLastTimeUsed())));
            Log.e("end time stamp", dateFormat.format(new Date(usageStats.getLastTimeStamp())));
            Log.e("used time", dateFormat1.format(new Date(usageStats.getTotalTimeInForeground())));
        }

        //收集event数据
        dealEvents();
        loadDatabase();
    }

    public String loadDatabase() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                //加载usageStats
                for (UsageStats usageStats : usageStatsList) {
                    UsageData usageData = new UsageData();
                    usageData.setAppName(usageStats.getPackageName());
                    usageData.setLastStartTime(usageStats.getLastTimeUsed());
                    usageData.setUsedTime(usageStats.getTotalTimeInForeground());
                    usageData.setStartTimeStamp(usageStats.getFirstTimeStamp());
                    //获取app中文名
                    PackageManager packageManager = MyApplication.getContext().getPackageManager();
                    ApplicationInfo applicationInfo = null;
                    try {
                        applicationInfo = packageManager.getApplicationInfo(usageStats.getPackageName(), 0);
                        usageData.setAppChineseName(packageManager.getApplicationLabel(applicationInfo).toString());
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    //使用反射获取启动次数
                    Class<?> c = UsageStats.class;
                    Method method = null;
                    try {
                        method = c.getMethod("getAppLaunchCount");
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    try {
                        Integer launchCountInteger = (Integer) method.invoke(usageStats);
                        usageData.setAppLunchCount(launchCountInteger.longValue());
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    usageDatabase.usageDao().insertAll(usageData);
                }

                //加载EventList
                for(UsageEvents.Event event:eventList){
                    EventData eventData=new EventData();
                    eventData.appName=event.getPackageName();
                    eventData.timeStamp=event.getTimeStamp();
                    //获取app中文名
                    PackageManager packageManager = MyApplication.getContext().getPackageManager();
                    ApplicationInfo applicationInfo = null;
                    try {
                        applicationInfo = packageManager.getApplicationInfo(event.getPackageName(), 0);
                        eventData.setAppChineseName(packageManager.getApplicationLabel(applicationInfo).toString());
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    usageDatabase.eventDao().insertAll(eventData);
                }
            }
        }).start();
        return null;
    }
}
