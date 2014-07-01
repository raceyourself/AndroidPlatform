package com.raceyourself.platform.models;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Query;

/**
 * A challenge.
 * 
 * Consistency model: Client can implicitly create using Actions.
 *                    Server can upsert using id.
 */
public class Challenge extends EntityCollection.CollectionEntity {

    public int id;
    public Date start_time;
    public Date stop_time;
    @JsonProperty("public")
    public boolean isPublic;
    public int creator_id;
    public int distance;
    public int duration;
    public String name;
    public String description;
    public int points_awarded;
    public String prize;
    public String type;
    // List<Attempt>
    // List<Friend>

    public Date deleted_at;

    public Challenge() {
    }

    public static Challenge get(int id) {
        return query(Challenge.class).where(Query.eql("id", id)).execute();        
    }
    
    public static List<Challenge> getPersonalChallenges() {
        EntityCollection defaultCollection = EntityCollection.getDefault();        
        return defaultCollection.getItems(Challenge.class);
    }
    	
}
