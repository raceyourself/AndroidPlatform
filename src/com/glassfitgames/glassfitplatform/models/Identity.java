package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.roscopeco.ormdroid.Entity;

public class Identity extends Entity {

	public int id; // Auto-generated ID
	public String provider;
	public String permissions;
	
	public Identity() {
	}    
	
	public static Identity getIdentity(String guid) {
		return query(Identity.class).where(eql("guid",guid)).execute();	
	}	
	
	public static Identity getIdentityByProvider(String provider) {
		return query(Identity.class).where(eql("provider",provider)).execute();	
	}
	
	public static List<Identity> getIdentities() {
		return query(Identity.class).executeMulti();
	}
	
    public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Set<String> getPermissions() {
    	Set<String> set = new HashSet<String>();
    	String[] perms = permissions.split(",");
    	for (String permission : perms) {
    		set.add(permission);
    	}
		return set;
	}

	public boolean hasPermissions(String permissions) {
    	String[] perms = permissions.split(",");
    	return hasPermissions(perms);
    }
    
    public boolean hasPermissions(String... perms) {
    	Set<String> permissions = getPermissions();
    	for(String permission : perms) {
    		if (!permissions.contains(permission)) return false;
    	}
    	return true;
    }
}
