package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Action extends Entity {

	@JsonIgnore
	public int id;
	public String json;

	public Action() {
	}
	
	public Action(String json) {
		this.json = json;
	}
	
	public static List<Action> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Action.class)
				.executeMulti();
	}
}
