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
    public static final String ACTION_KILL_RECEIVER = "com.bloc.bluetooth.le.ACTION_KILL_RECEIVER";
    
    public static final String KEY_CONTACTS = "contacts";
    public static final String KEY_RADIUS = "radius";
       
    public static ArrayList<Contact> mContactList;
    public static int mRadius;
        
    private boolean noDevice;
    public static boolean isBLeServiceBound;
    
    private LocationManager manager;

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            // This call to initialize sets the Bluetooth Adapter
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
                return;
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            isBLeServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            isBLeServiceBound = false;
        }
    };

    // Handles various events fired by the BluetoothLeService.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } 
            else if (BluetoothLeService.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Let the user know that the device is bonded
                Toast.makeText(context, "Device Paired", Toast.LENGTH_SHORT).show();
            } 
            else if (BluetoothLeService.PAIRING_REQUEST.equals(action)) {
                // TODO: add a pin when we have our own prototype (or find out SensorTag pin)
//            	BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
//            	String pin = "";
//            	device.setPin(pin.getBytes());
            } 
        }
    };
    
    private void setUserDisconnect(boolean bool) {
    	 final Intent intent = new Intent(ACTION_USER_DISCONNECT);
    	 intent.putExtra("value", bool);
    	 sendBroadcast(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final Intent intent = getIntent();
        final String action = intent.getAction();
        
        // If user closes the app task, this is called in order to set the global backend up again
        if (action != null && action.equals(BackgroundService.ACTION_BACKEND)) {
        	((BlocApplication) this.getApplication()).setAccountName(getAccountName());
        	((BlocApplication) this.getApplication()).setBackend(getCloudBackend());
        	finish();
        }
        else {
	        setContentView(R.layout.device_control);
	
	        if (mDeviceName == null) {
	        	mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
	        }
	        
	        if (mDeviceAddress == null) {
	        	mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
	        }
	        	        
	        mConnectionState = (TextView) findViewById(R.id.connection_state);

	        noDevice = intent.getBooleanExtra("noDevice", false);

	        // Device already connected
	        if (!noDevice && BluetoothLeService.isRunning) {
	        	mDeviceAddress = BluetoothLeService.mBluetoothDeviceAddress;
	        	updateConnectionState(R.string.connected);
	        	mConnected = true;
	        }
	
	        if (mDeviceAddress != null) {
		        // Sets up UI references.
		        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
	        }
	        	        
	        getActionBar().setTitle(mDeviceName);
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        
	        setUserDisconnect(false);
        }
        // Call this last so that onPostCreate is called after device address obtained
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public void onPostCreate() {
    	super.onPostCreate();
    	
    	((BlocApplication) this.getApplication()).setAccountName(getAccountName());
    	((BlocApplication) this.getApplication()).setBackend(getCloudBackend());
    	
    	// Start bluetooth service
    	if (!noDevice && (mDeviceAddress != null)) {
	        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);	        
	        bindService(gattServiceIntent, mServiceConnection, BIND_IMPORTANT);
	        startService(gattServiceIntent);
    	}
    	
        // Make sure location is enabled
    	checkLocationEnabled();
        
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
        
        if (contacts == null) {
        	showContactPickerDialog();
        }
        else {
        	Gson gson = new Gson();
            Type collectionType = new TypeToken<ArrayList<Contact>>(){}.getType();
            ArrayList<Contact> contact_list = gson.fromJson(contacts, collectionType);
        	mContactList = contact_list;
        }
        
        // Set up radius
        mRadius = prefs.getInt(KEY_RADIUS, -1);
        if (mRadius == -1) {
        	showRadiusPickerDialog();
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
    
    private void showContactPickerDialog() {
		ContactPickerDialog dlg = new ContactPickerDialog();
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getFragmentManager(), "contacts");
	}

    private void showRadiusPickerDialog() {
    	RadiusPickerDialog dlg = new RadiusPickerDialog();
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getFragmentManager(), "radius");
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        
        // Check for GPS
        checkLocationEnabled();
        
        // Check for bluetooth
        if (!noDevice && (mBluetoothLeService != null) && (mDeviceAddress != null)) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBLeServiceBound && (mBluetoothLeService != null)) {
        	unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } 
        else if (noDevice) {
        	 menu.findItem(R.id.menu_connect).setVisible(false);
             menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
            	if (mBluetoothLeService != null && mDeviceAddress != null) {
                    Toast.makeText(this, "Connecting...", Toast.LENGTH_LONG).show();
            		mBluetoothLeService.connect(mDeviceAddress);
            		setUserDisconnect(false);
            	}
            	else if (mDeviceAddress != null) {
                    Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
        	        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);	        
        	        bindService(gattServiceIntent, mServiceConnection, BIND_IMPORTANT);
        	        startService(gattServiceIntent);
            	}
                return true;
            case R.id.menu_disconnect:
            	if (mBluetoothLeService != null) {
            		setUserDisconnect(true);
            		mBluetoothLeService.disconnect();
            	}
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_contacts:
            	showContactPickerDialog();
            	return true;   
            case R.id.action_radius:
            	showRadiusPickerDialog();
            	return true;     
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
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
            	
            	Intent stopBluetoothIntent = new Intent(DeviceControlActivity.this, BluetoothLeService.class);
            	stopService(stopBluetoothIntent);
            	
//        		// Kill the broadcast receiver
//	       	   	final Intent intent = new Intent(ACTION_KILL_RECEIVER);
//	       	   	sendBroadcast(intent);
//       		
            	isBLeServiceBound = false;
            }
        }, 1000);
    	}
    
    public void exitActivities(View view) {
    	setUserDisconnect(true);
    	Intent exitIntent = new Intent(this, BlocActivity.class);
    	exitIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	exitIntent.putExtra("EXIT", true);
    	startActivity(exitIntent);
    	finish();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothLeService.PAIRING_REQUEST);
        return intentFilter;
    }
}
