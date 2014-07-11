package com.bloc;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.gms.R.color;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fragment_main_with_map);
    
    // To create the ring
    LinearLayout ringLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_ring);
    FrameLayout ringFrameLayout = new FrameLayout(this);
    
    ringFrameLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, (float) 3));
    ringLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    
    Resources res = this.getResources();
    Drawable ring = (GradientDrawable) res.getDrawable(R.drawable.ring);
    ring = ring.mutate();
    ringFrameLayout.setBackground(ring);
    ringLinearLayout.addView(ringFrameLayout);
    
    // To create the map
    map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map1))
    		.getMap();
    
    // Get the location manager
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    // Define the criteria how to select the locatioin provider -> use
    Criteria criteria = new Criteria();
    provider = locationManager.getBestProvider(criteria, false);
    Location location = locationManager.getLastKnownLocation(provider);

    if (location != null) {
      curLatLng = new LatLng(location.getLatitude(), location.getLongitude());
      Marker curMarker = map.addMarker(new MarkerOptions().position(curLatLng)
    	        .title("You"));
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(curLatLng, 15));
    } else {
      Log.e("KCoderError", "location not obtained");
    }
    //TODO: Check for availability of GooglePlay Services needs to be added. explained in google Location API v2 docs
    
    // Progress bar Setup. Obtained from https://github.com/passsy/android-HoloCircularProgressBar
    progressBar = (HoloCircularProgressBar) findViewById(R.id.progress_ring);
    progressBar.setProgressBackgroundColor(R.color.red);
    progressBar.setProgressColor(Color.BLACK);
    progressBar.setMarkerProgress(0.25f);//unnecessary line
    progressBar.setProgress(0.4f);
    progressBar.setThumbEnabled(false);
    progressBar.setMarkerEnabled(false);
	
  }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    getMenuInflater().inflate(R.menu.activity_main, menu);
//    return true;
//  }

} 