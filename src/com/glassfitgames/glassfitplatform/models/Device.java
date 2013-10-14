package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Device extends Entity {

	@JsonIgnore
	public int id;
	public String manufacturer;
	public String model;
	public int glassfit_version;

	public Device() {

	}
	
	public static List<Device> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Device.class)
				.executeMulti();
	}
}
