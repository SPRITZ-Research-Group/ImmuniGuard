/****************************************************************************************
 * Copyright (c) 2016, 2017, 2019 Vincent Hiribarren                                    *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * Linking Beacon Simulator statically or dynamically with other modules is making      *
 * a combined work based on Beacon Simulator. Thus, the terms and conditions of         *
 * the GNU General Public License cover the whole combination.                          *
 *                                                                                      *
 * As a special exception, the copyright holders of Beacon Simulator give you           *
 * permission to combine Beacon Simulator program with free software programs           *
 * or libraries that are released under the GNU LGPL and with independent               *
 * modules that communicate with Beacon Simulator solely through the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator and the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataParser interfaces. You may           *
 * copy and distribute such a system following the terms of the GNU GPL for             *
 * Beacon Simulator and the licenses of the other code concerned, provided that         *
 * you include the source code of that other code when and as the GNU GPL               *
 * requires distribution of source code and provided that you do not modify the         *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator and the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataParser interfaces.                   *
 *                                                                                      *
 * The intent of this license exception and interface is to allow Bluetooth low energy  *
 * closed or proprietary advertise data packet structures and contents to be sensibly   *
 * kept closed, while ensuring the GPL is applied. This is done by using an interface   *
 * which only purpose is to generate android.bluetooth.le.AdvertiseData objects.        *
 *                                                                                      *
 * This exception is an additional permission under section 7 of the GNU General        *
 * Public License, version 3 (“GPLv3”).                                                 *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.contacttracing.gaenrelay.bluetooth.model;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.ScanRecord;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import com.contacttracing.gaenrelay.bluetooth.AdvertiseDataGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ImmuniBeacon implements AdvertiseDataGenerator, Parcelable {

    public static final String ensUuid = "0000FD6F-0000-1000-8000-00805F9b34FB";
    private static final Logger sLogger = LoggerFactory.getLogger(ImmuniBeacon.class);
    private UUID serviceUuid;
    private byte[] RPI;

    public ImmuniBeacon() {
        setServiceUuid(UUID.fromString(ensUuid));
    }

    public UUID getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(UUID serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public byte[] getRPI() {
        return RPI;
    }

    public void setRPI(byte[] RPI) {
        this.RPI = RPI;
    }

    @Override
    public AdvertiseData generateAdvertiseData() {
        AdvertiseData.Builder data = new AdvertiseData.Builder();
        data.setIncludeDeviceName(false);
        data.addServiceUuid(new ParcelUuid(this.getServiceUuid()));
        byte[] serviceData = this.getRPI();
        data.addServiceData(new ParcelUuid(this.getServiceUuid()), serviceData);
        AdvertiseData adv = data.build();
        return adv;
    }

    public static ImmuniBeacon parseRecord(ScanRecord scanRecord) {
        // Check data validity
        List<ParcelUuid> scanRecordUuid = scanRecord.getServiceUuids();
        UUID immuniUuid = UUID.fromString(ensUuid);
        ParcelUuid immuniPUuid = new ParcelUuid(immuniUuid);
        boolean isImmuniBeacon = false;

        if ( scanRecordUuid != null ) {
            for ( ParcelUuid PUuid : scanRecordUuid ) {
                if (PUuid.equals(immuniPUuid)) {
                    sLogger.debug("Exposure Notification UUID detected: " + scanRecordUuid);
                    isImmuniBeacon = true;
                    immuniPUuid = PUuid;
                }
            }
        }

        if (!isImmuniBeacon) {
            return null;
        }

        final ImmuniBeacon immuniBeacon = new ImmuniBeacon();
        immuniBeacon.setServiceUuid(immuniUuid);

        final byte[] scanRecordData = scanRecord.getServiceData(immuniPUuid);

        final byte[] byteRPI = Arrays.copyOfRange(scanRecordData, 0, 20);

        immuniBeacon.setRPI(byteRPI);

        return immuniBeacon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this.serviceUuid);
        dest.writeByteArray(this.RPI);
    }

    protected ImmuniBeacon(Parcel in) {
        this.serviceUuid = (UUID) in.readSerializable();
        in.readByteArray( this.RPI );
    }

    public static final Creator<ImmuniBeacon> CREATOR = new Creator<ImmuniBeacon>() {
        @Override
        public ImmuniBeacon createFromParcel(Parcel source) {
            return new ImmuniBeacon(source);
        }

        @Override
        public ImmuniBeacon[] newArray(int size) {
            return new ImmuniBeacon[size];
        }
    };
}
