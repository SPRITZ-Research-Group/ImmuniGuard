package com.contacttracing.immuniguard;

import android.os.Parcel;
import android.os.Parcelable;

public class Rpi implements Parcelable {
    String rpi;

    public Rpi(String s) {
        rpi = s;
    }

    protected Rpi(Parcel in) {
        rpi = in.readString();

    }

    public String getRpi() {
        return rpi;
    }

    public static final Creator<Rpi> CREATOR = new Creator<Rpi>() {
        @Override
        public Rpi createFromParcel(Parcel in) {
            return new Rpi(in);
        }

        @Override
        public Rpi[] newArray(int size) {
            return new Rpi[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rpi);
    }
}