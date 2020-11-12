package com.example.thirdhomework;

import android.app.usage.UsageEvents;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.thirdhomework.entity.EventData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserBehaviorPrediction {
    private List<EventData> eventList=new ArrayList<>();

    private HashMap<String,Integer> markovStateMap=new HashMap<>();  //存储app名与状态索引映射

    private HashMap<Integer,String> reverseMarkovStateMap=new HashMap<>(); //存储状态索引与app名的映射

    private List<EventData> trainEventList=new ArrayList<>();  //训练集list

    private List<EventData> testEventList=new ArrayList<>();   //测试集list

    private int m=0;//markov状态个数

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

    public UserBehaviorPrediction(List<EventData> eventList) {
        this.eventList = eventList;
    }

    //计算马尔可夫转移概率
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void markov() {
        //处理数据分为训练集和验证集
        long boundary=System.currentTimeMillis()-3600*24*1000*2;
        for(EventData event:eventList){
            if(event.getTimeStamp()<boundary){
                trainEventList.add(event);
            }else{
                testEventList.add(event);
            }
        }
        //1.求markov状态对应map
        for(EventData event:trainEventList){
            String appName=event.getAppName();
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
        trainEventList=trainEventList.stream().filter(o->markovStateMap.containsKey(o.getAppName()))
                .collect(Collectors.toList());
        testEventList=testEventList.stream().filter(o->markovStateMap.containsKey(o.getAppName()))
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
            EventData curEvent=trainEventList.get(i),nextEvent=trainEventList.get(j);
            if(markovStateMap.containsKey(nextEvent.getAppName())){
                Integer x=markovStateMap.get(curEvent.getAppName()),y=markovStateMap.get(nextEvent.getAppName());
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
        for(EventData event:trainEventList){
            if(markovStateMap.containsKey(event.getAppName())){
                long timeStamp=(event.getTimeStamp()+28_800_000)%86_400_000;
                String appName= event.getAppName();
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
                k=5;
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
            EventData curEvent=testEventList.get(i),nextEvent=testEventList.get(j);
            int curHour=(int) (((curEvent.getTimeStamp() + 28_800_000) % 86_400_000) / 3_600_000);
            int curAppIndex=markovStateMap.get(curEvent.getAppName());
            int nextAppIndex=markovStateMap.get(nextEvent.getAppName());
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
}
