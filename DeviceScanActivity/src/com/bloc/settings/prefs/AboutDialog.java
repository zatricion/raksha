package com.bloc.settings.prefs;

import java.util.ArrayList;
import java.util.List;

import com.bloc.MainWithMapActivity;
import com.bloc.NewUserActivity;
import com.bloc.R;
import com.bloc.bluetooth.le.DeviceControlActivity;
import com.bloc.settings.contacts.AsyncGetContacts;
import com.bloc.settings.contacts.ContactListActivity;
import com.bloc.settings.contacts.ContactListAdapter;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class AboutDialog extends DialogFragment {
	private ListView settingsListView;
	
	public AboutDialog() {
		// No-arg constructor required for DialogFragment
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_settings_choice, container);
		settingsListView = (ListView) view.findViewById(R.id.settings_list_view);
		
        List<String> your_array_list = new ArrayList<String>();
        your_array_list.add("Show Welcome Screen");
        your_array_list.add("Quit Application");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, your_array_list);
        settingsListView.setAdapter(arrayAdapter); 		
        
        settingsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Pick the correct dialog to launch
				switch((int) id) {
					case 0:
					    SharedPreferences prefs = getActivity().getSharedPreferences("myPrefs", getActivity().MODE_PRIVATE);
						prefs.edit().putBoolean("new_user", true).commit(); 
						Intent intent = new Intent(getActivity(), NewUserActivity.class);
						startActivity(intent);
						dismiss();
						break;
					case 1:
						((DeviceControlActivity) getActivity()).quitApplication(view);
						dismiss();
						break;
				}
			}
		});

        return view;
    }
}
