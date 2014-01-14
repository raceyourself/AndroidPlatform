package com.glassfitgames.glassfitplatform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

/**
 * An event is an opaque json blob that encapsulates client-side events spawned from Unity.
 * The platform simply queues and transmits them to the server.
 * 
 * Consistency model: Client can add.
 *                    Removed from client when synced to server.
 */
public class Event extends Entity {

	@JsonIgnore
	public int id;
	public long ts;
        @JsonRawValue
	public String data;
	public int version;
	public int device_id;
	public int session_id;

	public Event() {
	}
	
	public Event(String json) {
	    this.ts = System.currentTimeMillis();
	    this.version = Utils.PLATFORM_VERSION;
	    Device self = Device.self();
	    if (self != null) this.device_id = Device.self().getId();
	    else this.device_id = -1;
	    this.session_id = Helper.getInstance(ORMDroidApplication.getInstance().getApplicationContext()).sessionId;
            this.data = json;
	}	
}
