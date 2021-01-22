/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.contacttracing.immuniguard;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BleUpdatesService extends Service {

    private static final String PACKAGE_NAME = "com.contacttracing.immuniguard";

    private static final String TAG = BleUpdatesService.class.getSimpleName();

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_02";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";
    public static String EXTRA_RPI = PACKAGE_NAME + ".ble";

    private final IBinder mBinder = new LocalBinder();

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345679;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */

    private NotificationManager mNotificationManager;

    private Handler mServiceHandler;
    private boolean mScanning;
    private boolean mChangingConfiguration = false;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 15000;

    // Initializes Bluetooth adapter.
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;

    private List<String> mRpiList;
    private ScanCallback mLeScanCallback;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private ScanFilter mScanFilter;
    private List<ScanFilter> mFilters;
    List<String> macAddresses;

    // ImmuniUuid
    private static final ParcelUuid immuniUuidParcel = new ParcelUuid(
            UUID.fromString("0000FD6F-0000-1000-8000-00805F9b34FB"));

    public BleUpdatesService() {
    }

    @Override
    public void onCreate() {

        Log.i(TAG, "BleUpdatesService onCreate()");

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mRpiList = new Vector<String>();
        macAddresses = new Vector<String>();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        }

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

        mFilters = new Vector<ScanFilter>();

        mScanFilter = new ScanFilter.Builder().setServiceUuid(immuniUuidParcel).build();
        mFilters.add(mScanFilter);

        mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String macAddress;
                List<ParcelUuid> scanRecordUuidList = result.getScanRecord().getServiceUuids();
                macAddress = result.getDevice().getAddress();
                Log.i(TAG, "Scanrecord Uuid: " + scanRecordUuidList);
                if ( scanRecordUuidList != null && !alreadyScanned(macAddresses, macAddress) ) {
                    for ( ParcelUuid PUuid : scanRecordUuidList ) {
                        final byte[] scanRecordData = result.getScanRecord().getServiceData(immuniUuidParcel);
                        final String rpi = ByteTools.bytesToHex(Arrays.copyOfRange(scanRecordData,0,20));
                        onNewRPI(rpi);
                    }
                }
                else {
                    Log.i(TAG, "No Scanrecord");
                }
            }
        };

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.ble_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    private boolean alreadyScanned(List<String> ma, String addr) {
        for (int i = 1; i < ma.size(); i++) {
            if (ma.get(i) == addr) {
                return true;
            }
        }
        macAddresses.add(addr);
        return false;
    }

    private void scanLeDevice() {
        if (!mScanning) {
            // Stops scanning after a pre-defined scan period.
            mServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (mLeScanner != null) {
                        mLeScanner.stopScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLeScanner.startScan(mFilters, mScanSettings, mLeScanCallback);
        } else {
            mScanning = false;
            mLeScanner.stopScan(mLeScanCallback);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Ble Start Command");

        ScheduledExecutorService mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Scan initiated");
                scanLeDevice();
            }
        }, 10, 90, TimeUnit.SECONDS);

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeBleUpdates();
            stopSelf();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingBleUpdates(this)) {
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    public void requestBleUpdates() {
        Log.i(TAG, "Requesting Ble updates");
        Utils.setRequestingBleUpdates(this, true);
        startService(new Intent(getApplicationContext(), BleUpdatesService.class));
    }

    public void removeBleUpdates() {
        Log.i(TAG, "Removing Ble updates");
        try {
            Utils.setRequestingBleUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingBleUpdates(this, true);
            Log.e(TAG, "Lost Ble permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, BleUpdatesService.class);

        CharSequence text = mRpiList.get(0);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_updates),
                        servicePendingIntent)
                .setContentText(text)
                .setContentTitle("Rpi")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BleUpdatesService getService() {
            return BleUpdatesService.this;
        }
    }

    private void onNewRPI(String rpi) {
        if (!mRpiList.contains(rpi)) {
            mRpiList.add(rpi);
        }

        Intent intent = new Intent(ACTION_BROADCAST);
        Rpi parcRpi = new Rpi(rpi);
        intent.putExtra(EXTRA_RPI, parcRpi);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }


    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(BleUpdatesService context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}
