package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.google.gson.Gson;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
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
		List<Contact> selectedContacts = new ArrayList<Contact>();
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
                	SharedPreferences.Editor ed = getSharedPreferences("myPrefs", MODE_PRIVATE).edit();
                	Gson gson = new Gson();
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
