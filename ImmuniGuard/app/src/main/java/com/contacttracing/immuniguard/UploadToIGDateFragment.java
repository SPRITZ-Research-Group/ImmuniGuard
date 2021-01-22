package com.contacttracing.immuniguard;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class UploadToIGDateFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private final static String TAG = "UploadToIGDateFragment";
    private final static int contagiousDays = 14;
    private final Context context;

    public UploadToIGDateFragment(Context context) {
        this.context = context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        Log.i(TAG,"onCreate Date: " + year + "-" + month + "-" + day);
        // Create a new instance of TimePickerDialog and return it
        DatePickerDialog dpd = new DatePickerDialog(getActivity(),this, year, month, day);
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis() + 1000);
        return dpd;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Log.i(TAG, "Positive test date " + view.getDayOfMonth() +
                " / " + (view.getMonth()+1) +
                " / " + view.getYear());
        Log.i(TAG,"onDateSet: " + year + "-" + (month+1) + "-" + day);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        long contagiousTimeStart = cal.getTimeInMillis() / 100000;
        long contagiousTimeStop = contagiousTimeStart - (contagiousDays * 864);
        new UploadToIGAsyncTask(context,contagiousTimeStart,contagiousTimeStop).execute();
    }
}