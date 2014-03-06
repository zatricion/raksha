package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import com.bloc.R;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class ContactPickerDialog extends DialogFragment {
	private ListView contactListView;
	private ContactListAdapter contactListAdapter;
	private AsyncGetContacts asyncGetContacts;
	private Button doneButton;
	
	public ContactPickerDialog() {
		// No-arg constructor required for DialogFragment
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container);
        
		doneButton = (Button) view.findViewById(R.id.done);
		contactListView = (ListView) view.findViewById(R.id.contact_list_view);
		asyncGetContacts = new AsyncGetContacts(getActivity(), contactListView);
		asyncGetContacts.execute();
		
		doneButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getActivity(), ContactListActivity.class);
				intent.putParcelableArrayListExtra("contacts", ((ContactListAdapter) contactListView.getAdapter()).getSelectedContacts());
				startActivity(intent);
			}
		});

        return view;
    }
}
