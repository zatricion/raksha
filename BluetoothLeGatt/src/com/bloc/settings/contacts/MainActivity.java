package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity {
	private ListView contactListView;
	private ContactListAdapter contactListAdapter;
	private AsyncGetContacts asyncGetContacts;
	private Button doneButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		doneButton = (Button) findViewById(R.id.done);
		contactListView = (ListView) findViewById(R.id.contact_list_view);
		asyncGetContacts = new AsyncGetContacts(this, contactListView);
		asyncGetContacts.execute();
		
		doneButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getApplicationContext(), FewContactsActivity.class);
				intent.putParcelableArrayListExtra("contacts", ((ContactListAdapter) contactListView.getAdapter()).getSelectedContacts());
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
