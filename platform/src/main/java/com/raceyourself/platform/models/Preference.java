package com.raceyourself.platform.models;

import java.nio.ByteBuffer;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

/**
 * A preference is a (string, 4-byte) tuple stored in the local database
 * 
 * Consistency model: Client can add, update and remove.
 *                    Currently no sync to server
 */
public class Preference extends Entity {

	public int id;
	public String key;
	public ByteBuffer value;

	public Preference() {
	    
	}
	
	public Preference(String key, long value) {
	    this.key = key;
	    this.value = ByteBuffer.allocate(8).putLong(value);
	}
	
	public Preference(String key, Boolean value) {
        this.key = key;
        this.value = ByteBuffer.allocate(8).putInt(value.booleanValue() ? 1 : 0);
    }
	
	public static Long getLong(String key) {
	    Preference p = query(Preference.class).where(Query.eql("key", key)).execute();
	    if (p != null) {
	        return p.value.getLong();
	    } else {
	        return null;
	    }
	}
	
	public static Boolean getBoolean(String key) {
        Preference p = query(Preference.class).where(Query.eql("key", key)).execute();
        if (p != null) {
            return (p.value.getInt() == 1 ? true : false);
        } else {
            return null;
        }
    }
	
    public static boolean setLong(String key, Long value) {
        Preference p = query(Preference.class).where(Query.eql("key", key)).execute();
        if (p != null) {
            p.value = ByteBuffer.allocate(8).putLong(value);
            p.save();
            return true;
        } else {
            new Preference(key, value).save();
            return true;
        }
    }
    
    public static boolean setBoolean(String key, Boolean value) {
        Preference p = query(Preference.class).where(Query.eql("key", key)).execute();
        if (p != null) {
            p.value = ByteBuffer.allocate(8).putInt(value.booleanValue() ? 1 : 0);
            p.save();
            return true;
        } else {
            new Preference(key, value).save();
            return true;
        }
    }
	
}
