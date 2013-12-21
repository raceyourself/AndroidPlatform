package com.glassfitgames.glassfitplatform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;

/**
 * TODO
 */
public class Calibration extends Entity {

	@JsonIgnore
	public int id;  // for local database only
	
	@JsonProperty("_id")
	@JsonRawValue
	@Column(unique = true)
	public String guid;  // globally unique id

	@Column(unique = true)
	public String name;  // key
	public String unit;  // human readable unit of measurement
	public Integer value;  // value stored as int to avoid float rounding errors
	public Integer decimal_shift; // decimal point shift, e.g. -2 => divide value by 100 to allow for non-in values
	public long last_update_timestamp;  // last time the value was changed
	
	public Calibration() {
	    
	}

	public void setGuid(JsonNode node) {
		this.guid = node.toString();
	}

}
