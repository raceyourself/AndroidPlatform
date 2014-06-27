package com.raceyourself.platform.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    public String id;
    public String json;

    public Challenge() {
    }
    public Challenge(JsonNode node) {
        this.json = node.toString();
        this.id = node.get("_id").toString();
        if (this.id.contains("$oid")) this.id = node.get("_id").get("$oid").asText();
        this.id = id.replace("\"", "");
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
    
    public static Challenge get(String id) {
        return query(Challenge.class).where(Query.eql("id", id)).execute();        
    }
    
    public static List<Challenge> getPersonalChallenges() {
        EntityCollection defaultCollection = EntityCollection.getDefault();        
        return defaultCollection.getItems(Challenge.class);
    }
    	
}
