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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.bloc.MainWithMapActivity;
import com.bloc.R;
import com.bloc.settings.contacts.Contact;
import com.bloc.settings.contacts.ContactPickerDialog;
import com.bloc.settings.prefs.RadiusPickerDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends CloudBackendActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String ACTION_USER_DISCONNECT = "com.bloc.bluetooth.le.ACTION_USER_DISCONNECT";
    public static final String ACTION_RADIUS_CHANGE = "com.bloc.bluetooth.le.ACTION_RADIUS_CHANGE";
    public static final String ACTION_LOC_CHANGE = "com.bloc.bluetooth.le.ACTION_LOC_CHANGE";

    public static final String KEY_CONTACTS = "contacts";
    public static final String KEY_RADIUS = "radius";
       
    public static ArrayList<Contact> mContactList;
    public static int mRadius;
        
    public static boolean isBLeServiceBound = false;
    
    private LocationManager manager;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        final Intent intent = getIntent();
        final String action = intent.getAction();
        
        // If user closes the app task, this is called in order to set the global backend up again
        if (action != null && action.equals(BackgroundService.ACTION_BACKEND)) {
        	((BlocApplication) this.getApplication()).setAccountName(getAccountName());
        	((BlocApplication) this.getApplication()).setBackend(getCloudBackend());
        	finish();
        }
        // Call this last so that onPostCreate is called after device address obtained
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onPostCreate() {
    	super.onPostCreate();
    	
    	((BlocApplication) this.getApplication()).setAccountName(getAccountName());
    	((BlocApplication) this.getApplication()).setBackend(getCloudBackend());

    	// Start background service
        if (!BackgroundService.isRunning) {
        	// Enable location tracking
        	Intent bgServiceIntent = new Intent(this, BackgroundService.class);
        	bgServiceIntent.setAction(BackgroundService.ACTION_INIT);
        	startService(bgServiceIntent);    
        }
        
        // Set up contacts
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String contacts = prefs.getString(KEY_CONTACTS, null);
        
        if (contacts != null) {
        	Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<Contact>>(){}.getType();
            ArrayList<Contact> contact_list = gson.fromJson(contacts, collectionType);
        	mContactList = contact_list;
        }
    }
    
    private void checkLocationEnabled() {
        if (manager == null) {
            manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { 
            //GPS Provider disabled
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            
            dialog.setTitle("You must enable Location Services to use this application.");  
            
            // Allow enable
            dialog.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });

            dialog.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(DeviceControlActivity.this, "Location not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            
            dialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(final DialogInterface arg0) {
                    Toast.makeText(DeviceControlActivity.this, "Location not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            
            dialog.show();
        }
    }
    
    public void showContactPickerDialog() {
		ContactPickerDialog dlg = new ContactPickerDialog(true);
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getSupportFragmentManager(), "contacts");
	}

    public void showRadiusPickerDialog() {
    	RadiusPickerDialog dlg = new RadiusPickerDialog(true);
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getSupportFragmentManager(), "radius");
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Make sure location is enabled
    	checkLocationEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    
    // methods for onClick events set up in device_control.xml  
    public void quitApplication(View view) {
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
		
    	exitActivities(view);
    	
    	new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
            	Intent stopBackgroundIntent = new Intent(DeviceControlActivity.this, BackgroundService.class);
            	stopService(stopBackgroundIntent);
            }
        }, 1000);
    	}
    
    public void exitActivities(View view) {
    	Intent exitIntent = new Intent(this, MainWithMapActivity.class);
    	exitIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	exitIntent.putExtra("EXIT", true);
    	startActivity(exitIntent);
    }
    
}
