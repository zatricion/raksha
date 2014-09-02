package com.bloc.settings.contacts;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.os.Bundle;
import android.app.Activity;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_few_contacts);
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
                    final List<Contact> oldSelectedContactList = new ArrayList<Contact>();
                    for (Contact contact : oldContactList) {
                    	if (contact.selected) {
                    		oldSelectedContactList.add(contact);
                    	}
                    }
                    for (Contact contact : selectedContacts) {
                    	if (!oldSelectedContactList.contains(contact)) {
                    		// Send text message
            			 	SmsManager sms = SmsManager.getDefault();
            			 	String new_emergency_contact_text = "I've chosen you as an emergency contact on Bloc. "
            			 			+ "In an emergency, Bloc will text you my location. "
            			 			+ "Bloc is in beta, so ask me to invite you if you want a map.";
            			 	sms.sendTextMessage(String.valueOf(contact.phNum), null, new_emergency_contact_text, null, null);
                    	}
                    }
                    
                    // Save contacts to myPrefs
                	SharedPreferences.Editor ed = prefs.edit();
                	String contacts = gson.toJson(contactList);
                	ed.putString(DeviceControlActivity.KEY_CONTACTS, contacts);
                	DeviceControlActivity.mContactList = contactList;
                    ed.commit();
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
