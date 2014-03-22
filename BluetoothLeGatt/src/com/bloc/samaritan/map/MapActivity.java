package com.bloc.samaritan.map;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.bloc.R;
import com.bloc.bluetooth.le.BackgroundService;
import com.bloc.bluetooth.le.BluetoothLeService;
import com.bloc.bluetooth.le.Geohasher;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity implements 
				OnCameraChangeListener, OnMyLocationChangeListener {
	private GoogleMap mMap;
	private LatLng victim_loc;
	private boolean mWaitingForLoc;
    private static final String KEY_CURRENT_LOC = "mCurrentLocation";
    private static final String KEY_ZOOM = "zoom";
    
	public static final String VICTIM_LOC = "victim_location";

    private Location mCurrentLocation;

    private static final Geohasher gh = new Geohasher();
    
    private final BroadcastReceiver mapUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BackgroundService.ACTION_UPDATE_MAP.equals(action)) {
            	victim_loc = gh.decode(intent.getStringExtra(VICTIM_LOC));
            } 
            else if (BackgroundService.ACTION_END_ALERT.equals(action)) {
		        Toast.makeText(MapActivity.this, "Bloc Member no longer in danger!", Toast.LENGTH_LONG).show();
		        finish();
            } 
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_map);
            
            Intent intent = getIntent();
            victim_loc = gh.decode(intent.getStringExtra(VICTIM_LOC));
    }
    
    @Override
    protected void onPause() {
            super.onPause();
            unregisterReceiver(mapUpdateReceiver);
            
            // save current location
            SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
            if (mMap != null) {
                    CameraPosition camPos = mMap.getCameraPosition();
                    ed.putString(KEY_CURRENT_LOC, gh.encode(camPos.target));
                    ed.putFloat(KEY_ZOOM, camPos.zoom);
            }
            ed.commit();
    }

	@Override
	protected void onResume() {
		super.onResume();
        registerReceiver(mapUpdateReceiver, makeMapIntentFilter());
        
		setUpMapIfNeeded();
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		String locHash = prefs.getString(KEY_CURRENT_LOC, "9q8yy");
		LatLng camPos = gh.decode(locHash);
		float zoom = prefs.getFloat(KEY_ZOOM, 16f);
		try {
		    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(camPos, zoom));
        } catch (Exception e) {
            // gulp: CameraUpdateFactory not ready if Google Play Services
            // needs to be updated
        }
		this.mWaitingForLoc = true;
	}

	
	private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap == null) {
                // Try to obtain the map from the SupportMapFragment.
                mMap = ((SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map)).getMap();
                // Check if we were successful in obtaining the map.
                if (mMap != null) {
                        setUpMap();
                }
        }
	}
	
    private void setUpMap() {
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(this);
        mMap.setOnMyLocationChangeListener(this);
    }

	@Override
	public void onMyLocationChange(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        // on start or first reliable fix, center the map
        boolean firstGoodFix = mWaitingForLoc && location.getAccuracy() < 30.;
        if (mCurrentLocation == null || firstGoodFix) {
                LatLng myLocation = new LatLng(lat, lon);
                // center map on new location
                mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
        }
        mCurrentLocation = location;

        // Convert my Location to LatLng
        LatLng my_loc = new LatLng(location.getLatitude(), location.getLongitude());
        
        if (firstGoodFix) {
            mWaitingForLoc = false;
        }		
        
        drawMarkers(my_loc, victim_loc);
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
		// TODO Auto-generated method stub	
	}
	
	private void drawMarkers(LatLng myPos, LatLng victimPos) {
		mMap.clear();

		float myMarkerColor = BitmapDescriptorFactory.HUE_AZURE;
		mMap.addMarker(new MarkerOptions()
			.position(myPos)
			.title("Me")
			.icon(BitmapDescriptorFactory.defaultMarker(myMarkerColor)));
		
		float victimMarkerColor = BitmapDescriptorFactory.HUE_RED;		
		mMap.addMarker(new MarkerOptions()
			.position(victimPos)
			.title("HELP")
			.icon(BitmapDescriptorFactory.defaultMarker(victimMarkerColor)));
		
		// TODO: do this better
        double minLat = Integer.MAX_VALUE;
        double maxLat = Integer.MIN_VALUE;
        double minLon = Integer.MAX_VALUE;
        double maxLon = Integer.MIN_VALUE;
        List<LatLng> listPoints = new ArrayList<LatLng>();
        listPoints.add(myPos);
        listPoints.add(victimPos);
        for (LatLng point : listPoints) {
	        maxLat = Math.max(point.latitude, maxLat);
	        minLat = Math.min(point.latitude, minLat);
	        maxLon = Math.max(point.longitude, maxLon);
	        minLon = Math.min(point.longitude, minLon);
	    }
		
        final LatLngBounds bounds = new LatLngBounds.Builder().include(new LatLng(maxLat, maxLon)).include(new LatLng(minLat, minLon)).build();
	    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
        mMap.animateCamera(cameraUpdate);
	}
	
    private static IntentFilter makeMapIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_UPDATE_MAP);
        intentFilter.addAction(BackgroundService.ACTION_END_ALERT);
        return intentFilter;
    }
}
