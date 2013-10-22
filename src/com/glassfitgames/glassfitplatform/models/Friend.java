package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Friend extends Entity {

	@JsonIgnore
	public int id;
	public String guid; // Server-generated id
	public String json;

	public Friend() {
	}
	
	public static List<Friend> getFriends() {
		return Query.query(Friend.class).executeMulti();
	}
	
	public static List<Friend> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Friend.class)
				.executeMulti();
	}
}
