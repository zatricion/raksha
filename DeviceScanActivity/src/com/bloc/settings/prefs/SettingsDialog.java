package com.bloc.settings.prefs;

import java.util.ArrayList;
import java.util.List;

import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.bloc.settings.contacts.AsyncGetContacts;
import com.bloc.settings.contacts.ContactListActivity;
import com.bloc.settings.contacts.ContactListAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SettingsDialog extends DialogFragment {
	private ListView settingsListView;
	
	public SettingsDialog() {
		// No-arg constructor required for DialogFragment
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_settings_choice, container);
		settingsListView = (ListView) view.findViewById(R.id.settings_list_view);
		
        List<String> your_array_list = new ArrayList<String>();
        your_array_list.add("Set Radius");
        your_array_list.add("Set Emergency Contacts");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, your_array_list);
        settingsListView.setAdapter(arrayAdapter); 		
        
        settingsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Pick the correct dialog to launch
				switch(arrayAdapter.getItem(position)) {
					case "Set Radius":
						((DeviceControlActivity) getActivity()).showRadiusPickerDialog();
						dismiss();
					case "Set Emergency Contacts":
						((DeviceControlActivity) getActivity()).showContactPickerDialog();
						dismiss();
				}
			}
		});

        return view;
    }
}
