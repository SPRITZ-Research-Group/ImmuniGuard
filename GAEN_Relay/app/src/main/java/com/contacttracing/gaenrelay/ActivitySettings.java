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

package com.contacttracing.gaenrelay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.akexorcist.localizationactivity.LocalizationActivity;

import com.contacttracing.gaenrelay.R;


public class ActivitySettings extends LocalizationActivity {

    public static void launchActivity(Activity context) {
        final Intent intent = new Intent(context, ActivitySettings.class);
        context.startActivity(intent);
        context.overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((App)getApplication()).updateLanguage(this); // Before setContentView
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.pref_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_right_out);
    }

}
