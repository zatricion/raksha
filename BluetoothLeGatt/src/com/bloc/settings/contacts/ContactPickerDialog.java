package com.bloc.settings.contacts;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;

import android.os.Bundle;
import android.app.DialogFragment;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class ContactPickerDialog extends DialogFragment {
	private ListView contactListView;
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
		
		if (DeviceControlActivity.mContactList != null) {
			contactListView.setAdapter(new ContactListAdapter(getActivity(), DeviceControlActivity.mContactList));
		}
		else {
			asyncGetContacts = new AsyncGetContacts(getActivity(), contactListView);
			asyncGetContacts.execute();
		}
		
		doneButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if (contactListView.getAdapter() != null) {
					Intent intent = new Intent(getActivity(), ContactListActivity.class);
					intent.putParcelableArrayListExtra("contacts", ((ContactListAdapter) contactListView.getAdapter()).getSelectedContacts());
					startActivity(intent);
				}
				dismiss();
			}
		});

        return view;
    }
}
