package com.raceyourself.platform.models;

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

	@Column(unique = true)
	public String id;
    public String identity_type;
    public String identity_uid;
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

    @Override
    public int save() {
        id = this.identity_type + "-" + this.identity_uid;
        return super.save();
    }

	public void flush() {
		if (deleted_at != null) {
			super.delete();
			return;
		}
	}
}
