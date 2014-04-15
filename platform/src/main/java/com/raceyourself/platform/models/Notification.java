package com.raceyourself.platform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Entity;

/**
 * A notification to be displayed to the user.
 * 
 * Consistency model: Client can mark notifications as read
 *                    Server can upsert using server id.
 */
public class Notification extends Entity {

	public int id;
	public boolean read = false;
	@JsonRawValue
	public String message;

	@JsonIgnore
	public boolean dirty = false;
	
	public Notification() {
	}
	
	public static List<Notification> getNotifications() {
		return query(Notification.class).executeMulti();
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		if (this.read != read) dirty = true;
		this.read = read;
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(JsonNode node) {
	    this.message = node.toString();
	}

	public void flush() {
		if (dirty) {
			dirty = false;
			save();
		}
	}
	
}
