package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import static com.roscopeco.ormdroid.Query.eql;

public class Notification extends Entity {

	@JsonIgnore
	public int id;
	public boolean read = false;
	public String json;

	public Notification() {
	}
	
	public Notification(String json) {
		this.json = json;
	}
	
	public static Notification getNotification(int id) {
		return query(Notification.class).where(eql("id", id)).execute();
	}

	public static List<Notification> getNotifications() {
		return query(Notification.class).executeMulti();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}
}
