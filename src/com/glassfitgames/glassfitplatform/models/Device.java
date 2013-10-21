package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import android.os.Build;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;
import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;

public class Device extends Entity {

	@JsonIgnore
	public int id;
	public String manufacturer;
	public String model;
	public int glassfit_version;

	public Device() {
		manufacturer = Build.MANUFACTURER;
		model = Build.MODEL;
		glassfit_version = Utils.PLATFORM_VERSION;
	}
	
	public boolean isNew() {		
		return query(Device.class).where(and(eql("manufacturer", manufacturer), eql("model", model))).execute() == null;
	}
	
	public static List<Device> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Device.class)
				.executeMulti();
	}
}
