package com.glassfitgames.glassfitplatform.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {

	public static final String SYNC_PREFERENCES = "sync_preferences"; // shared
																		// preference
																		// name
																		// to
																		// store
																		// the
																		// last
																		// synced
																		// time
	public static final String SYNC_GPS_DATA = "last_synced_time"; // shared
																	// preference
																	// variable
																	// name for
																	// gps data
	public static final String POSITION_SYNC_URL = "http://glassfit.dannyhawkins.co.uk/api/1/sync/"; // post url for position
														                                             // table


	// Utility method to convert httpresponse into string
	public static String httpResponseToString(HttpResponse response) {
		StringBuilder builder = new StringBuilder();
		BufferedReader bufferedReader;
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			InputStream inputStream = null;
			try {
				inputStream = entity.getContent();
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			bufferedReader = new BufferedReader(new InputStreamReader(
					inputStream));
			try {
				for (String line = null; (line = bufferedReader.readLine()) != null;) {
					builder.append(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return builder.toString();
	}

}
