package com.example.thirdhomework.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;


@Entity(tableName = "UsageData")
public class UsageData {

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "app_name")
    public String appName;
    @ColumnInfo(name="first_start_time")
    public long firstStartTime;
    @ColumnInfo(name="last_start_time")
    public long lastStartTime;
    @ColumnInfo(name="used_time")
    public long usedTime;
    @ColumnInfo(name="start_time_stamp")
    public long startTimeStamp;
    @Ignore
    public UsageData() {
    }

    public UsageData(int id, String appName, long firstStartTime, long lastStartTime, long usedTime) {
        this.id = id;
        this.appName = appName;
        this.firstStartTime = firstStartTime;
        this.lastStartTime = lastStartTime;
        this.usedTime = usedTime;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
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

    public long getFirstStartTime() {
        return firstStartTime;
    }

    public void setFirstStartTime(long firstStartTime) {
        this.firstStartTime = firstStartTime;
    }

    public long getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(long lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public long getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(long usedTime) {
        this.usedTime = usedTime;
    }
}
