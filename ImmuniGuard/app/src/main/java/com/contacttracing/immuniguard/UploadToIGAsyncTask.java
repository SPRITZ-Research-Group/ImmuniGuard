package com.contacttracing.immuniguard;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

public class UploadToIGAsyncTask extends AsyncTask<Void, Void, List<String>> {
    // your database endpoint
    private static final String urlDatabaseIG = "https://frozen-wildwood-51801.herokuapp.com/setPositiveHashIG.php?";
    private static final String TAG = "UploadToIGAsyncTask";
    private final Context context;

    private final long timestart;
    private final long timestop;

    public UploadToIGAsyncTask(Context context, long timestart, long timestop) {
        this.context = context;
        this.timestart = timestart;
        this.timestop = timestop;
    }

    protected List<String> doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        List<SensibleDataEntity> positiveRpi =
                new Vector<SensibleDataEntity>(MainActivity.mycontactsdb.sensibleDataDao().getPositive(timestart,timestop));
        SensibleDataEntity sde;
        List<String> hashlist = new Vector<String>();
        Log.i(TAG, "Evaluating Time between: " + timestart + " and " + timestop);
        for (int i =0; i<positiveRpi.size(); i++) {
            sde = positiveRpi.get(i);
            try {
                // https://frozen-wildwood-51801.herokuapp.com/setPositiveIG.php?rpi=0ac1433cc55125b0bcfb3fc444043259e7530c5d&location=45.833734%12.0154069&year=2020&month=8&day=1&hour=15&minute=33
                URL url = new URL(urlDatabaseIG + "hash=" + sde.hash
                );
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    sb.append(line);
                }
                Log.i(TAG, "Response num[" + i + "]: " + sb.toString());
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
        }
        if (positiveRpi.size() == 0) {
            Log.i(TAG, "No HASH between: " + timestart + " and " + timestop);
        }
        else {
            for (int i=0; i<positiveRpi.size(); i++) {
                hashlist.add(positiveRpi.get(i).hash);
            }
        }
        return hashlist;
    }

    protected void onPostExecute(List<String> hashlist) {
        for (int i=0; i<hashlist.size(); i++) {
            Toast.makeText(context, "Uploaded HASH: " + hashlist.get(i), Toast.LENGTH_LONG).show();
        }
    }

}
