package com.raceyourself.platform.models;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;

/**
 * API credentials.
 * 
 * Consistency model: Client can reset.
 *                    Server can replace.
 */
public class AccessToken extends Entity {

	public int id; // Auto-generated ID
	public String apiAccessToken; // token to access GlassFit server API. May expire. Use auth.GlassFitAccountAuth to set/refresh.
	public Date expirationTime; // The expiration datetime for the access token
    public int user_id = 0;

	public AccessToken() {
	}
	
	/**
	 * Gets the access token for the current user.
	 * Expects that only 1 record will exist (only current user is synced to device)
	 * If no records exists, creates a new one.
	 * @return AccessToken for current user
	 */
	public static AccessToken get() {
	    // should only return 1 record!
	    AccessToken ud = query(AccessToken.class).limit(1).execute();
	    if (ud == null) ud = new AccessToken();
	    return ud;
	}

    public int getUserId() {
        return user_id;
    }

    public void setUserId(int userId) {
        user_id = userId;
    }
	
	public String getApiAccessToken() {
    	if (hasExpired()) return null;
        return apiAccessToken;
    }

    public void setApiAccessToken(String apiAccessToken) {
        this.apiAccessToken = apiAccessToken;
    }

    public void resetTokenExpiration() {
        expirationTime = null;
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
