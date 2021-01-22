package com.contacttracing.gaenrelay;

import android.os.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ActivityLoadRelay extends AsyncTask<Integer, Void, String> {

    private static final Logger sLogger = LoggerFactory.getLogger(ActivityMain.class);

    protected String doInBackground(Integer... num) {
        sLogger.debug("connecting to relay db - load operation");
        HttpURLConnection urlConnection = null;
        String latestRPI = "";
        try {
            String apiaddress = "https://frozen-wildwood-51801.herokuapp.com/immunikeysload.php?";
            int lim = num[0];
            URL url = new URL(apiaddress + "lim=" + lim );
            sLogger.debug("URL: " + url);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line);
            }
            sLogger.debug("result: " + sb.toString());
            latestRPI = sb.toString();
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return latestRPI;
    }

}