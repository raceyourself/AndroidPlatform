package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.geq;
import static com.roscopeco.ormdroid.Query.leq;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

public class Transaction extends Entity {

	@JsonIgnore
	public int id;
	public int user_id;
	public long ts;
	public int source_id;
	public int product_id;
	public long points_delta;
	public int cash_delta;
	public String currency;

	public Transaction() {

	}

	public static List<Transaction> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Transaction.class)
				.where(and(geq("ts", lastSyncTime), leq("ts", currentSyncTime)))
				.executeMulti();
	}
}
