package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.geq;
import static com.roscopeco.ormdroid.Query.leq;

import java.util.List;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Device extends Entity {

	public int id;
	public String manufacturer;
	public String model;
	public int glassfit_version;
	public long ts;

	public Device() {

	}
	
	public static List<Device> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Device.class)
				.where(and(geq("ts", lastSyncTime), leq("ts", currentSyncTime)))
				.executeMulti();
	}
}
