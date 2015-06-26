package com.bloc.bluetooth.le;


import com.bloc.MainWithMapActivity;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

import org.json.JSONException;

public class PebbleReceiver extends BroadcastReceiver {
    private final static String TAG = PebbleReceiver.class.getSimpleName();

    public static final UUID PEBBLE_APP_UUID = UUID.fromString("33e684ef-0831-499f-8a3e-ab9ebce79f82");
    /**
     * Intent broadcast from pebble.apk containing one-or-more key-value pairs sent from the watch to the phone.
     */
    public static final String INTENT_APP_RECEIVE = "com.getpebble.action.app.RECEIVE";
    /**
     * The bundle-key used to store a message's UUID.
     */
    public static final String APP_UUID = "uuid";
    /**
     * The bundle-key used to store a message's transaction id.
     */
    public static final String TRANSACTION_ID = "transaction_id";
    /**
     * The bundle-key used to store a message's JSON payload send-to or received-from the watch.
     */
    public static final String MSG_DATA = "msg_data";
    
	@Override
	public void onReceive(Context context, Intent intent) {
	   if (intent.getAction().equals(INTENT_APP_RECEIVE)) {
	        final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);
	            
	        // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
	        if (!PEBBLE_APP_UUID.equals(receivedUuid)) {
            	Log.e(TAG, String.format("Received UUID: {0}, My UUID: {1}", receivedUuid, PEBBLE_APP_UUID));
	            return;
	        }
	            
	        final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
	        final String jsonData = intent.getStringExtra(MSG_DATA);
	        if (jsonData == null || jsonData.isEmpty()) {
	        	Log.e(TAG, "jsonData is null or empty");
	        	sendAlert(context); // send anyway, we have no other messages from the app yet
	            return;
	        }
	            
	        try {
	            // don't care about the data yet
	            @SuppressWarnings("unused")
				final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
	            sendAlert(context);
	            PebbleKit.sendAckToPebble(context, transactionId);
	        } catch (JSONException e) {
	            sendAlert(context); // send anyway, we have no other messages from the app yet
	        }
	    }
	}
	
    private void sendAlert(Context context) {
    	Log.e(TAG, "sending alert");
    	Intent activityIntent = new Intent(context, MainWithMapActivity.class);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activityIntent.setAction(BackgroundService.ACTION_SEND_EMERGENCY_ALERT);
		context.startActivity(activityIntent);
    }
}
