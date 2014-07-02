package com.bloc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;

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