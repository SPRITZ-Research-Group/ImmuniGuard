package com.contacttracing.immuniguard;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public final class DBOperationAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "DBOperationAsyncTask";
    private SensibleDataEntity sde;
    private SensibleDataDatabase db;

    public DBOperationAsyncTask(SensibleDataEntity sde, SensibleDataDatabase db) {
        this.sde = sde;
        this.db = db;
    }

    @Override
    protected Void doInBackground(Void... params) {
        db.sensibleDataDao().insert(sde);
        List<SensibleDataEntity> lsde = db.sensibleDataDao().getAll();
        SensibleDataEntity ssde;
        Log.i(TAG,"--DB--");
        for (int i=0; i<lsde.size(); i++) {
            ssde = lsde.get(i);
            Log.i(TAG,"MyRPI: " + ssde.myrpi + " --- ContactRPI: " + ssde.contactrpi + " --- Latitude: " + ssde.latitude +
                    " --- Longitude: " + ssde.longitude + " --- Time: " + ssde.time + " --- HASH: " + ssde.hash );
        }
        return null;
    }
}