package com.bloc.settings.contacts;

import java.util.ArrayList;

import com.bloc.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;

public class ContactListActivity extends Activity {
	private ArrayList<Contact> contactList;
	private ContactListAdapterNoCheckBox adapter;
	private ListView contactListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_few_contacts);
		contactList = getIntent().getExtras().getParcelableArrayList("contacts");
		contactListView = (ListView) findViewById(R.id.few_contact_list_view);
		adapter = new ContactListAdapterNoCheckBox(this, contactList);
		contactListView.setAdapter(adapter);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.few_contacts, menu);
		return true;
	}

}
