package com.example.bluetooth.le;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

// Handles various events fired by the BluetoothLeService.
// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
//                        or notification operations.
public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            Toast.makeText(context, "NEW DATA", Toast.LENGTH_SHORT).show();
        }
    }
};