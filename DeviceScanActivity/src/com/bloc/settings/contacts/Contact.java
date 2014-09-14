package com.bloc.settings.contacts;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable{
	// TODO These need to be made private
	public String name;
	public long phNum;
	public Boolean selected;
	public Boolean blocMember;
	public String blocID;
	
	public Contact(String name, long phNum){
		this.name = name;
		this.phNum = phNum;
		this.selected = Boolean.FALSE;
		this.blocMember = Boolean.FALSE;
		this.blocID = "none";
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeStringArray(new String[] {this.name, 
										   Long.toString(this.phNum), 
										   Boolean.toString(this.selected),
										   Boolean.toString(this.blocMember),
										   this.blocID});
	}

	public Contact(Parcel in){
		String[] data = new String[5];
		in.readStringArray(data);
		this.name = data[0];
		this.phNum = Long.valueOf(data[1]);
		this.selected = Boolean.valueOf(data[2]);
		this.blocMember = Boolean.valueOf(data[3]);
		this.blocID = data[4];
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