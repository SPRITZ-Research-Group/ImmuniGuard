package com.contacttracing.gaenrelay;

import android.os.AsyncTask;

import com.contacttracing.gaenrelay.bluetooth.ByteTools;
import com.contacttracing.gaenrelay.bluetooth.model.BeaconModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ActivitySaveRelay extends AsyncTask<BeaconModel, Void, Void> {

    private static final Logger sLogger = LoggerFactory.getLogger(ActivityMain.class);

    protected Void doInBackground(BeaconModel... mBeaconModel) {
        sLogger.debug("connecting to relay db - save operation");
        HttpURLConnection urlConnection = null;
        try {
            String apiaddress = "https://frozen-wildwood-51801.herokuapp.com/immunikeyssave.php?";
            String beaconmac = mBeaconModel[0].getBleMacAddress();
            boolean expnotif = mBeaconModel[0].getImmunibeacon().getServiceUuid().toString().equals("0000fd6f-0000-1000-8000-00805f9b34fb");
            String beaconrpi = ByteTools.bytesToHex(mBeaconModel[0].getImmunibeacon().getRPI());
            URL url = new URL(apiaddress + "mac=" + beaconmac + "&expnotif=" + expnotif + "&rpi=" + beaconrpi );
            sLogger.debug("URL: " + url);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return null;
    }

}