package com.bloc.bluetooth.le;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

// Handles various events fired by the BluetoothLeService.
// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
//                        or notification operations.
public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.e("Called", "Hello");
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_ACL_DISCONNECTED.equals(action)) {
            Toast.makeText(context, "DISCONNECTED", Toast.LENGTH_SHORT).show();
            if (!DeviceControlActivity.userDisconnect) {
            	sendAlert(context);
            }
        }
        else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            Toast.makeText(context, intent.getStringExtra(BluetoothLeService.EXTRA_DATA), Toast.LENGTH_SHORT).show();
        	sendAlert(context);
        }
    }
    
    private void sendAlert(Context context) {
    	Intent bgServiceIntent = new Intent(context, BackgroundService.class);
    	bgServiceIntent.setAction(BackgroundService.ACTION_EMERGENCY_ALERT);
    	context.startService(bgServiceIntent);
    }
};