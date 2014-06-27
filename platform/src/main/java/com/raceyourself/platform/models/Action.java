package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.roscopeco.ormdroid.Entity;

/**
 * An action is an opaque json blob that encapsulates server-side actions spawned from Unity.
 * The platform simply queues and transmits them to the server.
 * 
 * Consistency model: Client can add.
 *                    Removed from client when synced to server.
 */
public class Action extends Entity {

	@JsonIgnore
	public int id;
	public String json;

	public Action() {
	}
	
	public Action(String json) {
		this.json = json;
	}
	
	@JsonValue
        @JsonRawValue
	public String toJson() {
	    return json;
	}
	
}
