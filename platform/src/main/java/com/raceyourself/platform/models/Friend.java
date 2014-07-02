package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

import org.junit.Ignore;

import java.util.Date;
import java.util.List;

import static com.roscopeco.ormdroid.Query.eql;

/**
 * A friend model.
 */
public class Friend extends Entity {

	@Column(unique = true)
	public String id;
    public String provider;
    public String uid;
    public Integer user_id;
    private User user;
    public boolean has_glass;
    public String name;
    public String photo;
    public String screen_name;

	public Date refreshed_at = null;

	public Friend() {
	}
	
	public static List<Friend> getFriends() {
        // NOTE: May require a 'AND not in (select friendId from friendships where deleted_at is not null)' if we delete locally
		return Query.query(Friend.class).executeMulti();
	}
	
    @Override
    public int save() {
        id = this.uid+'@'+this.provider;
        return super.save();
    }

    public boolean includeUser() {
        if (user != null) return true;
        return includeUser(Query.query(User.class).where(eql("id", user_id)).execute());
    }

    public boolean includeUser(User user) {
        this.user = user;
        return (this.user != null);
    }

    public String getDisplayName() {
        if (user != null) return user.getDisplayName();
        if (name != null && name.trim().length() > 0) return name;
        if (screen_name != null && screen_name.trim().length() > 0) return screen_name;
        return id;
    }
}
