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

public class MainWithMapActivity extends FragmentActivity {
  private GoogleMap map;
  private LocationManager locationManager;
  private String provider;
  private LatLng curLatLng;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fragment_main_with_map);
    LinearLayout topLinearLayout = (LinearLayout) findViewById(R.id.linear_layout_top);
    FrameLayout ringFrameLayout = new FrameLayout(this);
    // Getting display size
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    int width = size.x;
    int height = size.y;
    
    ringFrameLayout.setLayoutParams(new LinearLayout.LayoutParams(width, 0, (float) 3));
    topLinearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    
    Resources res = this.getResources();
    Drawable ring = (GradientDrawable) res.getDrawable(R.drawable.ring);
    ring = ring.mutate();
    ringFrameLayout.setBackground(ring);
    topLinearLayout.addView(ringFrameLayout);
    
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
  }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    getMenuInflater().inflate(R.menu.activity_main, menu);
//    return true;
//  }

} 