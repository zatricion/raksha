package com.bloc.settings.contacts;

import com.bloc.R;
import java.util.List;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ContactListAdapterNoCheckBox extends ArrayAdapter<Contact>{
	private final Context context;
	private final List<Contact> contactList;

	public ContactListAdapterNoCheckBox(Context context, List<Contact> list) {
		super(context, R.layout.contact_list_row, list);
	    this.context = context;
	    this.contactList = list;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.contact_list_row_no_checkbox, parent, false);
	    
	    TextView nameTextView = (TextView) rowView.findViewById(R.id.name_text_view);
	    TextView phoneTextView = (TextView) rowView.findViewById(R.id.phone_text_view);
	    Contact contact = contactList.get(position);
	    phoneTextView.setText(Long.toString(contact.phNum));
	    nameTextView.setText(contact.name);
	    return rowView;
	}
	
}
