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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
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
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudBackendMessaging;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.bloc.R;
import com.bloc.settings.contacts.Contact;
import com.bloc.settings.contacts.ContactPickerDialog;
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
    
    public static final String KEY_CONTACTS = "contacts";
       
    public static ArrayList<Contact> mContactList;
    
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    
    private boolean noDevice;
    
    private LocationManager manager;

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private BackgroundService mBackgroundService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            // This call to initialize sets the Bluetooth Adapter
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
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
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Enable button notifications
                enableNotifications(mBluetoothLeService.getButtonService());
            } 
            else if (BluetoothLeService.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // Let the user know that the device is bonded
                Toast.makeText(context, "Device Paired", Toast.LENGTH_SHORT).show();
            } 
            else if (BluetoothLeService.PAIRING_REQUEST.equals(action)) {
                // TODO: add a pin when we have our own prototype (or find out SensorTag pin)
            	BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            	String pin = "";
            	//device.setPin(pin.getBytes());
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
	        setContentView(R.layout.gatt_services_characteristics);
	
	        if (mDeviceName == null) {
	        	mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
	        }
	        
	        if (mDeviceAddress == null) {
	        	mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
	        }
	        	        
	        mConnectionState = (TextView) findViewById(R.id.connection_state);

	        noDevice = intent.getBooleanExtra("noDevice", false);

	        // Device already connected
	        if (BluetoothLeService.isRunning && !noDevice) {
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
    	if (mDeviceAddress != null) {
	        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);	        
	        bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
	        startService(gattServiceIntent);
    	}
    	
        // Make sure location is enabled
    	checkLocationEnabled();
        
    	// Start background service
        if (checkGooglePlayApk() && !BackgroundService.isRunning) {
        	// Enable location tracking
        	Intent bgServiceIntent = new Intent(this, BackgroundService.class);
        	bgServiceIntent.setAction(BackgroundService.ACTION_INIT);
        	startService(bgServiceIntent);    
        }
        
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
		dlg.show(getFragmentManager(), "contacts");
	}

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        
        // Check for GPS
        checkLocationEnabled();
        
        // Check for bluetooth
        if (mBluetoothLeService != null && mDeviceAddress != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
    
    // Check for Google Play (location service)	
    private boolean checkGooglePlayApk() {
        int isAvailable = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
                    this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "Connect Connect to Maps", Toast.LENGTH_SHORT)
                    .show();
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
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
        	        bindService(gattServiceIntent, mServiceConnection, BIND_ABOVE_CLIENT);
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
    
    private void enableNotifications(BluetoothGattService gattService) {
        if (gattService == null) return;
        
        // Get button characteristic
        BluetoothGattCharacteristic button = 
        		gattService.getCharacteristic(BluetoothLeService.UUID_BUTTON_CHAR);
        
        final int charaProp = button.getProperties();   
        // Enable notifications for button characteristic
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = button;
            mBluetoothLeService.setCharacteristicNotification(button, true);
        }
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
