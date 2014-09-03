package com.bloc;

import com.bloc.bluetooth.le.BluetoothLeService;
import com.bloc.settings.contacts.ContactListActivity;
import com.bloc.settings.contacts.ContactListAdapter;
import com.bloc.settings.contacts.ContactPickerDialog;
import com.bloc.settings.prefs.RadiusPickerDialog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NewUserActivity extends FragmentActivity {
  MyAdapter newUserPagerAdapter;
  ViewPager mViewPager;
	
  static final int NUM_ITEMS = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);

if (prefs.getBoolean("new_user", true)) {
    // The app is being launched for first time
//    setContentView(R.layout.new_user_setup);
//    newUserPagerAdapter =
//	       new MyAdapter(getSupportFragmentManager());
//	mViewPager = (ViewPager) findViewById(R.id.pager);
//	mViewPager.setAdapter(newUserPagerAdapter);
	
    // record the fact that the app has been started at least once
    //prefs.edit().putBoolean("new_user", false).commit(); 
	Intent intent = new Intent(this, MainWithMapActivity.class);
	startActivity(intent);

	}
	else {
		Intent intent = new Intent(this, MainWithMapActivity.class);
		startActivity(intent);
	}
  }

  public static class MyAdapter extends FragmentPagerAdapter {
      public MyAdapter(FragmentManager fm) {
          super(fm);
      }

      @Override
      public int getCount() {
          return NUM_ITEMS;
      }

      @Override
      public Fragment getItem(int position) {
    	  switch(position) {
    	  	case 0:
    	  		return new RadiusPickerDialog(false);
    	  	case 1:
    	  		return new ContactPickerDialog();
    	  }
		return null;
      }
  }
}