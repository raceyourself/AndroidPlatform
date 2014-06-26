package com.raceyourself.platform.models;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;

/**
 * User details, including API credentials.
 * 
 * Consistency model: Client can update.
 *                    Server can replace.
 */
public class UserDetail extends Entity {

	@JsonIgnore
	public int id; // Auto-generated ID
	@Column(unique = true)
	public int guid; // Server-generated ID
	public String username;
	public String name; // The user's full name
	public int age;
	public int weight; 
	public int height;
	public String email;// The user's email
	public String photoUri;// Profile photo URI
	@JsonIgnore
	public String apiAccessToken; //token to access GlassFit server API. May expire. Use auth.GlassFitAccountAuth to set/refresh.
	@JsonIgnore
	public Date expirationTime; // The expiration datetime for the access token

	public UserDetail() {
	}
	
	/**
	 * Gets the UserDetail record for the current user.
	 * Expects that only 1 record will exist (only current user is synced to device)
	 * If no records exists, creates a new one.
	 * @return UserDetail for current user
	 */
	public static UserDetail get() {
	    // should only return 1 record!
	    UserDetail ud = query(UserDetail.class).limit(1).execute();
	    if (ud == null) ud = new UserDetail();
	    return ud;
	}
	
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}	
	
	public int getGuid() {
		return guid;
	}

	public void setGuid(int guid) {
		this.guid = guid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhotoUri() {
		return photoUri;
	}

	public void setPhotoUri(String photoUri) {
		this.photoUri = photoUri;
	}

	public String getApiAccessToken() {
    	if (hasExpired()) return null;
        return apiAccessToken;
    }

    public void setApiAccessToken(String apiAccessToken) {
        this.apiAccessToken = apiAccessToken;
    }
    
    public void tokenExpiresIn(int seconds) {
    	Calendar cal = new GregorianCalendar();
    	cal.add(Calendar.SECOND, seconds);
    	expirationTime = cal.getTime();
    }
    
    public boolean hasExpired() {
    	if (expirationTime == null) return false;
    	if (new Date().after(expirationTime)) return true;
    	return false;
    }    
    
}
