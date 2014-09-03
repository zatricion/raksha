package com.bloc.settings.contacts;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
	private boolean isPopup;
	
	public ContactPickerDialog(boolean isPopup) {
		this.isPopup = isPopup;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);
        
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
					intent.putExtra("isPopup", isPopup);
					intent.putParcelableArrayListExtra("contacts", ((ContactListAdapter) contactListView.getAdapter()).getContacts());
					startActivity(intent);
				}
				dismiss();
			}
		});

        return view;
    }
}
