package com.bloc.settings.contacts;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;

import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ContactPickerDialog extends DialogFragment {
	private ListView contactListView;
	private ContactListAdapter contactAdapter;
	private AsyncGetContacts asyncGetContacts;
	private Button doneButton;
	private EditText searchText;
	private boolean isPopup;
	
	public ContactPickerDialog(boolean isPopup) {
		this.isPopup = isPopup;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_selection, container, false);
        
		doneButton = (Button) view.findViewById(R.id.done);
		searchText = (EditText) view.findViewById(R.id.search);
		contactListView = (ListView) view.findViewById(R.id.contact_list_view);
		
		if (DeviceControlActivity.mContactList != null) {
			contactAdapter = new ContactListAdapter(getActivity(), DeviceControlActivity.mContactList, searchText);
			contactListView.setAdapter(contactAdapter);
			searchText.addTextChangedListener(new TextWatcher() {

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before,
						int count) {
					
				}

				@Override
				public void afterTextChanged(Editable s) {
					contactAdapter.getFilter().filter(s.toString());
				}
			});
		}
		else {
			asyncGetContacts = new AsyncGetContacts(getActivity(), contactListView, searchText);
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
		
		searchText.setOnFocusChangeListener(new OnFocusChangeListener() {
		    public void onFocusChange(View v, boolean hasFocus){
	            InputMethodManager imm =  (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	            if(!hasFocus) {
		        	imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		        }
		    }
		});

        return view;
    }
}
