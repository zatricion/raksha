package com.bloc;

import android.app.Activity;
import android.content.Context;
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
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bloc.bluetooth.le.DeviceControlActivity;
import com.google.android.gms.R.color;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
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
import com.google.cloud.backend.android.CloudBackendActivity;

import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class MainWithMapActivity extends FragmentActivity {
  private GoogleMap map;
  private LocationManager locationManager;
  private String provider;
  private LatLng curLatLng;
  private HoloCircularProgressBar progressBar;
  private int progressStatus = 0;
  private static int M2LAT = 111111;
  private float ringFrameWeightRatio = 3f/5f; //Hardcoded in the GUI
  private float markerDisplayAdj = 7f/3f; //Hardcoded change!!
  private Bitmap icon;
  private static float progressBarTimeMillis = 2000;
  private AsyncTask<Void, Float, Void> progressBarUpdateTask;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fragment_main_with_map);
    
    // To create the ring
    LinearLayout ringLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_ring);
    final ImageView ringImageView = (ImageView) findViewById(R.id.image_view_ring);
    
    ringLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    
    
    // Get the location manager
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Define the criteria how to select the location provider -> use
    Criteria criteria = new Criteria();
    provider = locationManager.getBestProvider(criteria, false);
    Location location = locationManager.getLastKnownLocation(provider);

    if (location != null) {
      curLatLng = new LatLng(location.getLatitude(), location.getLongitude());
      
      setUpMapIfNeeded();
     
    } else {
      Log.e("KCoderError", "location not obtained");
    }
    //TODO: Check for availability of GooglePlay Services needs to be added. explained in google Location API v2 doc    
    

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
		            if (cI*cI + cJ*cJ >=  (width*width) / 4) {
		            		 icon.setPixel(i, j, desiredAlpha);
		            }		 
		        }
		    }
		    ringImageView.setImageBitmap(icon);
		    
		}
    });
    progressBar = (HoloCircularProgressBar) findViewById(R.id.progress_ring);

    ringImageView.setOnTouchListener(new View.OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
			    progressBarUpdateTask = new progressAnimation().execute();
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				progressBar.setVisibility(View.INVISIBLE);
				if (progressBarUpdateTask != null) {
					progressBarUpdateTask.cancel(true);
				}
			}
			return true;
		}
    }); 
    // Progress bar Setup. Obtained from https://github.com/passsy/android-HoloCircularProgressBar
    
	
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
      // Add lots of markers to the map.
      map.addMarker(new MarkerOptions()
      						.position(curLatLng)
      						.title("You"));

      // Cannot zoom to bounds until the map has a size.
      final View mapView = getSupportFragmentManager().findFragmentById(R.id.map1).getView();
      if (mapView.getViewTreeObserver().isAlive()) {
          mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
            	  // Set radius of visible map to Neighborhood radius
                  float radius = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                		  			.getFloat(DeviceControlActivity.KEY_RADIUS, 5000);
               
                  LatLngBounds bounds = new LatLngBounds.Builder()
		                  .include(new LatLng(curLatLng.latitude, curLatLng.longitude - 
												(radius / (M2LAT * Math.cos(curLatLng.latitude)))))
						  .include(new LatLng(curLatLng.latitude, curLatLng.longitude + 
                		  						(radius / (M2LAT * Math.cos(curLatLng.latitude)))))
                		  .include(new LatLng(curLatLng.latitude + radius / M2LAT, curLatLng.longitude))
                		  .include(new LatLng(curLatLng.latitude - (markerDisplayAdj * radius) / M2LAT, curLatLng.longitude))
                          .build();
                  
                  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                  } else {
                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                  }
                  map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
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

	class progressAnimation extends AsyncTask<Void, Float, Void> {
		private long startTime;
		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
		    progressBar.setWheelSize(30);
		    progressBar.setProgressBackgroundColor(Color.TRANSPARENT);
		    progressBar.setProgressColor(Color.RED);
		    progressBar.setProgress(0f);
		    progressBar.setThumbEnabled(false);
		    progressBar.setMarkerEnabled(false);
		    
			startTime = System.currentTimeMillis();
		}
		@Override
		protected Void doInBackground(Void... params) {
			while (System.currentTimeMillis() - startTime <= progressBarTimeMillis){
				publishProgress((System.currentTimeMillis() - startTime) / progressBarTimeMillis);
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Float... elapsed) {
			progressBar.setProgress(elapsed[0]);
		}
		
		@Override
		protected void onPostExecute(Void res) {
			progressBar.setVisibility(View.INVISIBLE);
			Toast.makeText(getApplicationContext(), "Sending Alert", Toast.LENGTH_SHORT).show();
			
		}
	};

}