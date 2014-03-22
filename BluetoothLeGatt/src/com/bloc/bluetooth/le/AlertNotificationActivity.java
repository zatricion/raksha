package com.bloc.bluetooth.le;

import com.bloc.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class AlertNotificationActivity extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new AlertDialog.Builder(this)
	    			   .setTitle("Cancel Emergency Alert")
	    			   .setMessage("Are you sure you want to cancel your emergency?")
	    			   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    				   public void onClick(DialogInterface dialog, int which) { 
	    				   		requestPassword();
	    				   }
	    			   })
	    			   .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	    				   public void onClick(DialogInterface dialog, int which) { 
	    					   finish();
	    				   }
	    			   })
	    			   .setIcon(R.drawable.ic_launcher)
	    			   .show();
	}
	
	private void requestPassword() {
		//TODO: Ask for PIN and require real and duress PINs during setup of Rakshak
		stopAlert();
		finish();
	}
	
	private void stopAlert() {
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(BackgroundService.ALERT_NOTIFY_ID);
		
		Intent stopAlertIntent = new Intent(this, BackgroundService.class);
		stopAlertIntent.setAction(BackgroundService.ACTION_STOP_ALERT);
		startService(stopAlertIntent);
	}
}
