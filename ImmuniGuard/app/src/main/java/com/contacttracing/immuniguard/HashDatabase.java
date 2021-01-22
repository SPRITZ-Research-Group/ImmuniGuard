package com.contacttracing.immuniguard;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {HashEntity.class}, version = 1)
public abstract class HashDatabase extends RoomDatabase {
    public abstract HashDao HashDao();
}
