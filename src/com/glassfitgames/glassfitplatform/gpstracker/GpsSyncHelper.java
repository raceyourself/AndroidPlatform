package com.glassfitgames.glassfitplatform.gpstracker;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.geq;
import static com.roscopeco.ormdroid.Query.leq;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.utils.ISynchronization;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.roscopeco.ormdroid.Query;

public class GpsSyncHelper extends Thread implements ISynchronization {
	private Context context;
	private long currentSyncTime;
	private final String FAILURE = "failure";
	private final String UNAUTHORIZED = "unauthorized";

	public GpsSyncHelper(Context context, long currentSyncTime) {
		this.context = context;
		this.currentSyncTime = currentSyncTime;
		ORMDroidApplication.initialize(context);
	}

	@Override
	public void run() {
		@SuppressWarnings("unchecked")
		List<Position> positionList = (List<Position>) getData(
				getLastSync(Utils.SYNC_GPS_DATA), currentSyncTime);
		if (positionList.size() > 0) {
			String response = sendDataChanges(positionList, currentSyncTime,
					Utils.POSITION_SYNC_URL);
			boolean syncTimeUpdateFlag = applyChanges(response);
			if (syncTimeUpdateFlag) {
				saveLastSync(Utils.SYNC_GPS_DATA, currentSyncTime);
			}

		}
	}

	@Override
	public long getLastSync(String storedVariableName) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				Utils.SYNC_PREFERENCES, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(storedVariableName, 0);
	}

	// todo if the response contains the json data from server then save
	@Override
	public boolean applyChanges(String response) {
		if (response != null) {
			if (response.equals(FAILURE) || response.equals(UNAUTHORIZED)) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public String sendDataChanges(List<? extends Entity> inputList,
			long modifiedSince, String url) {
		JSONObject root = new JSONObject();
		JSONObject data = new JSONObject();
		JSONArray positions = new JSONArray();
		for (int i = 0; i < inputList.size(); i++) {
			JSONObject subdataobject = new JSONObject();
			try {
				subdataobject.put("track_id",
						((Position) inputList.get(i)).getTrackId());
				subdataobject.put("state_id",
						((Position) inputList.get(i)).getStateId());
				subdataobject.put("ts",
						((Position) inputList.get(i)).getGpsTimestamp()/1000);
				subdataobject.put("lng",
						((Position) inputList.get(i)).getLngx());
				subdataobject.put("lat",
						((Position) inputList.get(i)).getLatx());
				subdataobject.put("alt",
						((Position) inputList.get(i)).getAltitude());
				subdataobject.put("bearing",
						((Position) inputList.get(i)).getBearing());
				subdataobject
						.put("epe", ((Position) inputList.get(i)).getEpe());
				subdataobject.put("nmea",
						((Position) inputList.get(i)).toString());
				positions.put(subdataobject);
			} catch (JSONException exception) {
				throw new RuntimeException(
						"JSON error - couldn't parse the data"
								+ exception.getMessage());
			}
		}
		try {
			data.put("positions", positions);
			root.put("data", data);
		} catch (JSONException e) {
			throw new RuntimeException(
					"JSON error - couldn't parse the data"
							+ e.getMessage());
		}
		HttpResponse response = null;
		try {
			UserDetail ud = UserDetail.get();
			if (ud == null || ud.getApiAccessToken() == null) {
				return UNAUTHORIZED;
			}
			HttpClient httpclient = new DefaultHttpClient();			
			HttpPost httppost = new HttpPost(url+(modifiedSince/1000));
			StringEntity se = new StringEntity(root.toString());
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	        httppost.setEntity(se);
			httppost.setHeader(HTTP.CONTENT_TYPE, "application/json");
			httppost.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
			response = httpclient.execute(httppost);
		} catch (IllegalStateException exception) {
			exception.printStackTrace();
			return FAILURE;
		} catch (IllegalArgumentException exception) {
			exception.printStackTrace();
			return FAILURE;
		} catch (UnsupportedEncodingException exception) {
			exception.printStackTrace();
			return FAILURE;
		} catch (ClientProtocolException exception) {
			exception.printStackTrace();
			return FAILURE;
		} catch (IOException exception) {
			exception.printStackTrace();
			return FAILURE;
		}
		if (response != null) {
			return Utils.httpResponseToString(response);
		} else {
			return FAILURE;
		}
	}

	@Override
	public List<? extends Entity> getData(long lastSyncTime,
			long currentSyncTime) {
		return Query
				.query(Position.class)
				.where(and(geq("gps_ts", lastSyncTime), leq("gps_ts", currentSyncTime)))
				.executeMulti();
	}

	@Override
	public boolean saveLastSync(String storedVariableName, long currentSyncTime) {
		Editor editor = context.getSharedPreferences(Utils.SYNC_PREFERENCES,
				Context.MODE_PRIVATE).edit();
		editor.putLong(storedVariableName, currentSyncTime);
		return editor.commit();
	}

}
