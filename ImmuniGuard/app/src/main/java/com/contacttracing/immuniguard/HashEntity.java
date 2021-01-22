package com.contacttracing.immuniguard;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(indices = {@Index(value = {"hash"},
        unique = true)})
public class HashEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "hash")
    public String hash;


    HashEntity(String hash) {
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
        return Objects.hash(hash);
    }
}
