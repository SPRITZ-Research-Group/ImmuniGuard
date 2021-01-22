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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

import static java.time.LocalDateTime.now;

/**
 * The only activity in this sample.
 *
 * Note: Users have three options in "Q" regarding location:
 * <ul>
 *     <li>Allow all the time</li>
 *     <li>Allow while app is in use, i.e., while app is in foreground</li>
 *     <li>Not allow location at all</li>
 * </ul>
 * Because this app creates a foreground service (tied to a Notification) when the user navigates
 * away from the app, it only needs location "while in use." That is, there is no need to ask for
 * location all the time (which requires additional permissions in the manifest).
 *
 * "Q" also now requires developers to specify foreground service type in the manifest (in this
 * case, "location").
 *
 * Note: For Foreground Services, "P" requires additional permission in manifest. Please check
 * project manifest for more information.
 *
 * Note: for apps running in the background on "O" devices (regardless of the targetSdkVersion),
 * location may be computed less frequently than requested when the app is not in the foreground.
 * Apps that use a foreground service -  which involves displaying a non-dismissable
 * notification -  can bypass the background location limits and request location updates as before.
 *
 * This sample uses a long-running bound and started service for location updates. The service is
 * aware of foreground status of this activity, which is the only bound client in
 * this sample. After requesting location updates, when the activity ceases to be in the foreground,
 * the service promotes itself to a foreground service and continues receiving location updates.
 * When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that foreground service is removed.
 *
 * While the foreground service notification is displayed, the user has the option to launch the
 * activity from the notification. The user can also remove location updates directly from the
 * notification. This dismisses the notification and stops the service.
 */
public class MainActivity extends AppCompatActivity {

    // TAG
    private static final String TAG = MainActivity.class.getSimpleName();

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_PERMISSIONS_BT_REQUEST_CODE = 35;

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private LocationReceiver LocationReceiver;
    private BleReceiver BleReceiver;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;
    private BleUpdatesService BleService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    private boolean BleBound = false;

    // UI elements.
    private Button mRequestUpdatesButton;
    private Button mUploadPositiveButton;
    private Button mDownloadPositiveButton;
    private Button mUpdateHealthStatusButton;

    // Health Status
    private TextView mHealthStatusTextView;

    // Databases
    public static HashDatabase positivedb;
    public static SensibleDataDatabase mycontactsdb;

    // Location
    public static String realLocation = "Starting";
    public static double lastLatitude;
    public static double lastLongitude;

    // RPI Generation
    private ScheduledExecutorService mScheduledExecutorService;
    public static String lastRPI = "Starting";

    // Contagious Contacts
    public static List<String> contagiousContacts;

    // Discovered RPIs
    public static List<String> discoveredRPIs;

    // Monitors the state of the connection to the Location service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    // Monitors the state of the connection to the Ble service.
    private final ServiceConnection BleServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleUpdatesService.LocalBinder binder = (BleUpdatesService.LocalBinder) service;
            BleService = binder.getService();
            BleBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            BleService = null;
            BleBound = false;
        }
    };

    // Random String Generation
    public static RandomStringGenerator mRandomStringGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocationReceiver = new LocationReceiver();
        BleReceiver = new BleReceiver();
        setContentView(R.layout.activity_main);

        // retrieve databases
        positivedb = Room.databaseBuilder(getApplicationContext(), HashDatabase.class,"positivedb").build();
        mycontactsdb = Room.databaseBuilder(getApplicationContext(), SensibleDataDatabase.class,"mycontactsdb").build();

        // Check that the user hasn't revoked permissions by going to Settings.
        if (Utils.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mRequestUpdatesButton = (Button) findViewById(R.id.request_updates_button);
        mUploadPositiveButton = (Button) findViewById(R.id.upload_positive_button);
        mDownloadPositiveButton = (Button) findViewById(R.id.download_positive_button);
        mUpdateHealthStatusButton = (Button) findViewById(R.id.update_health_status_button);

        mHealthStatusTextView = (TextView) findViewById(R.id.health_status_textview);

        mRequestUpdatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkPermissions()) {
                    requestPermissions();

                } else {
                    mService.requestLocationUpdates();
                    BleService.requestBleUpdates();
                }
                mRequestUpdatesButton.setEnabled(false);
            }
        });

        mUploadPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new UploadToIGDateFragment(getApplicationContext());
                newFragment.show(getSupportFragmentManager(), "Upload Date Picker");
            }
        });

        mDownloadPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DownloadFromIGAsyncTask(getApplicationContext()).execute();
            }
        });

        mUpdateHealthStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateHealthStatus();
            }
        });

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, BleUpdatesService.class), BleServiceConnection,
                Context.BIND_AUTO_CREATE);

        // Contagious Contacts
        contagiousContacts = new Vector<String>();

        // Discovered RPIs
        discoveredRPIs = new Vector<String>();

        // Periodic own RPI generation
        mRandomStringGenerator = new RandomStringGenerator();
        final Handler handlerMyRPI = new Handler(Looper.getMainLooper());
        final Runnable runnableMyRPI = new Runnable() {
            public void run() {
                announceMyRPI();
                if (true) {
                    handlerMyRPI.postDelayed(this, 30000);
                }
            }
        };
        handlerMyRPI.post(runnableMyRPI);

        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        if( mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Bluetooth Not Supported",Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
            }
        }
    }

    public void announceMyRPI() {
        MainActivity.lastRPI = MainActivity.mRandomStringGenerator.getRandomString(40 );
        Log.i(TAG,"My new RPI is: " + MainActivity.lastRPI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Toast.makeText(MainActivity.this,"My new RPI: " + MainActivity.lastRPI, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(LocationReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(BleReceiver,
                new IntentFilter(BleUpdatesService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(LocationReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BleReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        if (BleBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(BleServiceConnection);
            BleBound = false;
        }
        super.onStop();
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
                BleService.requestBleUpdates();
            } else {
                // Permission denied.
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                realLocation = location.getLatitude() + " " + location.getLongitude();
                lastLatitude = Utils.round(location.getLatitude(),3);
                lastLongitude = Utils.round(location.getLongitude(),3);
                Log.i(TAG,"lastLatitude: " + lastLatitude + "  lastLongitude: " + lastLongitude );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Toast.makeText(MainActivity.this, "GPS Location: " + lastLatitude + ", " + lastLongitude, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Rpi parcRpi = intent.getParcelableExtra(BleUpdatesService.EXTRA_RPI);
                String hash = "";
                if (parcRpi != null && !parcRpi.equals("Starting") && realLocation != "Starting") {
                    String contactRPI = parcRpi.getRpi();
                    long currentTime = Calendar.getInstance().getTimeInMillis() / 100000;
                    // md5 hash function
                    String datastring = lastRPI + contactRPI + lastLatitude + lastLongitude + currentTime;
                    byte[] bytesOfMessage = new byte[0];
                    try {
                        bytesOfMessage = datastring.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    MessageDigest md = null;
                    try {
                        md = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    hash = ByteTools.bytesToHex(md.digest(bytesOfMessage));
                    SensibleDataEntity sde = new SensibleDataEntity(lastRPI, contactRPI,
                            String.valueOf(lastLatitude), String.valueOf(lastLongitude), currentTime, hash);
                    Log.i(TAG,"---HASH--- MyRPI: " + sde.myrpi + " --- ContactRPI: " + contactRPI + " --- Latitude: " + sde.latitude +
                            " --- Longitude: " + sde.longitude + " --- Time: " + sde.time + " --- HASH: " + sde.hash);
                    new DBOperationAsyncTask(sde,mycontactsdb).execute();
                    boolean alreadydiscovered = false;
                    for (int i=0; i<discoveredRPIs.size(); i++) {
                        if (contactRPI == discoveredRPIs.get(i)) {
                            Log.i(TAG, "Already Discovered: " + contactRPI + " == " + discoveredRPIs.get(i));
                            alreadydiscovered = true;
                        }
                    }
                    if (!alreadydiscovered) {
                        Log.i(TAG, "Nearby RPI: " + contactRPI);
                        Toast.makeText(MainActivity.this, "Nearby RPI: " + contactRPI + "\n HASH: " + hash, Toast.LENGTH_LONG).show();
                        discoveredRPIs.add(contactRPI);
                    }
                }
            }
        }
    }

    public void updateHealthStatus() {
        Log.i(TAG,"ContagiousContacts size: " + contagiousContacts.size());
        String hashstring;
        for (int i=0; i<contagiousContacts.size(); i++) {
            hashstring = contagiousContacts.get(i);
            Log.i(TAG,"CONTAGIOUS HASH; " + hashstring);
        }
        Toast.makeText(MainActivity.this, "Total Contagious Contacts: " + contagiousContacts.size(), Toast.LENGTH_SHORT).show();

        if (contagiousContacts.size() > 0) {
            mHealthStatusTextView.setText(contagiousContacts.size() + " " + getResources().getString(R.string.health_risk));
            mHealthStatusTextView.setBackgroundColor(getResources().getColor(R.color.colorHealthRisk));
        }
        else {
            mHealthStatusTextView.setText(getResources().getString(R.string.health_good));
            mHealthStatusTextView.setBackgroundColor(getResources().getColor(R.color.colorHealthGood));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}
