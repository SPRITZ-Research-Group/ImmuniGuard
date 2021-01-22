package com.contacttracing.immuniguard;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensibleDataDao {
    @Query("SELECT * FROM sensibledataentity")
    List<SensibleDataEntity> getAll();

    // select records compatible to positive diagnosis
    @Query("SELECT * FROM sensibledataentity WHERE (time <= :timestart  AND time >= :timestop)")
    List<SensibleDataEntity> getPositive(long timestart, long timestop);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(SensibleDataEntity sensibledataentity);

    @Query("INSERT OR IGNORE INTO sensibledataentity(myrpi,contactrpi,latitude,longitude,time) " +
            "VALUES (:myrpi,:contactrpi,:latitude,:longitude,:time)")
    void insertOrIgnore(String myrpi, String contactrpi, String latitude, String longitude, long time);

    @Delete
    void delete(SensibleDataEntity sensibledataentity);
}