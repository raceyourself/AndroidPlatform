package com.raceyourself.platform.models;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

import static com.roscopeco.ormdroid.Query.eql;

/**
 * A friend relation.
 * 
 * Consistency model: Client can add or delete friend relations where user_id != null? :TODO
 *                    Client can indirectly effect collections through third-party providers.
 *                    Server can upsert/delete using server id.
 */
public class Friendship extends Entity {

	@Column(unique = true)
	public String id;
    public String identity_type;
    public String identity_uid;
	private String friendId;
	
	public Date deleted_at = null;

	public Friendship() {
	}
	
	public static List<Friendship> getFriendships() {
		return Query.query(Friendship.class).executeMulti();
	}
	
	public void setFriend(Friend friend) {
        friend.save();
	    this.friendId = friend.id;
	}

    public Friend getFriend() {
        return Query.query(Friend.class).where(eql("id", friendId)).execute();
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
            Friend friend = getFriend();
            if (friend != null) friend.delete();
			return;
		}
	}
}
