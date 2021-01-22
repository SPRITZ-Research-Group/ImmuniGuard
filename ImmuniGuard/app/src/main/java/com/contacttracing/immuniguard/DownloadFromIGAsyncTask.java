package com.contacttracing.immuniguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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

public class DownloadFromIGAsyncTask extends AsyncTask<Void, Void, Integer>  {
    // your database endpoint
    private static final String urlDatabaseIG = "https://frozen-wildwood-51801.herokuapp.com/getPositiveHashIG.php?";
    private static final String TAG = "DownloadToIGAsyncTask";
    private List<HashEntity> lhashePos;
    private final Context context;

    public DownloadFromIGAsyncTask(Context context) {
        this.context = context;
    }

    protected Integer doInBackground(Void... params) {
        HttpURLConnection urlConnection = null;
        lhashePos = new Vector<HashEntity>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastDownloadedID = prefs.getString("last_downloaded_id", "0");
        int lastID = -1;
        Integer newContagiousContacts = 0;
        try {
            URL url = new URL(urlDatabaseIG
                    + "id=" + lastDownloadedID
            );
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
            String hash;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lastID = Integer.parseInt(line);
                hash = br.readLine();
                MainActivity.positivedb.HashDao().insert(new HashEntity(hash));
            }
            Log.i(TAG,"Response received");
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
            if (lastID != -1) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("last_downloaded_id", String.valueOf(lastID));
                editor.commit();
            }
            Log.i(TAG, "Last downloaded id: " + prefs.getString("last_downloaded_id", "0"));
            List<SensibleDataEntity> lsdeContacts = MainActivity.mycontactsdb.sensibleDataDao().getAll();
            lhashePos = MainActivity.positivedb.HashDao().getAll();
            SensibleDataEntity sdeTemp;
            HashEntity hashePos;
            Log.i(TAG,"--CONTACTSDB--");
            for (int i=0; i<lhashePos.size(); i++) {
                hashePos = lhashePos.get(i);
                for (int j=0; j<lsdeContacts.size(); j++) {
                    newContagiousContacts++;
                    sdeTemp = lsdeContacts.get(j);
                    Log.i(TAG, "POSITIVE HASH: " + hashePos.hash + " CONTACT HASH: " + sdeTemp.hash);
                    if ((sdeTemp.hash).equals(hashePos.hash)) {
                        Log.i(TAG, "New Contagious HASH: " + sdeTemp.hash);
                        MainActivity.contagiousContacts.add(sdeTemp.hash);
                    }
                }
            }
        }
        return newContagiousContacts;
    }

    protected void onPostExecute(Integer newContagiousContacts) {
        Toast.makeText(context,"New HASHES downloaded: " + newContagiousContacts,Toast.LENGTH_SHORT).show();
    }
}
