package com.bloc.bluetooth.le;

import com.google.cloud.backend.android.CloudBackendMessaging;

import android.app.Application;
import android.util.Log;

public class BlocApplication extends Application {
	private static CloudBackendMessaging backend;
	private static String account_name;
	
	public CloudBackendMessaging getBackend() {
	    return backend;
	}
	
	public void setBackend(CloudBackendMessaging backend) {
	    BlocApplication.backend = backend;
	}
	
	public String getAccountName() {
	    return account_name;
	}
	
	public void setAccountName(String account) {
	    BlocApplication.account_name = account;
	}
}
