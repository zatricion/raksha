package com.bloc.bluetooth.le;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bloc.R;
import com.bloc.samaritan.map.MapActivity;
import com.bloc.settings.contacts.Contact;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F.Op;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.cloud.backend.android.CloudBackendMessaging;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery;
import com.google.cloud.backend.android.CloudQuery.Order;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType;

public class BackgroundService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		LocationListener {
	
    // Global variables
    LocationClient mLocationClient;
    static boolean isRunning = false;
    
    // Local variables
    private final static String TAG = BackgroundService.class.getSimpleName();
    
    private Person mSelf;
    private String mPhone;
    private String mAccount;
    private static CloudBackendMessaging mBackend;
    private static final Geohasher gh = new Geohasher();
    private static PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private Location mCurrLocation;
    private boolean isHelping;
    private ArrayList<String> sentGCMs = new ArrayList<String>();
    
    public static int mRadius; // meters
    
    private boolean mAlert;
    
    // If we are using fastLocationRequest without having received an alert
    private boolean fastWithoutAlert = false; 
    
    public final static int ALERT_NOTIFY_ID = 100;
    
    public final static String ACTION_INIT =
            "com.bloc.bluetooth.le.ACTION_INIT";
    
    public final static String ACTION_STOP_ALERT =
            "com.bloc.bluetooth.le.ACTION_STOP_ALERT";
    
    public final static String ACTION_EMERGENCY_ALERT =
            "com.bloc.bluetooth.le.ACTION_EMERGENCY_ALERT";
    
    public final static String ACTION_BACKEND =
            "com.bloc.bluetooth.le.ACTION_BACKEND";
    
    public final static String ACTION_UPDATE_MAP =
            "com.bloc.bluetooth.le.ACTION_UPDATE_MAP";
    
    public final static String ACTION_END_ALERT =
            "com.bloc.bluetooth.le.ACTION_END_ALERT";
    
    // Location request for slow, battery saving updates
    LocationRequest slowLocationRequest;
    
    // Location request for fast, accurate updates
    LocationRequest fastLocationRequest;
    
	final CloudCallbackHandler<CloudEntity> updateHandler = new CloudCallbackHandler<CloudEntity>() {
		@Override
		public void onComplete(final CloudEntity result) {
			mSelf = new Person(result);
		}
	};
	
	private static final int WEEK_IN_SECONDS = 604800;
	private static final int HOUR_IN_SECONDS = 3600;
    
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
    
   
	private void initialize() {
        // Set up slowLocationRequest
        slowLocationRequest = LocationRequest.create();
        // Use low power
        slowLocationRequest.setPriority(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Set the update interval
        slowLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval
        slowLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        // Set up fastLocationRequest
        fastLocationRequest = LocationRequest.create();
		// Get more accurate and more frequent location fixes
        // Use high accuracy
        fastLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);  
        
        // Set the update interval to be faster
        fastLocationRequest.setInterval(UPDATE_INTERVAL / 50);
        
        // Set the fastest update interval
        fastLocationRequest.setFastestInterval(FASTEST_INTERVAL / 5);
        
        // Start with alert turned off
        mAlert = Boolean.FALSE;
        
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */        
		if (mLocationClient == null) {
            mLocationClient = new LocationClient(this, this, this);
		}
		
        // Connect to location client
        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting())
        {
        	mLocationClient.connect();
        }
        
        // The Service is running
        isRunning = true;
        
        // Not yet helping anyone
        isHelping = false;
        
        // Get radius
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        mRadius = prefs.getInt(DeviceControlActivity.KEY_RADIUS, 50); // default (meters)
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
        if (mAccount == null) {
        	mAccount = ((BlocApplication) this.getApplication()).getAccountName();
        }
        
        if (mBackend == null) {
        	mBackend = ((BlocApplication) this.getApplication()).getBackend();
        }
        
        // Set up mSelf
        addPersonIfNecessary();
        
		final String action = intent.getAction();
		
		if (ACTION_STOP_ALERT.equals(action)) {
			cancelAlert();
			mAlert = Boolean.FALSE;
			if (mCurrLocation != null) {
				sendMyLocation(mCurrLocation);
			}
			else if (mSelf != null) {
				mSelf.setAlert(mAlert);
                mBackend.update(mSelf.asEntity(),
                        updateHandler);
			}
			// Stop getting fast updates
	        if (mLocationClient.isConnected()) {
	        	mLocationClient.removeLocationUpdates(this);
	        	mLocationClient.requestLocationUpdates(slowLocationRequest, this);	
	        }
		}
		else if (ACTION_INIT.equals(action)) {
			// Set up the service
			initialize();
			
			// TODO: better notification
	        Notification note = new Notification.Builder(this)
								        .setContentTitle("SensorTag")
								        .build();
	        // Keep this service in the foreground
	        startForeground(42, note);
		}
        // If the intent is from the button, send out an alert
		else if (ACTION_EMERGENCY_ALERT.equals(action)) {
			// Notify the user about the alert
			createAlertNotification();
			
			// Get radius
	        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
	        mRadius = prefs.getInt(DeviceControlActivity.KEY_RADIUS, mRadius); // default (meters)
			
			// Send out the alert!
			mAlert = Boolean.TRUE;
			if (mCurrLocation != null) {
		        Toast.makeText(this, "Sending Alert", Toast.LENGTH_SHORT).show();
				sendMyLocation(mCurrLocation);
			}
			else if (mSelf != null) {
		        Toast.makeText(this, "Sending Alert without updated Location", Toast.LENGTH_SHORT).show();
				mSelf.setAlert(mAlert);
                mBackend.update(mSelf.asEntity(),
                        updateHandler);
			}
			
			// Alert emergency contacts
			alertEmergencyContacts();
	
			// Start getting fast updates
	        if (mLocationClient.isConnected()) {
	        	mLocationClient.removeLocationUpdates(this);
	        	mLocationClient.requestLocationUpdates(fastLocationRequest, this);	
	        }
		}
        
		return START_NOT_STICKY;
	}
	
	private void createAlertNotification() {
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("Emergency!")
		        .setContentText("Was this emergency alert an accident?");
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, AlertNotificationActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(AlertNotificationActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_CANCEL_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(ALERT_NOTIFY_ID, mBuilder.build());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isRunning = false;
		mLocationClient.disconnect();	
		//mBackend.clearAllSubscription();
	}
	
	@Override
	public void onLocationChanged(Location location) {
        Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
		if (fastWithoutAlert) {
        	mLocationClient.removeLocationUpdates(this);
        	mLocationClient.requestLocationUpdates(slowLocationRequest, this);	
        	fastWithoutAlert = false;
        }
        mCurrLocation = location;
        sendMyLocation(mCurrLocation);	
        
        //TODO: Not sure whether this is a good idea
		// Listen for alerts from people who have you as an emergency contact
		listenForContactAlerts();
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
        // Get phone number
		if (mPhone == null) {
	        TelephonyManager tMgr = (TelephonyManager)BackgroundService.this.getSystemService(Context.TELEPHONY_SERVICE);
	        String phone_number = tMgr.getLine1Number();
	        if (phone_number != null) {
	        	mPhone = phone_number;
	        }
	        else if (mSelf != null) {
	        	mPhone = mSelf.getPhone();
	        }
	        else {
//	        	mPhone =  UUID.randomUUID().toString();
//	        	Log.e(TAG, "Using fake phone number");
	        	mPhone = "18646508209";
	        }
		}
		
		// Start with last known location
		mCurrLocation = mLocationClient.getLastLocation();
		
		// Get new location quickly if there is no saved location
		if (mCurrLocation == null) {
	        mLocationClient.requestLocationUpdates(fastLocationRequest, this);
	        fastWithoutAlert = true;
		}
		else {
	        // Start periodic updates
	        mLocationClient.requestLocationUpdates(slowLocationRequest, this);
		}
		
		// If alert registered, send it out
		if (mAlert) {
	        Toast.makeText(this, "Sending Alert", Toast.LENGTH_SHORT).show();
		}
		
        // Report the new location to the backend
		if (mCurrLocation != null) {
			sendMyLocation(mCurrLocation);
		}
		     
        // Get notified of alerts
        listenForAlerts();    
        
		// Listen for alerts from people who have you as an emergency contact
		listenForContactAlerts();
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onTaskRemoved(Intent intent) {
    	Intent getBackendIntent = new Intent(this, DeviceControlActivity.class);
    	getBackendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	getBackendIntent.setAction(ACTION_BACKEND);
    	startActivity(getBackendIntent);
	}
	
	private void listenForContactAlerts() {
		Log.e(TAG, mPhone);
		CloudCallbackHandler<List<CloudEntity>> contactAlertHandler =
				new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> messages) {
				Log.e(TAG, "message");
				for (CloudEntity gcm : messages) {
					MatchType match = phoneUtil.isNumberMatch((String) gcm.get("recipient"), mPhone);
					Log.e(TAG, match.name());
					if (match.equals(MatchType.EXACT_MATCH) 
							|| match.equals(MatchType.NSN_MATCH) 
							|| match.equals(MatchType.SHORT_NSN_MATCH)) {
					 	if (!(isHelping || mAlert)) {
					 		Log.e(TAG, "Got contact alert");
					 		isHelping = true;
							// Get info from message
							String name = (String) gcm.get("name");
							String geohash = (String) gcm.get("location");
							
							// Start MapActivity
							Intent intent = new Intent(BackgroundService.this,
									MapActivity.class);
							intent.putExtra(MapActivity.VICTIM_LOC, geohash);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
							
							updateVictimLocation(name);
							break;
						}
					}
				}
			}
			
			@Override
			public void onError(IOException exception) {
				Log.e(TAG, exception.toString());
			}
		};
				  
		mBackend.subscribeToCloudMessage("ContactAlert", contactAlertHandler);	
	}
	
	private void alertEmergencyContacts() {
        SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String contacts = prefs.getString(DeviceControlActivity.KEY_CONTACTS, null);
        
    	Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<Contact>>(){}.getType();
        ArrayList<Contact> contactList = gson.fromJson(contacts, collectionType);

    	for (Contact contact : contactList) {
    		if (contact.selected) {
				CloudEntity ce = mBackend.createCloudMessage("ContactAlert");
				Log.e("Sending alert to", String.valueOf(contact.phNum));
				ce.setId(String.valueOf(contact.phNum));
				ce.put("recipient", String.valueOf(contact.phNum));
				ce.put("name", mAccount);
				ce.put("location", gh.encode(mCurrLocation));
				sentGCMs.add(ce.getId());
				mBackend.sendCloudMessage(ce);
    		}
		}
	}
	
	private void listenForAlertCancellation(final String name) {
		CloudCallbackHandler<List<CloudEntity>> cancellationHandler =
				new CloudCallbackHandler<List<CloudEntity>>() {
			@Override
			public void onComplete(List<CloudEntity> messages) {				
            	endAlert();
			}
		};
				  
		mBackend.subscribeToCloudMessage(name, cancellationHandler);	
	}
	
	private void cancelAlert() {
		CloudEntity ce = mBackend.createCloudMessage(mAccount);
		ce.put("cancel", mAccount);
		mBackend.sendCloudMessage(ce);
		
	 	// Delete sent cloud messages
		new Thread(new Runnable() {
		    public void run() {
		    	for (String id : sentGCMs) {
				 	try {
						mBackend.delete("_CloudMessages", id);;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				sentGCMs.clear();
		    }
		}).start();
	}
	
	private void endAlert() {
    	// End the alert
    	Intent intent = new Intent(ACTION_END_ALERT);
    	sendBroadcast(intent);
    	isHelping = false;
    	
    	mBackend.unsubscribeFromQuery("VictimUpdater");
    	
		Log.e(TAG, "END ALERT");
	}
	
	private void listenForAlerts() {
        CloudCallbackHandler<List<CloudEntity>> alertHandler =
        		new CloudCallbackHandler<List<CloudEntity>>() {
            @Override
            public void onComplete(List<CloudEntity> results) {
            	Log.e("Received", "ALERT");
            	Log.e("Received", results.toString());
				for (Person victim : Person.fromEntities(results)) {
					String name = victim.getName();
					// Don't want alerts from yourself or if you have an emergency
					if (!(name.equals(mAccount) || isHelping || mAlert)) {
	                    LatLng where = gh.decode(victim.getGeohash());
	                    BigDecimal radius = victim.getRadius();
	                    if (where == null || radius == null || mCurrLocation == null) {
	                    	break;
	                    }
	                    Location help = new Location("Help");
	                    help.setLatitude(where.latitude);
	                    help.setLongitude(where.longitude);
	                    if (mCurrLocation.distanceTo(help) < radius.floatValue()) {
	                    	// Stop listening for alerts
	                    	isHelping = true;
	                    	Intent intent = new Intent(BackgroundService.this, MapActivity.class);
	                    	intent.putExtra(MapActivity.VICTIM_LOC, victim.getGeohash());
	                    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                    	startActivity(intent);
	                    	updateVictimLocation(name);
	                        break;
	                    }
                    }
            	}
            }
        };

		CloudQuery cq = new CloudQuery("Person");
		cq.setQueryId("AlertListener");
		cq.setFilter(F.eq(Person.KEY_ALERT, Boolean.TRUE));
		cq.setScope(Scope.FUTURE);
		cq.setSubscriptionDurationSec(WEEK_IN_SECONDS);
		mBackend.list(cq, alertHandler);
	}
	
	private void updateVictimLocation(String name) {
        CloudCallbackHandler<List<CloudEntity>> updateLocationHandler =
        		new CloudCallbackHandler<List<CloudEntity>>() {
            @Override
            public void onComplete(List<CloudEntity> results) {
            	Log.e("Victim", "Update");
				for (Person victim : Person.fromEntities(results)) {
					if (victim.getAlert()) {
	                	Intent intent = new Intent(ACTION_UPDATE_MAP);
	                	intent.putExtra(MapActivity.VICTIM_LOC, victim.getGeohash());
	                	sendBroadcast(intent);
					}
					else {
						endAlert();
						break;
					}
            	}
            }
        };

		CloudQuery cq = new CloudQuery("Person");
		cq.setQueryId("VictimUpdater");
		cq.setFilter(F.eq(Person.KEY_NAME, name));
		cq.setScope(Scope.FUTURE);
		cq.setSubscriptionDurationSec(HOUR_IN_SECONDS);
		mBackend.list(cq, updateLocationHandler);
		
		listenForAlertCancellation(name);
	}

	private void sendMyLocation(final Location loc) {
            if (mSelf != null) {
				mSelf.setGeohash(gh.encode(loc));
				mSelf.setPhone(mPhone);
				mSelf.setAlert(mAlert);
				mSelf.setRadius(mRadius);
				mBackend.update(mSelf.asEntity(), updateHandler);
			}
	}
	
	private void addPersonIfNecessary() {
		if (mSelf == null || mSelf.asEntity().getId() == null) {
			// Query backend
	        mBackend.listByProperty("Person", "name", Op.EQ,
	                mAccount, Order.ASC, 1, Scope.PAST,
	                new CloudCallbackHandler<List<CloudEntity>>() {
	                        @Override
	                        public void onComplete(List<CloudEntity> results) {
	                                if (results.size() > 0) {
	                                        mSelf = new Person(results.get(0));
	                                        mSelf.setAlert(mAlert);
	                                        mSelf.setRadius(mRadius);
	                                        mBackend.update(mSelf.asEntity(),
	                                                        updateHandler);
	                                } else {
	                                	// TODO: get radius from preferences
	                                        mSelf = new Person(mAccount, mPhone, 
	                                        				   "none", mAlert, mRadius);
	                                        mBackend.insert(mSelf.asEntity(),
	                                                        updateHandler);
	                                }
	                        }
	                });
		}
	}
}