package com.bloc.bluetooth.le;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F.Op;

import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudBackendMessaging;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F;

public class BackgroundService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		LocationListener {
	
    // Global variables
    LocationClient mLocationClient;
    boolean mUpdatesRequested;
    
    private final static String TAG = BackgroundService.class.getSimpleName();
    
    private Person mSelf;
    private String mPhone;
    private String mAccount;
    private CloudBackendMessaging mBackend;
    private static final Geohasher gh = new Geohasher();
    private Location mCurrLocation;
    
	// TODO: get radius from preferences
    private Double TEMP_RADIUS = 50.0; // meters
    
    private boolean mAlert;
    
    public final static String ACTION_EMERGENCY_ALERT =
            "com.bloc.bluetooth.le.ACTION_EMERGENCY_ALERT";
    
    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
	final CloudCallbackHandler<CloudEntity> updateHandler = new CloudCallbackHandler<CloudEntity>() {
		@Override
		public void onComplete(final CloudEntity result) {
			mSelf = new Person(result);
		}
	};
	
	private static final int WEEK_IN_SECONDS = 604800;
    
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 300;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 30;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    
    @Override
	public void onCreate() {
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use low power
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Set the update interval
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        
        // Start with updates turned off
        mUpdatesRequested = true;
        
        // Start with alert turned off
        mAlert = Boolean.FALSE;
                
        mAccount = DeviceControlActivity.getAccountName();
        mBackend = DeviceControlActivity.getCloudBackend();
        
        // Connect to location client
        mLocationClient.connect();
        
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        // If the intent is from the button, send out an alert
		final String action = intent.getAction();
		if (ACTION_EMERGENCY_ALERT.equals(action)) {		
			// Send out the alert!
			mAlert = Boolean.TRUE;
		
			// Get more accurate and more frequent location fixes
	        // Use high accuracy
	        mLocationRequest.setPriority(
	                LocationRequest.PRIORITY_HIGH_ACCURACY);  
	        
	        // Set the update interval to be faster
	        mLocationRequest.setInterval(UPDATE_INTERVAL / 100);
	        
	        // Set the fastest update interval
	        mLocationRequest.setFastestInterval(FASTEST_INTERVAL / 10);
			
			// Start getting updates
			mLocationClient.requestLocationUpdates(mLocationRequest, this);		
		}
		
		// Continue running until explicitly stopped
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// Test
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
        mCurrLocation = location;
        // Report the new location to the backend
		sendMyLocation(location);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
		
	}

	@Override
	public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        
        // Get phone number
		if (mPhone == null) {
	        TelephonyManager tMgr = (TelephonyManager)BackgroundService.this.getSystemService(Context.TELEPHONY_SERVICE);
	        String phone_number = tMgr.getLine1Number();
			mPhone = phone_number;
		}
		
        // If already requested, start periodic updates
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }	
        
        // Get notified of alerts
        listenForAlerts();      
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
	}
	
	public String locationToString(Location location) {
	    return Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
	}
	
	void listenForAlerts() {
        CloudCallbackHandler<List<CloudEntity>> alertHandler =
        		new CloudCallbackHandler<List<CloudEntity>>() {
            @Override
            public void onComplete(List<CloudEntity> results) {
					for (Person victim : Person.fromEntities(results)) {
						// Don't want alerts from yourself
						if (!victim.getPhone().equals(mPhone)) {
		                    LatLng where = gh.decode(victim.getGeohash());
		                    Location help = new Location("Help");
		                    help.setLatitude(where.latitude);
		                    help.setLongitude(where.longitude);
		                    BigDecimal radius = victim.getRadius();
		                    if (mCurrLocation.distanceTo(help) < radius.floatValue()) {
		                    	// TODO: Create map activity asking for help and updating location
		                        Toast.makeText(getApplicationContext(), "Alert Received", Toast.LENGTH_SHORT).show();
		                        break;
		                    }
						}
                    }                   
            }
        };
        
		CloudQuery cq = new CloudQuery("Person");
		cq.setFilter(F.eq("alert", Boolean.TRUE));
		cq.setScope(Scope.FUTURE);
		cq.setSubscriptionDurationSec(WEEK_IN_SECONDS);
		mBackend.list(cq, alertHandler);
	}

	void sendMyLocation(final Location loc) {
	        // execute the insertion with the handler
	        // query for existing username before inserting
	        if (mSelf == null || mSelf.asEntity().getId() == null) {
	                mBackend.listByProperty("Person", "name", Op.EQ,
	                                mAccount, Order.ASC, 1, Scope.PAST,
	                                new CloudCallbackHandler<List<CloudEntity>>() {
	                                        @Override
	                                        public void onComplete(List<CloudEntity> results) {
	                                                if (results.size() > 0) {
	                                                        mSelf = new Person(results.get(0));
	                                                        mSelf.setGeohash(gh.encode(loc));
	                                                        mSelf.setPhone(mPhone);
	                                                        mSelf.setAlert(mAlert);
	                                                        mSelf.setRadius(TEMP_RADIUS);
	                                                        mBackend.update(mSelf.asEntity(),
	                                                                        updateHandler);
	                                                } else {
	                                                	// TODO: get radius from preferences
	                                                        final Person newGeek = new Person(
	                                                        								mAccount,
	                                                                                        mPhone,
	                                                                                        gh.encode(loc),
	                                                                                        mAlert,
	                                                                                        TEMP_RADIUS
	                                                                                        );
	                                                        mBackend.insert(newGeek.asEntity(),
	                                                                        updateHandler);
	                                                }
	                                        }
	                                });
	        } else {
	                mSelf.setGeohash(gh.encode(loc));
	                mSelf.setPhone(mPhone);
	                mSelf.setAlert(mAlert);
	                mSelf.setRadius(TEMP_RADIUS);
	                mBackend.update(mSelf.asEntity(), updateHandler);
	        }
	}
}