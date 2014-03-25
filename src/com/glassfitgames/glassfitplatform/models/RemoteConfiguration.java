package com.glassfitgames.glassfitplatform.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;

/**
 * A remote configuration hash.
 * 
 * Consistency model: Client can only fetch.
 */
public class RemoteConfiguration extends CollectionEntity {

	@JsonIgnore
	public int id;
	
	public String type; // Type, usually a device type
    @JsonRawValue
	public String configuration; // Json
	public Date created_at;
	public Date updated_at;
	

	public RemoteConfiguration() {
	}
	
    public String getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(JsonNode node) {
        this.configuration = node.toString();
    }	
}
