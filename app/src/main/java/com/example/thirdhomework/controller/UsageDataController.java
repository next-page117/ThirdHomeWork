package com.example.thirdhomework.controller;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;

import android.os.Build;

import android.util.Log;


import androidx.annotation.RequiresApi;
import androidx.room.Update;

import com.example.thirdhomework.UsageDatabase;
import com.example.thirdhomework.entity.UsageData;

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
import java.util.stream.IntStream;

public class UsageDataController {
    private static final String TAG=UsageDataController.class.getSimpleName();

    private DateFormat dateFormat=new SimpleDateFormat();

    private DateFormat dateFormat1=new SimpleDateFormat("HH:mm:ss");

    private UsageDatabase usageDatabase;

    private String interval;

    private UsageStatsManager usageStatsManager;

    private List<UsageStats> usageStatsList;

    private HashMap<String,List<UsageData>> usageDataMap=new HashMap<>();

    private static HashMap<String,Integer> intervals=new HashMap<>();


    static {
        intervals.put("daily",0);
        intervals.put("weekly",1);
        intervals.put("monthly",2);
        intervals.put("yearly",3);
    }

    public UsageDataController(UsageDatabase usageDatabase,UsageStatsManager usageStatsManager,String interval) {
        this.usageDatabase = usageDatabase;
        this.usageStatsManager=usageStatsManager;
        this.interval=interval;
    }
    //获取用户使用数据
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<UsageStats> getUsageStatsList(String interval){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        List<UsageStats> usageStatsList=usageStatsManager.queryUsageStats(intervals.get(interval),
                cal.getTimeInMillis(),System.currentTimeMillis());
        return usageStatsList;
    }

    /**
     * 测试获取用户事件数据
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public UsageEvents getUsageEvents(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        UsageEvents usageEvents=usageStatsManager.queryEventsForSelf(cal.getTimeInMillis(),System.currentTimeMillis());
        return usageEvents;
    }

    //测试用户事件数据

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void dealEvents(){
        UsageEvents usageEvents=getUsageEvents();
        UsageEvents.Event nextEvent=new UsageEvents.Event();
        while(usageEvents.hasNextEvent()){
            usageEvents.getNextEvent(nextEvent);
            if(nextEvent==null){
                return;
            }
            Log.e("appName",nextEvent.getPackageName());
            Log.e("event type", nextEvent.getEventType()+"  ");
            Log.e("time stamp",dateFormat.format(new Date(nextEvent.getTimeStamp())));
        }
    }*/

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void dataDeal(){
        dateFormat1.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));

        List<UsageStats> usageStatsListOfDaily=getUsageStatsList("daily");
        List<UsageStats> usageStatsList=getUsageStatsList("weekly");
        Collections.sort(usageStatsListOfDaily, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats o1, UsageStats o2) {
                return Long.compare(o1.getLastTimeUsed(),o2.getLastTimeUsed());
            }
        });
        Collections.sort(usageStatsList, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats o1, UsageStats o2) {
                return Long.compare(o1.getLastTimeUsed(),o2.getLastTimeUsed());
            }
        });


        HashMap<String,List<UsageStats>> usageStatsMapOfDaily=new HashMap<>();
        HashMap<String,List<UsageStats>> usageStatsMap=new HashMap<>();

        Log.e("dailySize",Integer.toString(usageStatsListOfDaily.size()));

        Log.e("statsSize",Integer.toString(usageStatsList.size()));
        for(UsageStats usageStatsOfDaily:usageStatsListOfDaily){
            if(usageStatsMapOfDaily.containsKey(usageStatsOfDaily.getPackageName())){
                Objects.requireNonNull(usageStatsMapOfDaily.get(usageStatsOfDaily.getPackageName())).add(usageStatsOfDaily);
            }else{
                List<UsageStats> list=new ArrayList<>();
                list.add(usageStatsOfDaily);
                usageStatsMapOfDaily.put(usageStatsOfDaily.getPackageName(),list);
            }
        }
        for(UsageStats usageStats:usageStatsList){
            if(usageStatsMap.containsKey(usageStats.getPackageName())){
                Objects.requireNonNull(usageStatsMap.get(usageStats.getPackageName())).add(usageStats);
            }else{
                List<UsageStats> list=new ArrayList<>();
                list.add(usageStats);
                usageStatsMap.put(usageStats.getPackageName(),list);
            }
        }

        //模拟获取第一次启动时间和其他数据

        for(Map.Entry<String,List<UsageStats>> entry:usageStatsMapOfDaily.entrySet()){
            for(int i=0,j=0;i<entry.getValue().size();j++){
                String appName=entry.getKey();
                if(j==usageStatsMap.get(appName).size()){
                    break;
                }
                List<UsageStats> usageStatsList1=usageStatsMap.get(appName);
                UsageStats usageStats= Objects.requireNonNull(usageStatsMap.get(appName))
                        .get(j);
                long curFirstTimeStamp= usageStats.getFirstTimeStamp();
                long curLastTimeUsed=entry.getValue().get(i).getLastTimeUsed();

                String curFirstTime=dateFormat.format(curFirstTimeStamp);  //测试用
                String curLastTime=dateFormat.format(curLastTimeUsed);    //测试用转化为公元时间
                while(curLastTimeUsed<=curFirstTimeStamp){
                    i++;
                    if(i==entry.getValue().size()){
                        break;
                    }
                    curLastTimeUsed=entry.getValue().get(i).getLastTimeUsed();
                    curLastTime=dateFormat.format(curLastTimeUsed);
                }
                Long lastTimeUsed=usageStats.getLastTimeUsed();
                String lastTimeUsedG=dateFormat.format(lastTimeUsed);
                UsageData usageData=new UsageData();
                if(curLastTimeUsed<=curFirstTimeStamp+86_400_000*7&&curLastTimeUsed<=lastTimeUsed){
                    usageData.setFirstStartTime(curLastTimeUsed);
                    i++;
                }else{
                    usageData.setFirstStartTime(lastTimeUsed);
                }
                usageData.setAppName(appName);
                usageData.setLastStartTime(lastTimeUsed);
                usageData.setUsedTime(usageStats.getTotalTimeInForeground());
                usageData.setStartTimeStamp(usageStats.getFirstTimeStamp());
                if(usageDataMap.containsKey(appName)){
                    Objects.requireNonNull(usageDataMap.get(appName)).add(usageData);
                }else{
                    List<UsageData> list=new ArrayList<>();
                    list.add(usageData);
                    usageDataMap.put(appName,list);
                }
            }
        }
        for(Map.Entry<String,List<UsageData>> entry:usageDataMap.entrySet()){
            Log.e("appName",entry.getKey());
            for(UsageData usageData:entry.getValue()){
                Log.e("fistTime",dateFormat.format(new Date(usageData.getFirstStartTime())));
                Log.e("lastTime",dateFormat.format(new Date(usageData.getLastStartTime())));
                Log.e("lastTime",dateFormat1.format(usageData.getUsedTime()));
            }
        }
        //loadDatabase();
    }

    public String loadDatabase(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                usageStatsList=getUsageStatsList("daily");
                int i=1;
                for(UsageStats usageStats:usageStatsList){
                    UsageData usageData=new UsageData();
                    usageData.setId(i++);
                    usageData.setAppName(usageStats.getPackageName());
                    usageData.setLastStartTime(usageStats.getLastTimeUsed());
                    usageData.setUsedTime(usageStats.getTotalTimeInForeground());
                    usageData.setStartTimeStamp(usageStats.getFirstTimeStamp());
                    usageDatabase.usageDao().insertAll(usageData);
                }
            }
        }).start();
        return null;
    }

}
