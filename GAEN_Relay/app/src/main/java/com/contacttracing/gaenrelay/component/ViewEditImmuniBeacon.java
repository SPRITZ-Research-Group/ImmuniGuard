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

package com.contacttracing.gaenrelay.component;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.contacttracing.gaenrelay.ActivityLoadRelay;
import com.contacttracing.gaenrelay.R;
import com.contacttracing.gaenrelay.bluetooth.ByteTools;
import com.contacttracing.gaenrelay.bluetooth.model.ImmuniBeacon;
import com.contacttracing.gaenrelay.bluetooth.model.BeaconModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class ViewEditImmuniBeacon extends FrameLayout implements BeaconModelEditor {

    private static final Logger sLogger = LoggerFactory.getLogger(ViewEditImmuniBeacon.class);

    private final TextInputLayout mUuidLayout;
    private final TextInputLayout mRPILayout;
    private final TextInputEditText mUuidValue;
    private final TextInputEditText mRPIValue;
    private final Button mUuidButton;
    private final Button mLatestRPIButton;

    public ViewEditImmuniBeacon(Context context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.card_beacon_immunibeacon_edit, this);

        mUuidLayout = (TextInputLayout)view.findViewById(R.id.cardimmunibeacon_textinputlayout_uuid);
        mRPILayout = (TextInputLayout)view.findViewById(R.id.cardimmunibeacon_textinputlayout_rpi);
        mUuidValue = (TextInputEditText)view.findViewById(R.id.cardimmunibeacon_textinput_uuid);
        mUuidValue.addTextChangedListener(new SimplifiedTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkUuidValue();
            }
        });
        mRPIValue = (TextInputEditText)view.findViewById(R.id.cardimmunibeacon_textinput_rpi);
        mRPIValue.addTextChangedListener(new SimplifiedTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkRPIValue();
            }
        });
        mUuidButton = (Button)view.findViewById(R.id.cardimmunibeacon_button_generateuuid);
        mUuidButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mUuidValue.setText(ImmuniBeacon.ensUuid);
            }
        });
        mLatestRPIButton = (Button)view.findViewById(R.id.cardimmunibeacon_button_latestrpi);
        mLatestRPIButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String newLatestRPI = null;
                try {
                    newLatestRPI = new ActivityLoadRelay().execute(new Integer(1)).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                finally {
                    mRPIValue.setText( newLatestRPI );
                }
            }
        });
    }

    @Override
    public void loadModelFrom(BeaconModel model) {
        ImmuniBeacon immuniBeacon = model.getImmunibeacon();
        if (immuniBeacon == null) {
            return;
        }
        mUuidValue.setText(immuniBeacon.getServiceUuid().toString());
        mRPIValue.setText(ByteTools.bytesToHex(immuniBeacon.getRPI()));
        sLogger.debug( "loadModelFrom setText: " + ByteTools.bytesToHex(immuniBeacon.getRPI()));
    }

    @Override
    public boolean saveModelTo(BeaconModel model) {
        if ( ! checkAll() ) {
            return false;
        }
        ImmuniBeacon immuniBeacon = new ImmuniBeacon();
        immuniBeacon.setServiceUuid(UUID.fromString(mUuidValue.getText().toString()));
        immuniBeacon.setRPI(ByteTools.toByteArray(mRPIValue.getText().toString()));
        sLogger.debug( "saveModelTo getText: " + ByteTools.bytesToHex(immuniBeacon.getRPI()) );
        model.setImmuniBeacon(immuniBeacon);
        return true;
    }

    @Override
    public void setEditMode(boolean editMode) {
        mUuidValue.setEnabled(editMode);
        mRPIValue.setEnabled(editMode);
        mUuidButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
        mLatestRPIButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
    }

    private boolean checkUuidValue() {
        try {
            final String uuid = mUuidValue.getText().toString();
            if (uuid.length() < 36) {
                throw new IllegalArgumentException();
            }
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(uuid);
            mUuidLayout.setError(null);
        }
        catch (IllegalArgumentException e) {
            mUuidLayout.setError(getResources().getString(R.string.edit_error_uuid));
            return false;
        }
        return true;
    }

    private boolean checkRPIValue() {
        try {
            final String rpi = mRPIValue.getText().toString();
            sLogger.debug( "checkRPIValue: " + mRPIValue.getText().toString() );
            if (rpi.length() != 40) {
                throw new IllegalArgumentException();
            }
            mRPILayout.setError(null);
        }
        catch (IllegalArgumentException e) {
            mRPILayout.setError(getResources().getString(R.string.edit_error_rpi));
            return false;
        }
        return true;
    }

    private boolean checkAll() {
        return checkRPIValue() & checkUuidValue();
    }

    private abstract class SimplifiedTextWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start,
                                      int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }



}
