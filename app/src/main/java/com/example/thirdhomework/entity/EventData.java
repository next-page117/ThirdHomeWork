package com.example.thirdhomework.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EventData {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name="app_name")
    public String appName;
    @ColumnInfo(name="app_chinese_name")
    public String appChineseName;
    @ColumnInfo(name="time_stamp")
    public long timeStamp;

    public EventData() {
    }

    public EventData(int id, String appName, String appChineseName, long timeStamp) {
        this.id = id;
        this.appName = appName;
        this.appChineseName = appChineseName;
        this.timeStamp = timeStamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppChineseName() {
        return appChineseName;
    }

    public void setAppChineseName(String appChineseName) {
        this.appChineseName = appChineseName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
