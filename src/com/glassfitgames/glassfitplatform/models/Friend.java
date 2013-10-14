package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Friend extends Entity {

	public int id;

	public Friend() {

	}
	
	public static List<Friend> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Friend.class)
				.executeMulti();
	}
}
