package com.bloc.bluetooth.le;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.cloud.backend.android.CloudEntity;

/**
 * Model object describing a user of this app
 *
 * @author David M. Chandler
 * @author zatricion
 */
public class Person {

	private CloudEntity cloudEntity;

	public static final String KEY_NAME = "name";
	public static final String KEY_PHONE = "telephone";
	public static final String KEY_GEOHASH = "location";
	public static final String KEY_ALERT = "alert";
	public static final String KEY_RADIUS = "radius";

	public Person(String name, String number, String geohash, Boolean alert, Double radius) {
		this.cloudEntity = new CloudEntity("Person");
		this.setName(name);
		this.setPhone(number);
		this.setGeohash(geohash);
		this.setAlert(alert);
		this.setRadius(radius);
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
	
	public String getAlert() {
		return (String) cloudEntity.get(KEY_ALERT);
	}

	public void setAlert(Boolean alert) {
		cloudEntity.put(KEY_ALERT, alert);
	}
	
	public BigDecimal getRadius() {
		return (BigDecimal) cloudEntity.get(KEY_RADIUS);
	}

	public void setRadius(Double radius) {
		cloudEntity.put(KEY_RADIUS, radius);
	}

	public Date getUpdatedAt() {
		return cloudEntity.getUpdatedAt();
	}

}
