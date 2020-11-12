package com.example.thirdhomework;

import android.content.Context;

import androidx.room.Database;

import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.example.thirdhomework.dao.EventDao;
import com.example.thirdhomework.dao.UsageDao;
import com.example.thirdhomework.entity.EventData;
import com.example.thirdhomework.entity.UsageData;

@Database(entities = {UsageData.class, EventData.class},version=1,exportSchema = false)
public abstract class UsageDatabase extends RoomDatabase {

    public static volatile UsageDatabase sInstance;


    public abstract UsageDao usageDao();

    public abstract EventDao eventDao();


    public static synchronized UsageDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room
                    .databaseBuilder(context.getApplicationContext(), UsageDatabase.class, "usage_data")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return sInstance;
    }
}
