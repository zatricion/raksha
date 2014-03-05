package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ContactListAdapter extends ArrayAdapter<Contact>{
	private final Context context;
	private final List<Contact> contactList;
	private Boolean[] contactSelectedList;

	public ContactListAdapter(Context context, List<Contact> list) {
		super(context, R.layout.contact_list_row, list);
	    this.context = context;
	    this.contactList = list;
	    this.contactSelectedList = new Boolean[list.size()];
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.contact_list_row, parent, false);
	    
	    TextView nameTextView = (TextView) rowView.findViewById(R.id.name_text_view);
	    TextView phoneTextView = (TextView) rowView.findViewById(R.id.phone_text_view);
	    CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check_box);
	    
	    checkBox.setOnCheckedChangeListener(mListener);
	    checkBox.setTag(position);
	    Contact contact = contactList.get(position);
	    phoneTextView.setText(Long.toString(contact.phNum));
	    nameTextView.setText(contact.name);
	    checkBox.setChecked(contact.selected);
	    return rowView;
	}
	
	OnCheckedChangeListener mListener = new OnCheckedChangeListener() {
		 @Override
	     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			 contactSelectedList[(Integer) buttonView.getTag()] = isChecked;
			 CheckBox checkBox = (CheckBox) buttonView;
			 contactList.get((Integer) buttonView.getTag()).selected = isChecked;
	     }
	};
	
	public ArrayList<Contact> getSelectedContacts(){
		ArrayList<Contact> retList = new ArrayList<Contact>();
		int i;
		for(i=0;i<contactList.size();i++){
			if (contactSelectedList[i] == Boolean.valueOf("true")){
				retList.add(contactList.get(i));
			}
		}
		return retList;
	}
}
