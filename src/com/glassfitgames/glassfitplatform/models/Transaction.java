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
	public int id;
	@JsonProperty("_id")
	@JsonRawValue
	@Column(unique = true)
	public String guid;
	public int user_id;
	public long ts;
	public int source_id;
	public int product_id;
	public long points_delta;
	public int cash_delta;
	public String currency;

	public Transaction() {

	}

	public void setGuid(JsonNode node) {
		this.guid = node.toString();
	}

}
