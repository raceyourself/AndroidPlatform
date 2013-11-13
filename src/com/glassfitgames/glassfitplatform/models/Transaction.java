package com.glassfitgames.glassfitplatform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;

/**
 * TODO
 */
public class Transaction extends Entity {

	@JsonIgnore
	public int id;  // for local database only
	
	@JsonProperty("_id")
	@JsonRawValue
	@Column(unique = true)
	public String guid;  // globally unique id
	public long ts; // timestamp of transaction (millisecs since 1970 in device time) TODO: convert to server time on sync
	public String transaction_type;  // e.g. base points, bonus, game purchase
	public String transaction_calc;  // e.g. 1 point * 5 seconds * 1.25 multiplier
	public String source_id;   // game/class that generated the transaction
	public long points_delta;  // points awarded/deducted
	public long points_balance; // sum of points_deltas with timestamps <= current record
	public int cash_delta;     // cash added/removed from GlassFit account
	public String currency;    // currency (e.g. GBP/USD) that the transaction was in
	
	public Transaction() {}  // public constructor with no args required by ORMdroid
	
	public Transaction(String type, String calc, String source_id, int points_delta) {
        this.transaction_type = type;
        this.transaction_calc = calc;
        this.source_id = source_id;
        this.points_delta = points_delta;
        this.ts = System.currentTimeMillis();
	}

	public void setGuid(JsonNode node) {
		this.guid = node.toString();
	}
	
	public static Transaction getLastTransaction() {
	    return Entity.query(Transaction.class).orderBy("ts desc").limit(1).execute();
	}
	
	@Override
	public int save() {
	    Transaction lastTransaction = getLastTransaction();
	    if (lastTransaction == null) {
	        this.points_balance = this.points_delta;
	    } else {
	        this.points_balance = lastTransaction.points_balance + this.points_delta;
	    }
	    
	    return super.save();
	}

}
