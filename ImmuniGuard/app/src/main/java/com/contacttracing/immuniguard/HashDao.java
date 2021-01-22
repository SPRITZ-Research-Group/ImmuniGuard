package com.contacttracing.immuniguard;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HashDao {
    @Query("SELECT * FROM hashentity")
    List<HashEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(HashEntity hashentity);
}