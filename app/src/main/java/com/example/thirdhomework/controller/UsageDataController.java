package com.example.thirdhomework.controller;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import android.util.Log;


import androidx.annotation.RequiresApi;
import androidx.room.Update;

import com.example.thirdhomework.MainActivity;
import com.example.thirdhomework.MyApplication;
import com.example.thirdhomework.UsageDatabase;
import com.example.thirdhomework.entity.UsageData;

import java.lang.reflect.Field;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UsageDataController {
    private static final String TAG = UsageDataController.class.getSimpleName();

    private DateFormat dateFormat = new SimpleDateFormat();

    private static DateFormat dateFormat1 = new SimpleDateFormat("HH:mm:ss");

    private UsageDatabase usageDatabase;

    private String interval;

    private UsageStatsManager usageStatsManager;

    private static List<UsageStats> usageStatsList;

    private HashMap<String, List<UsageData>> usageDataMap = new HashMap<>();

    private static HashMap<String, Integer> intervals = new HashMap<>();

    private List<UsageData> usageDataList;

    private long endTimeInMillis;

    private final long ONE_DAY_IN_MILLIS = 86_400_000;


    static {
        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT"));
        intervals.put("daily", 0);
        intervals.put("weekly", 1);
        intervals.put("monthly", 2);
        intervals.put("yearly", 3);
    }

    public UsageDataController() {
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

    //测试获取用户事件数据
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public UsageEvents getUsageEvents() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        long temp = cal.getTimeInMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(cal.getTimeInMillis(), System.currentTimeMillis());
        return usageEvents;
    }

    //测试用户事件数据

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void dealEvents() {
        UsageEvents usageEvents = getUsageEvents();
        HashMap<String, List<UsageEvents.Event>> eventHashMap = new HashMap<>();
        int type1Count = 0;
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event nextEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(nextEvent);
            String appName = nextEvent.getPackageName();
            int eventType = nextEvent.getEventType();
            if (eventType == 1 || eventType == 23 || eventType == 2) {
                if (eventType == 1) {
                    type1Count++;
                }
                if (eventHashMap.containsKey(appName)) {
                    eventHashMap.get(appName).add(nextEvent);
                } else {
                    List<UsageEvents.Event> eventList = new ArrayList<>();
                    eventList.add(nextEvent);
                    eventHashMap.put(appName, eventList);
                }
            }
        }
        Log.e("eventType1 count", type1Count + "");
        for (Map.Entry<String, List<UsageEvents.Event>> entry : eventHashMap.entrySet()) {
            Log.e("app name", entry.getKey());
            for (UsageEvents.Event event : entry.getValue()) {
                Log.e("event type", event.getEventType() + "");
                Log.e("time stamp", dateFormat.format(new Date(event.getTimeStamp())));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void dataDeal() {
        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        List<UsageStats> usageStatsListOfDaily = getUsageStatsList("daily");
        List<UsageStats> usageStatsList = getUsageStatsList("yearly");
        Collections.sort(usageStatsListOfDaily, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats o1, UsageStats o2) {
                return Long.compare(o1.getLastTimeUsed(), o2.getLastTimeUsed());
            }
        });
        Collections.sort(usageStatsList, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats o1, UsageStats o2) {
                return Long.compare(o1.getLastTimeUsed(), o2.getLastTimeUsed());
            }
        });


        HashMap<String, List<UsageStats>> usageStatsMapOfDaily = new HashMap<>();
        HashMap<String, List<UsageStats>> usageStatsMap = new HashMap<>();

        Log.e("dailySize", Integer.toString(usageStatsListOfDaily.size()));

        Log.e("statsSize", Integer.toString(usageStatsList.size()));
        for (UsageStats usageStatsOfDaily : usageStatsListOfDaily) {
            if (usageStatsMapOfDaily.containsKey(usageStatsOfDaily.getPackageName())) {
                Objects.requireNonNull(usageStatsMapOfDaily.get(usageStatsOfDaily.getPackageName())).add(usageStatsOfDaily);
            } else {
                List<UsageStats> list = new ArrayList<>();
                list.add(usageStatsOfDaily);
                usageStatsMapOfDaily.put(usageStatsOfDaily.getPackageName(), list);
            }
        }
        for (UsageStats usageStats : usageStatsList) {
            if (usageStatsMap.containsKey(usageStats.getPackageName())) {
                Objects.requireNonNull(usageStatsMap.get(usageStats.getPackageName())).add(usageStats);
            } else {
                List<UsageStats> list = new ArrayList<>();
                list.add(usageStats);
                usageStatsMap.put(usageStats.getPackageName(), list);
            }
        }

        //模拟获取第一次启动时间和其他数据

        for (Map.Entry<String, List<UsageStats>> entry : usageStatsMapOfDaily.entrySet()) {
            for (int i = 0, j = 0; i < entry.getValue().size(); j++) {
                String appName = entry.getKey();
                if (j == usageStatsMap.get(appName).size()) {
                    break;
                }
                List<UsageStats> usageStatsList1 = usageStatsMap.get(appName);
                UsageStats usageStats = Objects.requireNonNull(usageStatsMap.get(appName))
                        .get(j);
                long curFirstTimeStamp = usageStats.getFirstTimeStamp();
                long curLastTimeUsed = entry.getValue().get(i).getLastTimeUsed();

                String curFirstTime = dateFormat.format(curFirstTimeStamp);  //测试用
                String curLastTime = dateFormat.format(curLastTimeUsed);    //测试用转化为公元时间
                while (curLastTimeUsed <= curFirstTimeStamp) {
                    i++;
                    if (i == entry.getValue().size()) {
                        break;
                    }
                    curLastTimeUsed = entry.getValue().get(i).getLastTimeUsed();
                    curLastTime = dateFormat.format(curLastTimeUsed);
                }
                Long lastTimeUsed = usageStats.getLastTimeUsed();
                String lastTimeUsedG = dateFormat.format(lastTimeUsed);
                UsageData usageData = new UsageData();
                if (curLastTimeUsed <= curFirstTimeStamp + 86_400_000 * 7 && curLastTimeUsed <= lastTimeUsed) {
                    usageData.setFirstStartTime(curLastTimeUsed);
                    i++;
                } else {
                    usageData.setFirstStartTime(lastTimeUsed);
                }
                usageData.setAppName(appName);
                usageData.setLastStartTime(lastTimeUsed);
                usageData.setUsedTime(usageStats.getTotalTimeInForeground());
                usageData.setStartTimeStamp(usageStats.getFirstTimeStamp());
                if (usageDataMap.containsKey(appName)) {
                    Objects.requireNonNull(usageDataMap.get(appName)).add(usageData);
                } else {
                    List<UsageData> list = new ArrayList<>();
                    list.add(usageData);
                    usageDataMap.put(appName, list);
                }
            }
        }
        for (Map.Entry<String, List<UsageData>> entry : usageDataMap.entrySet()) {
            Log.e("appName", entry.getKey());
            for (UsageData usageData : entry.getValue()) {
                Log.e("fistTime", dateFormat.format(new Date(usageData.getFirstStartTime())));
                Log.e("lastTime", dateFormat.format(new Date(usageData.getLastStartTime())));
                Log.e("lastTime", dateFormat1.format(usageData.getUsedTime()));
            }
        }
        //loadDatabase();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void collectData() {
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
                .filter((o) -> !usageDataLastTimeMap.containsKey(o.getPackageName())
                        || usageDataLastTimeMap.containsKey(o.getPackageName())
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
        loadDatabase();
    }

    public String loadDatabase() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
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
            }
        }).start();
        return null;
    }
}
