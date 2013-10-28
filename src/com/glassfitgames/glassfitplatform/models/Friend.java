package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

/**
 * A friend (relation).
 * 
 * Consistency model: Client can add or delete friend relations where provider = glassfit.
 *                    Client can indirectly effect collections through third-party providers.
 *                    Server can upsert/delete using server id.
 */
public class Friend extends Entity {

	@JsonProperty("_id")
	@JsonRawValue
	@Column(unique = true)
	public String id;
	
	public boolean deleted = false;

	public Friend() {
	}
	
	public static List<Friend> getFriends() {
		return Query.query(Friend.class).executeMulti();
	}
	
	public void setGuid(JsonNode node) {
		this.id = node.toString();
	}

	@Override
	public void delete() {
		deleted = true;
	}
	
	public void flush() {
		if (deleted) {
			super.delete();
			return;
		}
	}
}
