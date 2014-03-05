package com.bloc.bluetooth.le;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.cloud.backend.android.CloudEntity;

/**
 * Model object describing a geek user of this app
 *
 * @author David M. Chandler
 */
public class Person {

	private CloudEntity cloudEntity;

	public static final String KEY_NAME = "name";
	public static final String KEY_PHONE = "telephone";
	public static final String KEY_GEOHASH = "location";

	public Person(String name, String number, String geohash) {
		this.cloudEntity = new CloudEntity("Person");
		this.setName(name);
		this.setPhone(number);
		this.setGeohash(geohash);
	}

	public Person(CloudEntity e) {
		this.cloudEntity = e;
	}

	public CloudEntity asEntity() {
		return this.cloudEntity;
	}

	public static List<Person> fromEntities(List<CloudEntity> entities) {
		List<Person> geeks = new ArrayList<Person>();
		for (CloudEntity cloudEntity : entities) {
			geeks.add(new Person(cloudEntity));
		}
		return geeks;
	}

	public String getName() {
		return (String) cloudEntity.get(KEY_NAME);
	}

	public void setName(String name) {
		cloudEntity.put(KEY_NAME, name);
	}

	public String getPhone() {
		return (String) cloudEntity.get(KEY_PHONE);
	}

	public void setPhone(String number) {
		cloudEntity.put(KEY_PHONE, number);
	}

	public String getGeohash() {
		return (String) cloudEntity.get(KEY_GEOHASH);
	}

	public void setGeohash(String geohash) {
		cloudEntity.put(KEY_GEOHASH, geohash);
	}

	public Date getUpdatedAt() {
		return cloudEntity.getUpdatedAt();
	}

}
