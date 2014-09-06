package com.bloc.bluetooth.le;

import com.bloc.MainWithMapActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

// Handles various events fired by the BluetoothLeService.
public class MyBroadcastReceiver extends BroadcastReceiver {	
    
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
        		"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
        	boolean sendAlertOnBoot = PreferenceManager.getDefaultSharedPreferences(context)
        											   .getBoolean("sendAlertOnBoot", false);
        	if (sendAlertOnBoot) {
        		Intent activityIntent = new Intent(context, MainWithMapActivity.class);
        		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		activityIntent.setAction(BackgroundService.ACTION_SEND_EMERGENCY_ALERT);
        		context.startActivity(activityIntent);
        	}
        }

//        if (DeviceControlActivity.ACTION_USER_DISCONNECT.equals(action)) {      	
//        	userDisconnect = intent.getBooleanExtra("value", false);
//        }
//        else if (BluetoothLeService.ACTION_ACL_DISCONNECTED.equals(action)) {
//            Toast.makeText(context, "DISCONNECTED", Toast.LENGTH_SHORT).show();
//            if (!userDisconnect) {
//            	sendAlert(context);
//            }
//        }
//        else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//        	String extra = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
//            // Toast.makeText(context, intent.getStringExtra(BluetoothLeService.EXTRA_DATA), Toast.LENGTH_SHORT).show();
//        	if (extra.equals("Button")) {
//        		sendAlert(context);
//        	}
//        }
    }
    
    private void sendAlert(Context context) {
    	Intent bgServiceIntent = new Intent(context, BackgroundService.class);
    	bgServiceIntent.setAction(BackgroundService.ACTION_SEND_EMERGENCY_ALERT);
    	context.startService(bgServiceIntent);
    }
};