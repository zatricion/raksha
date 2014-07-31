/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloc.bluetooth.le;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bloc.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BlocActivity extends Activity {
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }
        
        if (DeviceControlActivity.isBLeServiceBound) {
        	moveOn(false);
        	return;
        }
        
        checkGooglePlayApk();   

    }
    
    private void moveOn(boolean noDevice) {
		final Intent intent = new Intent(this, DeviceControlActivity.class);
		intent.putExtra("noDevice", noDevice);
		startActivity(intent);
    }
    
    private void startBloc() {
	    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	        Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			final Intent intent = new Intent(this, DeviceScanActivity.class);
			startActivity(intent);
	    } else {
			final Intent intent = new Intent(this, DeviceControlActivity.class);
			intent.putExtra("noDevice", true);
			startActivity(intent);
	    }
    }
    
    // Check for Google Play (location service)	
    private void checkGooglePlayApk() {
        int isAvailable = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
        	startBloc();
            return;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
                    this, PLAY_SERVICES_RESOLUTION_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "Google Play Services unavailable", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
        return;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode) {
        case PLAY_SERVICES_RESOLUTION_REQUEST:
          if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Google Play Services must be installed.",
                Toast.LENGTH_SHORT).show();
            finish();
          }
          else if (resultCode == RESULT_OK) {
        	  startBloc();
          }
      }
      super.onActivityResult(requestCode, resultCode, data);
    }    
	
    @Override
    protected void onDestroy() {
        super.onDestroy();        
    }

    @Override
    protected void onResume() {
        super.onResume(); 
    }
 

    @Override
    protected void onPause() {
        super.onPause();
    }
}