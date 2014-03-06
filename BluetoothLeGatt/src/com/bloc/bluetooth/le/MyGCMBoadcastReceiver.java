package com.bloc.bluetooth.le;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

public class MyGCMBoadcastReceiver extends GCMBroadcastReceiver {
	protected String getGCMIntentServiceClassName(Context context) {
		return "com.bloc.bluetooth.le.GCMIntentService";
	}
}