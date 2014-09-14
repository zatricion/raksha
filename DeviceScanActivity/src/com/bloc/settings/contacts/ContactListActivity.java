package com.bloc.settings.contacts;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.bloc.MainWithMapActivity;
import com.bloc.R;
import com.bloc.bluetooth.le.BackgroundService;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class ContactListActivity extends Activity {
	private ArrayList<Contact> contactList;
	private ContactListAdapterNoCheckBox adapter;
	private ListView contactListView;
	private Button confirmButton;
	boolean isPopup;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_few_contacts);
		isPopup = getIntent().getBooleanExtra("isPopup", true);
		contactList = getIntent().getExtras().getParcelableArrayList("contacts");
		contactListView = (ListView) findViewById(R.id.contact_list_view);
		
		// Only show selected contacts
		final List<Contact> selectedContacts = new ArrayList<Contact>();
		for (Contact contact : contactList) {
		  if (contact.selected) {
			  selectedContacts.add(contact);
		  }
		}
		adapter = new ContactListAdapterNoCheckBox(this, selectedContacts);
		contactListView.setAdapter(adapter);
		confirmButton = (Button) findViewById(R.id.confirm);
		
		confirmButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
                if (contactList != null) {
                	// Get old contacts to compare and send intro text messages to new choices
                    SharedPreferences prefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
                    String old_contacts = prefs.getString(DeviceControlActivity.KEY_CONTACTS, null);
                	Gson gson = new Gson();
                    Type collectionType = new TypeToken<ArrayList<Contact>>(){}.getType();
                    List<Contact> oldContactList = gson.fromJson(old_contacts, collectionType);
                    final List<String> oldSelectedContactList = new ArrayList<String>();
                    if (oldContactList != null) {
	                    for (Contact contact : oldContactList) {
	                    	if (contact.selected) {
	                    		oldSelectedContactList.add(String.valueOf(contact.phNum));
	                    	}
	                    }
                    }
                    ArrayList<Contact> notifyContactList = new ArrayList<Contact>();
                    for (Contact contact : selectedContacts) {
                    	String phoneNum = String.valueOf(contact.phNum);
                    	if (!oldSelectedContactList.contains(phoneNum)) {
                    		notifyContactList.add(contact);
                    	}
                    }
                    
                    // Let BackgroundService handle the notifications
        			BackgroundService.mNotifyContacts = notifyContactList;
            		if (isPopup) {
				    	Intent bgServiceIntent = new Intent(getApplicationContext(), BackgroundService.class);
				    	bgServiceIntent.setAction(BackgroundService.ACTION_NOTIFY_CONTACTS);
				    	startService(bgServiceIntent);
            		}

                    // Save contacts to myPrefs
                	SharedPreferences.Editor ed = prefs.edit();
                	String contacts = gson.toJson(contactList);
                	ed.putString(DeviceControlActivity.KEY_CONTACTS, contacts);
                	DeviceControlActivity.mContactList = contactList;
                    ed.commit();
                }
                if (!isPopup) {
            		Intent intent = new Intent(getApplicationContext(), MainWithMapActivity.class);
            		startActivity(intent);
                }
                finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.few_contacts, menu);
		return true;
	}

}
