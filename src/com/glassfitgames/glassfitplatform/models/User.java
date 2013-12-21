
package com.glassfitgames.glassfitplatform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;
import com.roscopeco.ormdroid.Column;

/**
 * User model.
 * 
 * Consistency model: Client can do nothing. 
 *                    Server can replace.
 */
public class User extends CollectionEntity {

    @JsonIgnore
    public int id; // Auto-generated ID

    @Column(unique = true)
    @JsonProperty("id")
    public int guid; // Server-generated ID
    public String email;// The user's email
    public String username;
    public String name; // The user's full name

    public int getId() {
        return id;
    }
    public int getGuid() {
        return guid;
    }
    public String getEmail() {
        return email;
    }
    public String getUsername() {
        return username;
    }
    public String getName() {
        return name;
    }

}
