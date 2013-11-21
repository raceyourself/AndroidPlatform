package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.roscopeco.ormdroid.Entity;

/**
 * A challenge.
 * 
 * Consistency model: Client can implicitly create using Actions.
 *                    Server can upsert using id.
 */
public class Challenge extends Entity {

    @JsonIgnore
    public String id;
    public JsonNode node;

    public Challenge() {
    }
    public Challenge(JsonNode node) {
        this.node = node;
        this.id = node.get("_id").toString();
    }
        
    @JsonCreator
    public static Challenge build(JsonNode node) {
        return new Challenge(node);
    }
    
    @JsonValue
    @JsonRawValue
    public String toJson() {
        return node.toString();
    }
    
    public static List<Challenge> getPersonalChallenges() {
        return query(Challenge.class).executeMulti();
    }
    	
}
