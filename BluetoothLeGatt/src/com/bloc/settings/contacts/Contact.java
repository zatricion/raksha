package com.bloc.settings.contacts;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

public class Contact implements Parcelable{
	// TODO These need to be made private
	public String name;
	public long phNum;
	public Boolean selected;
	
	public Contact(String name, long phNum){
		this.name = name;
		this.phNum = phNum;
		this.selected = Boolean.FALSE;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeStringArray(new String[] {this.name, Long.toString(this.phNum), Boolean.toString(this.selected)});
	}

	public Contact(Parcel in){
		String[] data = new String[3];
		in.readStringArray(data);
		this.name = data[0];
		this.phNum = Long.valueOf(data[1]);
		this.selected = Boolean.valueOf(data[2]);
	}
		
	public static final Parcelable.Creator<Contact> CREATOR =
			new Parcelable.Creator<Contact>() {
		public Contact createFromParcel(Parcel in){
			return new Contact(in);
		}
		
		public Contact[] newArray(int size){
			return new Contact[size];
		}
		
	};
		
}