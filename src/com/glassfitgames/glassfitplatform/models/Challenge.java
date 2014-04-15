package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;
import com.roscopeco.ormdroid.Query;

/**
 * A challenge.
 * 
 * Consistency model: Client can implicitly create using Actions.
 *                    Server can upsert using id.
 */
public class Challenge extends CollectionEntity {

    public int id;
    public String json;

    public Challenge() {
    }
        
    @JsonValue
    @JsonRawValue
    public String toJson() {
        return json;
    }
    
    public static Challenge get(String id) {
        return query(Challenge.class).where(Query.eql("id", id)).execute();        
    }
    
    public static List<Challenge> getPersonalChallenges() {
        EntityCollection defaultCollection = EntityCollection.getDefault();        
        return defaultCollection.getItems(Challenge.class);
    }
    	
}
