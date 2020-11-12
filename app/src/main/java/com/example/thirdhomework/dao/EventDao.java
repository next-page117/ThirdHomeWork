package com.example.thirdhomework.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.thirdhomework.entity.EventData;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM EventData")
    List<EventData> getAll();

    @Query("SELECT * FROM EventData WHERE id IN (:userIds)")
    List<EventData> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM EventData WHERE app_chinese_name LIKE '%' || :name || '%'")
    List<EventData> findByName(String name);

    @Insert
    void insertAll(EventData... users);

    @Delete
    void delete(EventData user);
    @Query("DELETE FROM EventData")
    void deleteAll();
    @Query("DELETE FROM EventData WHERE app_chinese_name LIKE '%' || :name || '%'")
    void deleteByName(String name);
    @Query("SELECT * FROM EventData WHERE app_name LIKE '%' || :name || '%'")
    List<EventData> findByUniqueName(String name);
}
