package com.glassfitgames.glassfitplatform.gpstracker;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;

/**
 * Helper exposes the public methods we'd expect the games to use.
 * The basic features include registering a listener for GPS locations,
 * plus starting and stopping logging of positions to the SQLite database.
 *
 */
public class Helper {
	
    public float getCurrentPace() {
        Log.i("platform.gpstracker.Helper", "getCurrentPace() called");
        return 0f; //TODO - implement this!
    }
    
    public Position getCurrentPosition() {
        Log.i("platform.gpstracker.Helper", "getCurrentPosition() called");
        return new Position(); //TODO - implement this!
    }
    
    public float getTargetPace() {
        Log.i("platform.gpstracker.Helper", "getTargetPace() called");
        return 0f; //TODO - implement this!
    }
        
    public Position getTargetPosition() {
        Log.i("platform.gpstracker.Helper", "getTargetPosition() called");
        return new Position(); //TODO - implement this!
    }
    
    
	public void startLogging() {
	    Log.i("platform.gpstracker.Helper", "startLogging() called");
	}
	
	public void stopLogging() {
	    Log.i("platform.gpstracker.Helper", "stopLogging() called");
	}
	
	public void pauseLogging() {
	    Log.i("platform.gpstracker.Helper", "pauseLogging() called");
	}
	
	
	public void syncToServer() {
	    Log.i("platform.gpstracker.Helper", "syncToServer() called");
	}

}
