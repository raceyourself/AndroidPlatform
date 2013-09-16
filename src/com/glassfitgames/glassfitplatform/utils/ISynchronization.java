package com.glassfitgames.glassfitplatform.utils;

import java.util.List;

import com.roscopeco.ormdroid.Entity;

public interface ISynchronization {

	/**
	 * 
	 * @param storedVariableName
	 *            variable name in the shared preference
	 * @param currentSyncTime
	 *            updates the last sync time with current sync time
	 * @return true if the last sync time is stored in shared preferences
	 */

	public boolean saveLastSync(String storedVariableName, long currentSyncTime);

	/**
	 * 
	 * @param storedVariableName
	 *            variable name in the shared preference
	 * @return last sync time stored in the shared preference
	 */

	public long getLastSync(String storedVariableName);

	/**
	 * 
	 * @param data
	 *            the data to be inserted in the table
	 * @return true if data inserted successfully else false
	 */

	public boolean applyChanges(String data);

	/**
	 * 
	 * @param input
	 *            the json data to be send,
	 * @param modifiedSince
	 *            items that have been updated in server since this time has
	 *            passed,
	 * @param url
	 *            url of the webservices
	 * @return the response of the webservices
	 */

	public String sendDataChanges(List<? extends Entity> input,
			long modifiedSince, String url);

	/**
	 * 
	 * @param lastSyncTime
	 *            the last sync time that is stored in shared preference,
	 * @param currentSyncTime
	 *            current sync time
	 * @return the list of entries from the tables which is greater than or
	 *         equal to lastsyncTime and less than or equal to currentSyncTime
	 */

	public List<? extends Entity> getData(long lastSyncTime,
			long currentSyncTime);

}
