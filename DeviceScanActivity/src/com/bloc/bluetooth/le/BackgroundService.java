package com.bloc.bluetooth.le;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.bloc.R;
import com.bloc.samaritan.map.MapActivity;
import com.bloc.settings.contacts.Contact;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusClient.Builder;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.CloudQuery.Scope;
import com.google.cloud.backend.android.F.Op;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Contacts.People;
import android.provider.ContactsContract.Profile;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
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
    public static boolean isRunning = false;
    
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
    private BroadcastReceiver mScreenReceiver;
    private ArrayList<String> sentGCMs = new ArrayList<String>();
    
    public static int mRadius; // meters
    
    private boolean mAlert;
    private String mRegId;
    
	public static String mPhotoUri;

    // If we are using fastLocationRequest without having received an alert
    private boolean fastWithoutAlert = false; 
    
    public final static int ALERT_NOTIFY_ID = 100;
    
    public final static String ACTION_INIT =
            "com.bloc.bluetooth.le.ACTION_INIT";
    
    public final static String ACTION_STOP_ALERT =
            "com.bloc.bluetooth.le.ACTION_STOP_ALERT";
    
    public final static String ACTION_SEND_EMERGENCY_ALERT =
            "com.bloc.bluetooth.le.ACTION_SEND_EMERGENCY_ALERT";
    
    public final static String ACTION_RECEIVE_EMERGENCY_ALERT =
            "com.bloc.bluetooth.le.ACTION_RECEIVE_EMERGENCY_ALERT";
    
    public final static String ACTION_BACKEND =
            "com.bloc.bluetooth.le.ACTION_BACKEND";
    
    public final static String ACTION_UPDATE_MAP =
            "com.bloc.bluetooth.le.ACTION_UPDATE_MAP";
    
    public final static String ACTION_GET_LOC =
            "com.bloc.bluetooth.le.ACTION_GET_LOC";
    
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
	protected boolean mIntentInProgress;
	
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
    
    private Handler backgroundServiceHandler = new Handler();
   
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
        mRadius = prefs.getInt(DeviceControlActivity.KEY_RADIUS, 2000); // default (meters)
    }
	
	public class ScreenReceiver extends BroadcastReceiver {	 
		private Handler mHandler = new Handler();
		private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
            	Log.e(TAG, "nevermind");
            	PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("sendAlertOnBoot", false).commit();
            }
        };
		
		@Override
	    public void onReceive(final Context context, Intent intent) {
			
	        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            	Log.e(TAG, "alrt");
	        	PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("sendAlertOnBoot", true).commit();
	            mHandler.removeCallbacks(mRunnable);
	            mHandler.postDelayed(mRunnable, 15000);
	        }
	    }
	}
	
	@Override
	public void onCreate() {
		super.onCreate();         
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, filter);
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
	        Notification note = new NotificationCompat.Builder(this)
								        .setContentTitle("Bloc")
								        .build();
	        // Keep this service in the foreground
	        startForeground(42, note);
		}
		else if (ACTION_RECEIVE_EMERGENCY_ALERT.equals(action)) {
			final String blocID = intent.getStringExtra("blocID");
			final boolean contactAlert = intent.getBooleanExtra("contactAlert", false);
	        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
	            @Override
	            protected Void doInBackground(Void... params) {
	                try {
	    				receiveAlert(blocID, contactAlert);
	                } catch (IOException e) {
	                    Log.e(TAG, e.toString());
	                } 
	                return null;
	            }
	        };
	        task.execute();
		}
		else if (ACTION_SEND_EMERGENCY_ALERT.equals(action)) {
			sendAlert();
		}
		else if (ACTION_GET_LOC.equals(action)) {
	        mLocationClient.requestLocationUpdates(fastLocationRequest, this);
	        fastWithoutAlert = true;
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
        unregisterReceiver(mScreenReceiver);		
	}
	
	@Override
	public void onLocationChanged(Location location) {
        // Toast.makeText(this, "Location Changed", Toast.LENGTH_SHORT).show();
		if (fastWithoutAlert) {
        	mLocationClient.removeLocationUpdates(this);
        	mLocationClient.requestLocationUpdates(slowLocationRequest, this);	
        	fastWithoutAlert = false;
        }
        mCurrLocation = location;
        sendMyLocation(mCurrLocation);
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
		
	private void alertEmergencyContacts() {
        final SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String contacts = prefs.getString(DeviceControlActivity.KEY_CONTACTS, null);
        
    	final Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<Contact>>(){}.getType();
        List<Contact> contactList = gson.fromJson(contacts, collectionType);
        if (contactList == null) {
        	return;
        }
        
        final SmsManager sms = SmsManager.getDefault();
        int index = 0;
        for (final Contact contact : contactList) {
        	if (contact.selected) {
				final String phoneNum = String.valueOf(contact.phNum);
	        	if (contact.blocMember) {
			        // Send to bloc users
	        		Log.e("Contact is bloc member", phoneNum);
	    	        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
	    	            @Override
	    	            protected Void doInBackground(Void... params) {
	    	                try {
	    	                	CloudEntity contactEntity = mBackend.get("Person", contact.blocID);
	    		        		String regID = (String) contactEntity.get(Person.KEY_REGISTRATION_ID);
	    		        		// Send alert directly
	    		        		final CloudEntity ce = mBackend.createCloudMessage("ContactAlert");
	    						ce.setId(mSelf.getPhone());
	    						ce.put("blocID", contact.blocID);
	    						ce.put("regID", regID);
	    						sentGCMs.add(ce.getId());
	    						backgroundServiceHandler.post(new Runnable() {
									@Override
									public void run() {
			    						mBackend.sendCloudMessage(ce);
									}
	    						});
	    	                } catch (IOException e) {
	    	                    Log.e(TAG, e.toString());
	    	                } 
	    	                return null;
	    	            }
	    	        };
	    	        task.execute();
	        	}
	        	else {
	        		Log.e("Contact is NOT bloc member", phoneNum);
	        		final int ind = index;
	        		final List<Contact> newContactList = contactList;
					CloudCallbackHandler<List<CloudEntity>> nonBlocSMSHandler =
							new CloudCallbackHandler<List<CloudEntity>>() {
						@Override
						public void onComplete(List<CloudEntity> results) {	
							for (Person emContact : Person.fromEntities(results)) {
								MatchType match = phoneUtil.isNumberMatch(emContact.getPhone(), phoneNum);
								if (match.equals(MatchType.EXACT_MATCH) 
											|| match.equals(MatchType.NSN_MATCH) 
											|| match.equals(MatchType.SHORT_NSN_MATCH)) {
									String entityId = emContact.asEntity().getId();
									Log.e("Contact is bloc member", entityId);
					        		String regID = (String) emContact.asEntity().get(Person.KEY_REGISTRATION_ID);
							        // Send to bloc member
									CloudEntity ce = mBackend.createCloudMessage("ContactAlert");
									ce.setId(mSelf.getPhone());
									ce.put("blocID", contact.blocID);
									ce.put("regID", regID);
									sentGCMs.add(ce.getId());
									mBackend.sendCloudMessage(ce);
									
									// Update this contact as a bloc member
									newContactList.get(ind).blocMember = Boolean.TRUE;
									newContactList.get(ind).blocID = entityId;
							    	SharedPreferences.Editor ed = prefs.edit();
							    	String newContacts = gson.toJson(newContactList);
							    	ed.putString(DeviceControlActivity.KEY_CONTACTS, newContacts);
							    	DeviceControlActivity.mContactList = (ArrayList<Contact>) newContactList;
							        ed.commit();
									
									// Don't send SMS
									return;
								}
							}
							
							// Send to non-bloc member
					        String alert_text;
							if (mCurrLocation != null) {
								alert_text = "EMERGENCY ALERT: I am in danger. "
										+ "Current location: " 
										+ String.valueOf(mCurrLocation.getLatitude()) + ", "
										+ String.valueOf(mCurrLocation.getLongitude());
							}
							else {
								LatLng loc = gh.decode(mSelf.getGeohash());
								alert_text = "EMERGENCY ALERT: I am in danger. "
										+ "Current location: " 
										+ String.valueOf(loc.latitude) + ", "
										+ String.valueOf(loc.longitude);
							}
							sms.sendTextMessage(phoneNum, null, alert_text, null, null);
						}
					};
					CloudQuery cq = new CloudQuery("Person");
					cq.setScope(Scope.PAST);
					mBackend.list(cq, nonBlocSMSHandler);
	        	}	
        	}
        	index++;
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
    	
    	// Wait a little before allowing more alerts
	    new Handler().postDelayed(new Runnable() {
	      @Override
	      public void run() {
	      	isHelping = false;
	      }
	    }, 10000);
    	
    	mBackend.unsubscribeFromQuery("VictimUpdater");
    	
		Log.e(TAG, "END ALERT");
	}
	
	private void sendAlert() {
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
	
	private void receiveAlert(String blocID, boolean contactAlert) throws IOException {
		Person alertSender = new Person(mBackend.get("Person", blocID));
		final String name = alertSender.getName();
		String geohash = alertSender.getGeohash();
		double radius = alertSender.getRadius().doubleValue();
		if (!(name.equals(mAccount) || isHelping || mAlert)) {
			LatLng where = gh.decode(geohash);
            if (mCurrLocation == null) {
            	return;
            }
            Location help = new Location("Help");
            help.setLatitude(where.latitude);
            help.setLongitude(where.longitude);
            if (contactAlert || mCurrLocation.distanceTo(help) < radius) {
            	// Stop listening for alerts
            	isHelping = true;
            	Intent intent = new Intent(BackgroundService.this, MapActivity.class);
            	intent.putExtra(MapActivity.VICTIM_LOC, geohash);
            	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	startActivity(intent);
            	backgroundServiceHandler.post(new Runnable() {
					@Override
					public void run() {
		            	updateVictimLocation(name);						
					}
            	});
            }
		}
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
			mRegId = GCMIntentService.getRegistrationId((Application) getApplicationContext());
            if (mSelf != null) {
				mSelf.setGeohash(gh.encode(loc));
				mSelf.setPhone(mPhone);
				mSelf.setAlert(mAlert);
				mSelf.setRadius(mRadius);
				mSelf.setRegId(mRegId);
				mSelf.setPhotoUri(mPhotoUri);
				mBackend.update(mSelf.asEntity(), updateHandler);
			}
            Intent locIntent = new Intent(DeviceControlActivity.ACTION_LOC_CHANGE);
            locIntent.putExtra("loc", gh.encode(loc));
            sendBroadcast(locIntent);
	}
	
	private void addPersonIfNecessary() {
		// Sets the columns to retrieve for the user profile
		String[] columns = new String[]
		    {
		        Profile.DISPLAY_NAME_PRIMARY,
		    };
		// Retrieves the profile from the Contacts Provider
		Cursor profileCursor = getContentResolver().query(
		        Profile.CONTENT_URI,
		        columns ,
		        null,
		        null,
		        null);
		profileCursor.moveToFirst();
		final String moniker = profileCursor.getString(0);
		
		mRegId = GCMIntentService.getRegistrationId((Application) getApplicationContext());
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
	                                        mSelf.setRegId(mRegId);
	                                        mSelf.setMoniker(moniker);
	                                        mSelf.setPhotoUri(mPhotoUri);
	                                        mBackend.update(mSelf.asEntity(),
	                                                        updateHandler);
	                                } else {
	                                        mSelf = new Person(moniker, mAccount, mPhone, 
	                                        				   "none", mAlert, mRadius, mRegId, mPhotoUri);
	                                        mBackend.insert(mSelf.asEntity(),
	                                                        updateHandler);
	                                }
	                        }
	                });
		}
	}
}