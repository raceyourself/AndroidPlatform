package com.glassfitgames.glassfitplatform.models;

import com.roscopeco.ormdroid.Entity;
import static com.roscopeco.ormdroid.Query.eql;
import java.sql.Types;

public class Devices extends Entity {

	public int device_id;
	public int user_id;
	public String manufacturer;
	public String model;
	public int glassfit_version;

	public Devices() {

	}
}
