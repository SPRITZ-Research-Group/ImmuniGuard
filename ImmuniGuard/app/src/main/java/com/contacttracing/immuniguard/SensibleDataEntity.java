package com.contacttracing.immuniguard;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(indices = {@Index(value = {"myrpi","contactrpi","latitude","longitude","time","hash"},
        unique = true)})
public class SensibleDataEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "myrpi")
    public String myrpi;

    @ColumnInfo(name = "contactrpi")
    public String contactrpi;

    @ColumnInfo(name = "latitude")
    public String latitude;

    @ColumnInfo(name = "longitude")
    public String longitude;

    @ColumnInfo(name = "time")
    public long time;

    @ColumnInfo(name = "hash")
    public String hash;


    SensibleDataEntity(String myrpi, String contactrpi, String latitude, String longitude, long time, String hash) {
        this.myrpi = myrpi;
        this.contactrpi = contactrpi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.hash = hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensibleDataEntity that = (SensibleDataEntity) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, myrpi, contactrpi, latitude, longitude, time, hash);
    }
}
