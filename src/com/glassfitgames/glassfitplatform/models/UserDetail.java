package com.glassfitgames.glassfitplatform.models;

import com.roscopeco.ormdroid.Entity;

public class UserDetail extends Entity {

	public int id; // Auto-generated ID
	public String name; // The users full name
	public int age;// The users age
	public String email;// The user's email
	public String photoUri;// Profile photo URI
	public String apiAccessToken; //token to access GlassFit server API. May expire. Use auth.GlassFitAccountAuth to set/refresh.

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

    public String getApiAccessToken() {
        return apiAccessToken;
    }

    public void setApiAccessToken(String apiAccessToken) {
        this.apiAccessToken = apiAccessToken;
    }

}
