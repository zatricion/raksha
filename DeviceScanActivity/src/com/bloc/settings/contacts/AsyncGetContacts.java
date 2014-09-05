package com.bloc.settings.contacts;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;

public class AsyncGetContacts extends AsyncTask<Void, Void, Void>{
	private ArrayList<Contact> contactList = new ArrayList<Contact>();
	private Context context;
	private ListView contactListView;
	private EditText searchText;
	
	public AsyncGetContacts(Context context, ListView contactListView, EditText searchText){
		this.context = context;
		this.contactListView = contactListView;
		this.searchText = searchText;
	}
	
	@Override
	protected Void doInBackground(Void... args) {
		ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, Phone.DISPLAY_NAME + " ASC");
 
        if (cur.getCount() > 0) {
           while (cur.moveToNext()) {
               String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
               String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
               if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) { 
                    // get the phone number
                   Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                          ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                                          new String[]{id}, null);
                   while (pCur.moveToNext()) {
                         String phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                         try {
                        	 contactList.add(new Contact(name, Long.valueOf(phone.replaceAll("[^\\d.]", ""))));
                         } catch (NumberFormatException e) {
                        	 continue;
                         }
                   }
                   pCur.close();
               }
           }
        }
        cur.close();
        return null;
     }
	protected void onPostExecute(Void result){
		final ContactListAdapter contactAdapter = new ContactListAdapter(context, contactList, searchText);
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
}