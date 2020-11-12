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

    private UsageStatsManager usageStatsManager;

    private static List<UsageStats> usageStatsList;

    private HashMap<String, List<UsageData>> usageDataMap = new HashMap<>();

    private static HashMap<String, Integer> intervals = new HashMap<>();

    private List<UsageData> usageDataList;

    private List<UsageEvents.Event> eventList=new ArrayList<>();

    private long endTimeInMillis;

    private final long ONE_DAY_IN_MILLIS = 86_400_000;

    private HashMap<String,Integer> markovStateMap=new HashMap<>();

    private HashMap<Integer,String> reverseMarkovStateMap=new HashMap<>();

    private List<UsageEvents.Event> trainEventList=new ArrayList<>();

    private List<UsageEvents.Event> testEventList=new ArrayList<>();

    private int m=0;//markov状态值

    private int[][] changeCount;  //markov转移次数

    private int[] changeCountInCol;  //列转移次数总数

    private int changeCountSum;  //转移总数

    private double[][] changeP;  //markov转移概率

    private double[] marginP;   //markov边际概率

    private double[][] statistic;  //统计量 x^2

    private double[][] usePossibilityEachTime;  //记录用户在每时刻的使用概率

    private double[][][] appScore;      //app得分情况

    private ArrayList<ArrayList<HashSet<Integer>>> candidateAppSet=new ArrayList<>();  //下一次最后可能使用的候选app

    private int k;

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

    //测试获取用户事件数据

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<UsageEvents.Event> initEventList() {
        eventList.clear();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        long temp = cal.getTimeInMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(cal.getTimeInMillis(), System.currentTimeMillis());
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event nextEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(nextEvent);
            if(nextEvent.getEventType()==2&&!nextEvent.getPackageName().contains("miui")
                    &&!nextEvent.getPackageName().contains("android")){
                eventList.add(nextEvent);
            }

        }
        return eventList;
    }

    //测试用户事件数据
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dealEvents() throws InterruptedException {
        initEventList();   //初始化eventList
        String appName,preAppName="";
        for(int i=0;i<eventList.size();){
            UsageEvents.Event event=eventList.get(i);
            appName=event.getPackageName();
            /*//获取app中文名
            PackageManager packageManager = MyApplication.getContext().getPackageManager();
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = packageManager.getApplicationInfo(appName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String appChineseName=packageManager.getApplicationLabel(applicationInfo).toString();*/
            if(!preAppName.equals(appName)) {
                //Log.e("log", appChineseName + "," + event.getEventType() + "," + dateFormat.format(new Date(event.getTimeStamp())));
                preAppName=appName;
                i++;
            }else{
                eventList.remove(event);
            }
        }
        markov();
    }
    //计算马尔可夫转移概率
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void markov() throws InterruptedException {
        //处理数据分为训练集和验证集
        long boundary=System.currentTimeMillis()-3600*24*1000*2;
        for(UsageEvents.Event event:eventList){
            if(event.getTimeStamp()<boundary){
                trainEventList.add(event);
            }else{
                testEventList.add(event);
            }
        }
        //1.求markov状态对应map
        for(UsageEvents.Event event:trainEventList){
            String appName=event.getPackageName();
            if(!markovStateMap.containsKey(appName)){
                markovStateMap.put(appName,m++);
            }
        }
        //计算changeCount
        calculateChangeCount();
        //去除列总数过小数据
        Iterator<Map.Entry<String,Integer>> iterator=markovStateMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Integer> entry=iterator.next();
            if(changeCountInCol[(int)entry.getValue()]<15){
                iterator.remove();
            }
        }
        m=markovStateMap.size();
        //归正map索引
        int index=0;
        for(Map.Entry<String,Integer> entry:markovStateMap.entrySet()){
            markovStateMap.put(entry.getKey(),index);
            reverseMarkovStateMap.put(index++,entry.getKey());
        }
        //再次处理trainEventList
        trainEventList=trainEventList.stream().filter(o->markovStateMap.containsKey(o.getPackageName()))
                .collect(Collectors.toList());
        testEventList=testEventList.stream().filter(o->markovStateMap.containsKey(o.getPackageName()))
                .collect(Collectors.toList());
        //再次计算changeCount和changeCountInCol
        calculateChangeCount();
        //求转移概率矩阵
        changeP=new double[m][m];
        int[] changeCountInRow=new int[m];  //记录行总数
        for(int i=0;i<changeCount.length;i++){
            int sum=0;
            for(int num:changeCount[i]){
                changeCountInRow[i]+=num;
            }
        }
        for(int i=0;i<changeCount.length;i++){
            for(int j=0;j<changeCount[i].length;j++){
                changeP[i][j]=(double)changeCount[i][j]/changeCountInRow[i];
            }
        }
        //求边际概率P.j
        marginP=new double[m];
        for(int i=0;i<marginP.length;i++){
            marginP[i]=(double)changeCountInCol[i]/changeCountSum;
        }
        //求统计量x^2
        statistic=new double[m][m];
        double x_2=0;
        for(int i=0;i<statistic.length;i++){
            for(int j=0;j<statistic[0].length;j++){
                if(changeCount[i][j]==0){
                    statistic[i][j]=0;
                }else{
                    statistic[i][j]=Math.abs(Math.log(changeP[i][j]/marginP[j]));
                }
                x_2+=statistic[i][j];
            }
        }
        x_2=x_2*2;
        Log.e("x_2",x_2+"");
        for(Map.Entry<String,Integer> entry:markovStateMap.entrySet()){
            //获取app中文名
            PackageManager packageManager = MyApplication.getContext().getPackageManager();
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = packageManager.getApplicationInfo(entry.getKey(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String appChineseName=packageManager.getApplicationLabel(applicationInfo).toString();
            System.out.println(appChineseName);
        }
        calculatePossibilityEachTime();
        calculateScore(reverseMarkovStateMap.get(2),System.currentTimeMillis());
        markovTest();
    }
    //计算马尔可夫转移计数
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void calculateChangeCount(){
        //求转移次数矩阵
        changeCount=new int[m][m];
        for(int i=0,j=1;j<trainEventList.size();j++){
            UsageEvents.Event curEvent=trainEventList.get(i),nextEvent=trainEventList.get(j);
            if(markovStateMap.containsKey(nextEvent.getPackageName())){
                Integer x=markovStateMap.get(curEvent.getPackageName()),y=markovStateMap.get(nextEvent.getPackageName());
                changeCount[x][y]++;
                i=j;
            }
        }
        for(int[] nums:changeCount){
            for(int num:nums){
                System.out.print(num+",");
            }
            System.out.println();
        }
        changeCountInCol=new int[m];//记录列总数
        changeCountSum=0;//记录总数
        for(int j=0;j<changeCount[0].length;j++){
            for(int i=0;i<changeCount.length;i++){
                changeCountInCol[j]+=changeCount[i][j];
                changeCountSum+=changeCount[i][j];
            }
        }
    }

    //计算序列每时刻的条件概率
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void calculatePossibilityEachTime(){
        usePossibilityEachTime=new double[24][m];
        int[] usePossibilityEachTimeInRow=new int[24];
        for(UsageEvents.Event event:trainEventList){
            if(markovStateMap.containsKey(event.getPackageName())){
                long timeStamp=(event.getTimeStamp()+28_800_000)%86_400_000;
                String curTime=dateFormat.format(new Date(event.getTimeStamp()));
                String appName= event.getPackageName();
                int hours=(int)(timeStamp/3_600_000);
                usePossibilityEachTime[(int)(timeStamp/3_600_000)][markovStateMap.get(appName)]++;
                usePossibilityEachTimeInRow[(int)(timeStamp/3_600_000)]++;
            }
        }
        for(int i=0;i<usePossibilityEachTime.length;i++){
            for(int j=0;j<usePossibilityEachTime[0].length;j++){
                usePossibilityEachTime[i][j]/=usePossibilityEachTimeInRow[i];
            }
        }
    }
    //得出下一个最大可能使用应用
    public void calculateScore(String appName,long curTimeStamp) {
        //计算各app得分情况
        int curAppIndex = markovStateMap.get(appName);
        int curHours = (int) (((curTimeStamp + 28_800_000) % 86_400_000) / 3_600_000);
        appScore = new double[24][m][m];   //三维矩阵，时间-当前app-下一app
        for (int hour = 0; hour < 24; hour++) {
            candidateAppSet.add(new ArrayList<>());
            for (int x = 0; x < m; x++) {
                Double[] valueNums=new Double[m];Integer[] indexNums=new Integer[m];
                for(int i=0;i<indexNums.length;i++){  //初始化indexNums
                    indexNums[i]=i;
                }
                for (int y = 0; y < m; y++) {
                    appScore[hour][x][y] = changeP[x][y] * usePossibilityEachTime[hour][y];
                    valueNums[y]=appScore[hour][x][y];
                }
                //选择treeMap中前k个元素存入candidate
                k=1;
                candidateAppSet.get(hour).add(new HashSet<>());
                quickSort(valueNums,indexNums,0,m-1);
                while(k-->0){
                    candidateAppSet.get(hour).get(x).add(indexNums[m-k-1]);
                }
                System.out.println();
            }
        }
        System.out.println();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void markovTest(){
        int exactCount=0;
        int testAmount=0;
        double precision;
        for(int i=0,j=i+1;j<testEventList.size();i++,j++){
            UsageEvents.Event curEvent=testEventList.get(i),nextEvent=testEventList.get(j);
            int curHour=(int) (((curEvent.getTimeStamp() + 28_800_000) % 86_400_000) / 3_600_000);
            int curAppIndex=markovStateMap.get(curEvent.getPackageName());
            int nextAppIndex=markovStateMap.get(nextEvent.getPackageName());
            if(candidateAppSet.get(curHour).get(curAppIndex).contains(nextAppIndex)){
                exactCount++;
            }
            testAmount++;
        }
        precision=(double) exactCount/testAmount;
        System.out.println(precision);
    }
    //快速排序值数组和索引数组
    public void quickSort(Double[] valueNums,Integer[] indexNums,int l,int r){
        if(l>=r){
            return ;
        }
        double pivot=valueNums[r];
        int i=l,j=i;
        for(;j<r;j++){
            if(valueNums[j]<=pivot){
                swap(valueNums,i,j);
                swap(indexNums,i,j);
                i++;
            }
        }
        swap(valueNums,i,r);
        swap(indexNums,i,r);
        quickSort(valueNums,indexNums,l,i-1);
        quickSort(valueNums,indexNums,i+1,r);
    }

    public <T> void  swap(T[] nums,int i,int j){
        T temp=nums[i];
        nums[i]=nums[j];
        nums[j]=temp;
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
                .filter((o) -> usageDataLastTimeMap.containsKey(o.getPackageName())
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
