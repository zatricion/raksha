package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bloc.R;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class ContactListAdapter extends ArrayAdapter<Contact> implements Filterable {
	private final Context context;
	private List<Contact> contactList;
	private List<Contact> filteredContactList;
	private EditText searchText;

	public ContactListAdapter(Context context, List<Contact> list, EditText searchText) {
		super(context, R.layout.contact_list_row, list);
	    this.context = context;
	    this.contactList = list;
	    this.filteredContactList = list;
	    this.searchText = searchText;
	}
	
	@Override
	public int getCount() {
	    return filteredContactList.size();
	}

	@Override
	public View getView(int position, View rowView, ViewGroup parent){
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// TODO: Implement ViewHolder
		if (rowView == null) {
			rowView = inflater.inflate(R.layout.contact_list_row, parent, false);
		}
	    TextView nameTextView = (TextView) rowView.findViewById(R.id.name_text_view);
	    TextView phoneTextView = (TextView) rowView.findViewById(R.id.phone_text_view);
	    CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.check_box);
	    
	    checkBox.setTag(position);
	    Contact contact = filteredContactList.get(position);
	    phoneTextView.setText(Long.toString(contact.phNum));
	    nameTextView.setText(contact.name);
	    checkBox.setChecked(contact.selected);
	    checkBox.setOnClickListener(mListener);
	    return rowView;
	}
	
	OnClickListener mListener = new OnClickListener() {
		 @Override
	     public void onClick(View v) {
			 Contact changed = filteredContactList.get((Integer) v.getTag());
	         boolean checked = ((CheckBox)v).isChecked();
			 changed.selected = checked;

			 if (checked) {
				 ((View) searchText.getParent()).requestFocus();
			 }
	     }
	};
	
	public ArrayList<Contact> getContacts(){
		ArrayList<Contact> retList = new ArrayList<Contact>();
		int i;
		for(i=0;i<contactList.size();i++){
			retList.add(contactList.get(i));
		}
		return retList;
	}
	
	public Filter getFilter() {
        return new Filter() {
        	
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
            	filteredContactList = (List<Contact>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
            	List<Contact> filteredResults = getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                results.values = filteredResults;

                return results;
            }
            
            private List<Contact> getFilteredResults(CharSequence constraint) {
            	List<Contact> filtered = new ArrayList<Contact>();
            	if (contactList == null) {
            		contactList = new ArrayList<Contact>(filteredContactList);
            	}
            	for (Contact contact : contactList) {
            		boolean fullContainsSub = 
            				contact.name.toUpperCase(Locale.getDefault())
            					.indexOf(((String) constraint).toUpperCase(Locale.getDefault())) != -1;
            		if (fullContainsSub) {
            			filtered.add(contact);
            		}
            	}
            	return filtered;
            }
        };
    }
}
