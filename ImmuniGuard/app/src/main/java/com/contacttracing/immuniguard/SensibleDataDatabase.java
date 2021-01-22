package com.contacttracing.immuniguard;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SensibleDataEntity.class}, version = 1)
public abstract class SensibleDataDatabase extends RoomDatabase {
    public abstract SensibleDataDao sensibleDataDao();
}
