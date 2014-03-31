package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;
import android.os.Build;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;

/**
 * A unique device/installation running the app.
 * NOTE: Strict global uniqueness. Used as component in client-generated guids.
 * 
 * Consistency model: Client can add-generate?
 *                    Server can upsert using globally unique primary key.
 */
public class Device extends Entity {

	@JsonProperty("_id")
	public int id;
	public String manufacturer;
	public String model;
	public int glassfit_version;
	public String push_id;
	@JsonIgnore
	public boolean self;

	public Device() {
		manufacturer = Build.MANUFACTURER;
		model = Build.MODEL + "/Gear";
		glassfit_version = Utils.PLATFORM_VERSION;
	}
	
	public static Device self() {		
		return query(Device.class).where(eql("self", true)).execute();
	}

	public int getId() {
		return id;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getModel() {
		return model;
	}

	public int getGlassfit_version() {
		return glassfit_version;
	}

}
