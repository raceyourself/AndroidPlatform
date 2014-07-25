
package com.raceyourself.platform.models;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

import java.util.List;

import lombok.Getter;

import static com.roscopeco.ormdroid.Query.eql;

/**
 * User model.
 * 
 * Consistency model: Client can do nothing. 
 *                    Server can replace.
 */
public class User extends EntityCollection.CollectionEntity {

    @Getter
    public int id;
    @Getter
    public String email;// The user's email
    @Getter
    public String username;
    @Getter
    public String name; // The user's full name
    @Getter
    public String gender;
    @Getter
    public Integer timezone;
    @Getter
    public int points;
    @Getter
    public int rank;
    public String image;

    public static User get(int id) {
    	return Entity.query(User.class).where(eql("id", id)).execute();
    }

    public String getDisplayName() {
        if (name != null && name.trim().length()> 0) return name;
        if (username != null && username.trim().length()> 0) return username;
        return email;
    }

    public void setAuthentications(List<Authentication> authentications) {
        for (Authentication auth : Authentication.getAuthentications()) {
            auth.delete();
        }
        for (Authentication auth : authentications) {
            auth.save();
        }
    }

    public void setProfile(Profile profile) {
        profile.id = this.id;
        profile.save();
    }

    public Profile getProfile() {
        return query(Profile.class).where(eql("id", this.id)).execute();
    }

    public String getImage() {
        // Protocol-relative URLs
        if (image != null && image.startsWith("//")) return "http:" + image;
        else return image;
    }

    public static class Profile extends Entity {
        public int id;
        public String running_fitness;
    }
}
