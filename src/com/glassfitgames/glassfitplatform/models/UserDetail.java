package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.sql.Blob;
import java.util.List;

import com.roscopeco.ormdroid.Entity;

public class UserDetail extends Entity {

	public int user_id; // Auto-generated ID
	public String name; // The users full name
	public int age;// The users age
	public String email;// The user's email
	public Blob photo;// Profile photo

	public UserDetail() {

	}

}
