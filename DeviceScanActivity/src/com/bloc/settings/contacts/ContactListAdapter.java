package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import com.bloc.R;

import android.content.Context;
import android.telephony.SmsManager;
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

	public ContactListAdapter(Context context, List<Contact> list) {
		super(context, R.layout.contact_list_row, list);
	    this.context = context;
	    this.contactList = list;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.contact_list_row, parent, false);
	    
	    TextView nameTextView = (TextView) rowView.findViewById(R.id.name_text_view);
	    TextView phoneTextView = (TextView) rowView.findViewById(R.id.phone_text_view);
	    CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check_box);
	    
	    checkBox.setTag(position);
	    Contact contact = contactList.get(position);
	    phoneTextView.setText(Long.toString(contact.phNum));
	    nameTextView.setText(contact.name);
	    checkBox.setChecked(contact.selected);
	    checkBox.setOnCheckedChangeListener(mListener);
	    return rowView;
	}
	
	OnCheckedChangeListener mListener = new OnCheckedChangeListener() {
		 @Override
	     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			 Contact changed = contactList.get((Integer) buttonView.getTag());
			 if (isChecked) {
			 	SmsManager sms = SmsManager.getDefault();
			 	String new_emergency_contact_text = "I've chosen you as an emergency contact on Bloc. "
			 			+ "In an emergency, Bloc will text you my location. "
			 			+ "Bloc is in beta, so ask me to invite you if you want a map.";
			 	sms.sendTextMessage(String.valueOf(changed.phNum), null, new_emergency_contact_text, null, null);
			 	Log.e("HI", "HIHI");
			 }
			 changed.selected = isChecked;
	     }
	};
	
	public ArrayList<Contact> getSelectedContacts(){
		ArrayList<Contact> retList = new ArrayList<Contact>();
		int i;
		for(i=0;i<contactList.size();i++){
			retList.add(contactList.get(i));
		}
		return retList;
	}
}
