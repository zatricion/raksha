package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.widget.ListView;

public class AsyncGetContacts extends AsyncTask<Void, Void, Void>{
	private List<Contact> contactList = new ArrayList<Contact>();
	private Context context;
	private ContactListAdapter contactListAdapter;
	private ListView contactListView;
	
	public AsyncGetContacts(Context context, ListView contactListView){
		this.context = context;
		this.contactListView = contactListView;
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
//                    System.out.println("name : " + name + ", ID : " + id);
 
                    // get the phone number
                   Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                          ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                                          new String[]{id}, null);
                   while (pCur.moveToNext()) {
                         String phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                         System.out.println("phone" + phone);
                         contactList.add(new Contact(name, Long.valueOf(phone.replaceAll("[^\\d.]", ""))));
                   }
                   pCur.close();
               }
           }
        }
        cur.close();
//        contactListAdapter.addAll(contactList);
        return null;
     }
	protected void onPostExecute(Void result){
		contactListView.setAdapter(new ContactListAdapter(context, contactList));
	}
}