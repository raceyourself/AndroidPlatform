package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;

/**
 * A challenge.
 * 
 * Consistency model: Client can implicitly create using Actions.
 *                    Server can upsert using id.
 */
public class Challenge extends CollectionEntity {

    @JsonIgnore
    public String id;
    public String json;

    public Challenge() {
    }
    public Challenge(JsonNode node) {
        this.json = node.toString();
        this.id = node.get("_id").toString();
    }
        
    @JsonCreator
    public static Challenge build(JsonNode node) {
        return new Challenge(node);
    }
    
    @JsonValue
    @JsonRawValue
    public String toJson() {
        return json;
    }
    
    public static List<Challenge> getPersonalChallenges() {
        return query(Challenge.class).executeMulti();
    }
    	
}
