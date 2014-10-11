package com.bloc;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bloc.bluetooth.le.BackgroundService;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.bloc.bluetooth.le.DeviceScanActivity;
import com.bloc.bluetooth.le.Geohasher;
import com.bloc.settings.prefs.AboutDialog;
import com.bloc.settings.prefs.RadiusPickerDialog;
import com.bloc.settings.prefs.SettingsDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.cloud.backend.android.CloudBackendActivity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import android.animation.ObjectAnimator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.os.SystemClock;

public class MainWithMapActivity extends DeviceControlActivity {
  private GoogleMap map;
  private LocationManager locationManager;
  private String provider;
  private LatLng curLatLng;
  private static int M2LAT = 111111;
  private float ringFrameWeightRatio = 3f/5f; //Hardcoded in the GUI
  private float markerDisplayAdj = 4f/3f; //Hardcoded change!!
  private Bitmap icon;
  private Marker you;
  private static float progressBarTimeMillis = 2000;
  final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private final static int SCAN_REQUEST = 8000;
  private AsyncTask<Void, Float, Void> progressBarUpdateTask;
  private static final Geohasher gh = new Geohasher();
  protected static final int REQUEST_CODE_RESOLVE_ERR = 1992;
  private TextView deviceStatusTV;
  private ProgressBar progressBar;
  private Timer sendAlertTimer;
  private boolean sendAlertRightAway;
  private GoogleApiClient mPlusClient;
  private TextView zeroRadiusTV;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    String action = intent.getAction();
    sendAlertRightAway = false;

    if (action != null && action.equals(BackgroundService.ACTION_SEND_EMERGENCY_ALERT)) {
    	sendAlertRightAway = true;
    }

    setContentView(R.layout.fragment_main_with_map);
    deviceStatusTV = (TextView) findViewById(R.id.text_view_status);

    // To create the ring
    LinearLayout ringLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_ring);
    final ImageView ringImageView = (ImageView) findViewById(R.id.image_view_ring);
    
    ringLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    
    if (curLatLng == null) {
        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
	        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
	    if (location != null) {
	      curLatLng = new LatLng(location.getLatitude(), location.getLongitude());
	    } else {
	      Log.e("Error", "location not obtained");
	    }
    }
    setUpMapIfNeeded();
    


    ringImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			int width = ringImageView.getMeasuredWidth();
			int height = ringImageView.getMeasuredHeight();
			icon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		    int desiredAlpha = 0xAF000000;
		    for(int i = 0; i < width; i++)
		    {
		    	int cI = i - width / 2;
		    	
		        for(int j = 0; j < height; j++)
		        {
		        	int cJ = j - height / 2;
		        	float radius = Math.min(width, height);
		            if (cI*cI + cJ*cJ >=  (radius*radius) / 4) {
		            		 icon.setPixel(i, j, desiredAlpha);
		            }		 
		        }
		    }
		    ringImageView.setImageBitmap(icon);
		    
		}
    });
    
    progressBar = (ProgressBar) findViewById(R.id.progress_ring);
    progressBar.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			int width = ringImageView.getMeasuredWidth();
			int height = ringImageView.getMeasuredHeight();
			progressBar.setPivotX(width/2);
			progressBar.setPivotY(height/2);
			progressBar.setRotation(270f);
		}
    });

    ringImageView.setOnTouchListener(new View.OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				progressBar.setVisibility(View.VISIBLE);
			    ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
			    animation.setDuration(1500);
			    animation.setInterpolator(new LinearInterpolator());
			    animation.start();
			    
			    sendAlertTimer = new Timer("alertTimer", true);
			    
			    final TimerTask sendAlertTimerTask = new TimerTask(){
					@Override
					public void run() {
				    	Intent bgServiceIntent = new Intent(getApplicationContext(), BackgroundService.class);
				    	bgServiceIntent.setAction(BackgroundService.ACTION_SEND_EMERGENCY_ALERT);
				    	startService(bgServiceIntent);
					}
			    };
			    sendAlertTimer.schedule(sendAlertTimerTask, (long) 1500f);
			    Log.i("karan setOnTouchListener","schedule after");
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				sendAlertTimer.cancel();
				sendAlertTimer = null;
				progressBar.setVisibility(View.INVISIBLE);
			}
			return true;
		}
    });     
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
	if (intent.getBooleanExtra("EXIT", false)) {
	    finish();
	    return;
	}
  }
  
  @Override
  protected void onPause() {
      super.onPause();
      unregisterReceiver(mapUpdateReceiver);
  }
  
  @Override
  protected void onResume() {
      super.onResume();
      // Check for availability of GooglePlay Services needs to be added. explained in google Location API v2 doc    
      checkGooglePlayApk();
      
      // Set up the map
      setUpMapIfNeeded();
      
      if (BackgroundService.isRunning) {
	      Intent bgServiceIntent = new Intent(this, BackgroundService.class);
	      bgServiceIntent.setAction(BackgroundService.ACTION_GET_LOC);
	      startService(bgServiceIntent);
      }
      
	  if (sendAlertRightAway) {
	    	Intent bgServiceIntent = new Intent(this, BackgroundService.class);
	    	bgServiceIntent.setAction(BackgroundService.ACTION_SEND_EMERGENCY_ALERT);
	    	startService(bgServiceIntent);
	    	sendAlertRightAway = false;
	  }
	  
      
		mPlusClient = new GoogleApiClient.Builder(this)
								.addApi(Plus.API)
								.addScope(Plus.SCOPE_PLUS_PROFILE)
								.build();
		mPlusClient.registerConnectionCallbacks(new ConnectionCallbacks() {
			@Override
			public void onConnected(Bundle arg0) {
				// Get photo uri from G+
				if (mPlusClient.isConnected()) {
					Person user = Plus.PeopleApi.getCurrentPerson(mPlusClient);
					BackgroundService.mPhotoUri = user.getImage().getUrl();
					BackgroundService.userMoniker = user.getName().getGivenName() + " " + user.getName().getFamilyName();
					mPlusClient.disconnect();
				}
				else if (mPlusClient.isConnecting()) {
					// Wait a second, then get photo uri
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							if (mPlusClient.isConnected()) {
								Person user = Plus.PeopleApi.getCurrentPerson(mPlusClient);
								BackgroundService.mPhotoUri = user.getImage().getUrl();
								BackgroundService.userMoniker = user.getName().getGivenName() + " " + user.getName().getFamilyName();
								mPlusClient.disconnect();
							}
						}
					}, 1000);
				}
			}

			@Override
			public void onConnectionSuspended(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		mPlusClient.registerConnectionFailedListener(new OnConnectionFailedListener() {

			@Override
			public void onConnectionFailed(ConnectionResult result) {
			    if (result.hasResolution()) {
			        try {
			            result.startResolutionForResult(MainWithMapActivity.this, REQUEST_CODE_RESOLVE_ERR);
			        } catch (SendIntentException e) {
			            mPlusClient.connect();
			        }
			    }
			}
		});

		mPlusClient.connect();
		
	  // If radius is 0, explain to the user that only EC's get notified
      mRadius = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
	  								.getInt(KEY_RADIUS, 2000);
      if (mRadius == 0) {
    	  showZeroRadiusView();
      }
            
      // Register radius and location change receiver
      IntentFilter changeFilter = new IntentFilter();
      changeFilter.addAction(ACTION_RADIUS_CHANGE);
      changeFilter.addAction(ACTION_LOC_CHANGE);
      registerReceiver(mapUpdateReceiver, changeFilter);
  }
  
  private void showZeroRadiusView() {
	  if (zeroRadiusTV == null) {
		  DisplayMetrics displaymetrics = new DisplayMetrics();
		  getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		  int height = displaymetrics.heightPixels;
		  View topLayout = findViewById(R.id.frame1);
		  zeroRadiusTV = new TextView(this);
		  zeroRadiusTV.setText("Your radius is set to zero, so only your emergency contacts will receive alerts");
		  zeroRadiusTV.setGravity(Gravity.CENTER);
		  zeroRadiusTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		  zeroRadiusTV.setTextColor(getResources().getColor(R.color.bloc_color));
		  zeroRadiusTV.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) (height/1.7)));
	      ((ViewGroup) topLayout).addView(zeroRadiusTV);
	  }
  }
  
  private void removeZeroRadiusView() {
	  if (zeroRadiusTV != null) {
    	  View topLayout = findViewById(R.id.frame1);
          ((ViewGroup) topLayout).removeView(zeroRadiusTV);    
          zeroRadiusTV = null;
	  }
  }

  private void setUpMapIfNeeded() {
      // Do a null check to confirm that we have not already instantiated the map.
      if (map == null) {
          // Try to obtain the map from the SupportMapFragment.
          map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map1))
                  .getMap();
          // Check if we were successful in obtaining the map.
          if (map != null) {
              setUpMap();
          }
      }
  }
  private void setUpMap() {
      // Enables/disables zoom gestures (i.e., double tap, pinch & stretch).
      map.getUiSettings().setZoomGesturesEnabled(false);
      // Hide the zoom controls as the button panel will cover it.
      map.getUiSettings().setZoomControlsEnabled(false);
      //Enables/disables scroll gestures (i.e. panning the map).
      map.getUiSettings().setScrollGesturesEnabled(false);
      // Enables/disables the compass (icon in the top left that indicates the orientation of the
      // map).
      map.getUiSettings().setCompassEnabled(false);


      // Cannot zoom to bounds until the map has a size.
      final View mapView = getSupportFragmentManager().findFragmentById(R.id.map1).getView();
      if (mapView.getViewTreeObserver().isAlive()) {
          mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
            	  // Set radius of visible map to Neighborhood radius
                  mRadius = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                		  			.getInt(KEY_RADIUS, 2000);
                  
                  updateMap();
                  
                  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                  } 
              }
          });
      }
      
      // Credit to http://stackoverflow.com/questions/14497734/dont-snap-to-marker-after-click-in-android-map-v2
      // Don't center marker when clicked
      map.setOnMarkerClickListener(new OnMarkerClickListener() {
    	  public boolean onMarkerClick(Marker marker) {
    		  // Open the info window for the marker
    		  marker.showInfoWindow();
		
    		  // Event was handled by our code do not launch default behaviour.
    		  return true;
    	  }
      });
  }
	
    // Check for Google Play (location service)	
    private void checkGooglePlayApk() {
        int isAvailable = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
                    this, PLAY_SERVICES_RESOLUTION_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "Google Play Services unavailable", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
        return;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode) {
        case PLAY_SERVICES_RESOLUTION_REQUEST:
          if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Google Play Services must be installed.",
                Toast.LENGTH_SHORT).show();
            finish();
          }
          else if (resultCode == RESULT_OK) {
        	  return;
          }
      }
      super.onActivityResult(requestCode, resultCode, data);
    }    
    
    public void settings(View v) {
    	SettingsDialog dlg = new SettingsDialog();
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getSupportFragmentManager(), "settings");
    }
    
    public void showAbout(View v) {
    	AboutDialog dlg = new AboutDialog();
    	dlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		dlg.show(getSupportFragmentManager(), "about");
    }
	
	private void updateMap() {
		// Set radius of visible map to Neighborhood radius
	    mRadius = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
	  		  			.getInt(KEY_RADIUS, 2000);
	    
	    if (curLatLng == null) {
	        // Get the location manager
	        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

	        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        if (location == null) {
		        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	        }
		    if (location != null) {
		      curLatLng = new LatLng(location.getLatitude(), location.getLongitude());
		    } else {
		      Log.e("MainWithMapActivity", "location not obtained");
		      return;
		    }
	    }
	    
		LatLngBounds bounds = new LatLngBounds.Builder()
		      .include(new LatLng(curLatLng.latitude, curLatLng.longitude - 
									(mRadius / (M2LAT * Math.cos(Math.toRadians(curLatLng.latitude))))))
			  .include(new LatLng(curLatLng.latitude, curLatLng.longitude + 
			  						(mRadius / (M2LAT * Math.cos(Math.toRadians(curLatLng.latitude))))))
			  .include(new LatLng(curLatLng.latitude + mRadius / M2LAT, curLatLng.longitude))
			  .include(new LatLng(curLatLng.latitude - (markerDisplayAdj * mRadius) / M2LAT, curLatLng.longitude))
		      .build();
		
		// Add marker to the map.
		map.clear();
		if (mRadius != 0) {
			you = map.addMarker(new MarkerOptions()
					  						.position(curLatLng)
					  						.title("You"));
			you.setPosition(curLatLng);
		}
		
		map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
	}
	
    private final BroadcastReceiver mapUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_RADIUS_CHANGE.equals(action)) {
            	if (mRadius == 0) {
            		showZeroRadiusView();
            	} else {
            		removeZeroRadiusView();
            	}
            	if (curLatLng != null) {
	            	setUpMapIfNeeded();
	                updateMap();
            	}
            } else if (ACTION_LOC_CHANGE.equals(action)) {
            	setUpMapIfNeeded();
            	curLatLng = gh.decode(intent.getStringExtra("loc"));
        		updateMap();
            }
        }
    };
}