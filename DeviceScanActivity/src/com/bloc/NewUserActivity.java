package com.bloc;

import com.bloc.bluetooth.le.BackgroundService;
import com.bloc.settings.contacts.ContactListActivity;
import com.bloc.settings.contacts.ContactListAdapter;
import com.bloc.settings.contacts.ContactPickerDialog;
import com.bloc.settings.prefs.RadiusPickerDialog;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NewUserActivity extends FragmentActivity {
  MyAdapter newUserPagerAdapter;
  ViewPager mViewPager;
	
  static final int NUM_ITEMS = 5;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);

  if (prefs.getBoolean("new_user", true)) {
    // The app is being launched for first time
    setContentView(R.layout.new_user_setup);
    newUserPagerAdapter =
	       new MyAdapter(getSupportFragmentManager());
	mViewPager = (ViewPager) findViewById(R.id.pager);
	mViewPager.setAdapter(newUserPagerAdapter);
	PagerTitleStrip titleStrip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
	titleStrip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
	
    // record the fact that the app has been started at least once
    prefs.edit().putBoolean("new_user", false).commit(); 
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
      public CharSequence getPageTitle (int position) {
    	  switch(position) {
	  	  	case 0:
	  	  		return "Welcome to Bloc";
	  	  	case 1:
	  	  		return "Your Neighborhood";
	  	  	case 2:
	  	  		return "Triggering an Alert";
	  	  	case 3:
	  	  		return "Response Time";
	  	  	case 4:
	  	  		return "Select Emergency Contacts";	  	  		
	  	  }
          return "Bloc";
      }

      @Override
      public Fragment getItem(int position) {
    	  switch(position) {
    	  	case 0:
    	  		return new WelcomeFragment("<br>"
    	  				+ "Thanks for becoming a member of Bloc!"
    	  				+ "<br><br>"
    	  				+ "A bloc is a combination of countries, parties, or groups sharing a common purpose."
    	  				+ "<br><br>"
    	  				+ "In an emergency, Bloc sends your location to "
    	  				+ "your emergency contacts and to all Bloc members in your Neighborhood.");
	  	  	case 1:
		  	  	return new WelcomeFragment("<br>"
		  	  			+ "Your Neighborhood is like a protecting circle that surrounds "
		  	  			+ "you all the time."
		  	  			+ "<br><br>"
		  	  			+ "When you trigger an alert, the phones of all Bloc members in your Neighborhood "
		  	  			+ "will vibrate, and a map to your location will appear."
		  	  			+ "<br><br>"
		  	  			+ "You can change the radius of your Neighborhood in your Settings.");
	  	  	case 2:
		  	  	return new WelcomeFragment("<br>"
		  	  			+ "There are two ways to trigger an alert:"
		  	  			+ "<br><br>"
		  	  			+ "1. Touch and hold your Neighborhood until a red circle comes completely around."
		  	  			+ "<br><br>"
		  	  			+ "2. With your phone in your pocket or purse, hold the power button down until it vibrates once.");
	  	  	case 3:
		  	  	return new WelcomeFragment("<br>"
		  	  			+ "Bad things happen, and sometimes only a few minutes separate life from death."
		  	  			+ "<br><br>"
		  	  			+ "Bloc gives you a way to protect yourself, your loved ones, and your community."
		  	  			+ "<br><br>"
		  	  			+ "On the next page you will select your emergency contacts. Once they are confirmed, your emergency "
		  	  			+ "contacts will receive a text letting them know that they have been chosen.");
		  	case 4:
    	  		return new ContactPickerDialog(false);
    	  }
		return null;
      }
  }
  
  // Fragments for the New User Introduction
  public static class WelcomeFragment extends Fragment {
	    private String htmlText; 
	    
	    public WelcomeFragment(String htmlText) {
	    	this.htmlText = htmlText;
	    }
	    
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
	        // Inflate the layout for this fragment
	        View v = inflater.inflate(R.layout.welcome_view, container, false);
	        TextView text = (TextView) v.findViewById(R.id.welcome_text);
	        text.setText(Html.fromHtml(htmlText));
		    
		    return v;
	    }
	}
}
//
//"<h1> &nbsp;&nbsp;Let's get started:</h1>"
//+ "<h5> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1. Swipe right to select your emergency contacts</h5>"
//+ "<h5> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2. Hit done, then confirm your chosen contacts</h5>"
//+ "<h2> &nbsp;&nbsp;Next you will see a map with a circle &nbsp;&nbsp;around your location.</h2>"
//+ "<h2>&nbsp;&nbsp;This is your Neighborhood. You control &nbsp;&nbsp;the radius of your Neighborhood in &nbsp;&nbsp;your Settings.</h2>"
//+ "<h2>&nbsp;&nbsp;If you long press on your Neighborhood &nbsp;&nbsp;and let the red circle go all the way &nbsp;&nbsp;around, an emergency alert "
//+ "will be sent &nbsp;&nbsp;to all Bloc members inside the &nbsp;&nbsp;Neighborhood, as well as to your &nbsp;&nbsp;emergency contacts, "
//+ "no matter where &nbsp;&nbsp;they are.</h2>"