package com.glassfitgames.glassfitplatform.models;

import java.util.Date;
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
 * Consistency model: Client can add or delete friend relations where user_id != null? :TODO
 *                    Client can indirectly effect collections through third-party providers.
 *                    Server can upsert/delete using server id.
 */
public class Friend extends Entity {

	@JsonProperty("_id")
	@JsonRawValue
	@Column(unique = true)
	public String id;
	@JsonRawValue
	public String friend;
	
	public Date deleted_at = null;

	public Friend() {
	}
	
	public static List<Friend> getFriends() {
		return Query.query(Friend.class).executeMulti();
	}
	
	public void setFriend(JsonNode node) {
	    this.friend = node.toString();
	}
	
	@Override
	public void delete() {
		deleted_at = new Date();
	}
	
	public void flush() {
		if (deleted_at != null) {
			super.delete();
			return;
		}
	}
}
