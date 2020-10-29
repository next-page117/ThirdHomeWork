package com.example.thirdhomework;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class MyService extends Service {


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public MyService(){

    }
    //onCreate是在创建服务时才调用
    @Override
    public void onCreate() {
        Log.e("service state","service start");
        super.onCreate();
    }

    //在每次服务启动的时候调用
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    //实现前台服务
    /*@RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String ID="Notification_ID";
        String NAME="Notification_NAME";
        NotificationManager manager=(NotificationManager)getSystemService (NOTIFICATION_SERVICE);
        NotificationChannel channel=new NotificationChannel(ID,NAME,NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel (channel);
        Notification notification=new Notification.Builder (this,ID)
                .setContentTitle ("收到一条重要通知")
                .setContentText ("这是重要通知")
                .setSmallIcon (R.mipmap.ic_launcher)
                .build ();
        startForeground (1,notification);

        return super.onStartCommand(intent, flags, startId);
    }*/

}
