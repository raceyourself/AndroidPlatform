package com.glassfitgames.glassfitplatform.models;

import java.sql.Timestamp;

import com.roscopeco.ormdroid.Entity;

public class Transactions extends Entity {

	public int user_id;
	public Timestamp ts;
	public int source_id;
	public int product_id;
	public long points_delta;
	public int cash_delta;
	public char currency;

	public Transactions() {

	}

}
