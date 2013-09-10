package com.glassfitgames.glassfitplatform.gpstracker;

/**
 * Helper exposes the public methods we'd expect the games to use.
 * The basic features include registering a listener for GPS locations,
 * plus starting and stopping logging of positions to the SQLite database.
 *
 */
public class Helper {
	
	public void registerLocationChangedListener(callback) {}
	
	public void registerOrientationChangedListener(callback) {}
	
	public void startLogging() {}
	
	public void stopLogging() {}
	
	public void pauseLogging() {}

}
