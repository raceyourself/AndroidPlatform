package com.glassfitgames.glassfitplatform.gpstracker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.ORMDroidApplication;

public class SyncHelper extends Thread {
	private Context context;
	private long currentSyncTime;
	private final String FAILURE = "failure";
	private final String UNAUTHORIZED = "unauthorized";

	public SyncHelper(Context context, long currentSyncTime) {
		this.context = context;
		this.currentSyncTime = currentSyncTime;
		ORMDroidApplication.initialize(context);
	}

	public void run() {
		Data data = new Data(getLastSync(Utils.SYNC_GPS_DATA), currentSyncTime);
		if (data.hasData() || true) {
			String response = sendDataChanges(data, currentSyncTime,
					Utils.POSITION_SYNC_URL);
			boolean syncTimeUpdateFlag = applyChanges(response);
			if (syncTimeUpdateFlag) {
				saveLastSync(Utils.SYNC_GPS_DATA, currentSyncTime);
			}

		}
	}

	public long getLastSync(String storedVariableName) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				Utils.SYNC_PREFERENCES, Context.MODE_PRIVATE);
		return sharedPreferences.getLong(storedVariableName, 0);
	}

	// todo if the response contains the json data from server then save
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

	public String sendDataChanges(Data data, long modifiedSince, String url)  {
		UserDetail ud = UserDetail.get();
		if (ud == null || ud.getApiAccessToken() == null) {
			return UNAUTHORIZED;
		}
		
		ObjectMapper om = new ObjectMapper();
		om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		
		HttpResponse response = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();			
			HttpPost httppost = new HttpPost(url+(modifiedSince/1000));
			StringEntity se = new StringEntity(om.writeValueAsString(data));
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	        httppost.setEntity(se);
	        // Content-type is sent twice and defaults to text/plain, TODO: fix?
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
			try {
				return IOUtils.toString(response.getEntity().getContent());
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return FAILURE;
			} catch (IOException e) {
				e.printStackTrace();
				return FAILURE;
			}
		} else {
			return FAILURE;
		}
	}

	public boolean saveLastSync(String storedVariableName, long currentSyncTime) {
		Editor editor = context.getSharedPreferences(Utils.SYNC_PREFERENCES,
				Context.MODE_PRIVATE).edit();
		editor.putLong(storedVariableName, currentSyncTime);
		return editor.commit();
	}

	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
	@JsonTypeName("data")
	public static class Data {
		public List<Device> devices;
		public List<Friend> friends;	
		public List<Orientation> orientations;
		public List<Position> positions;
		public List<Track> tracks;
		public List<Transaction> transactions;
		
		public Data(long lastSyncTime, long currentSyncTime) {
			devices = Device.getData(lastSyncTime, currentSyncTime);
			friends = Friend.getData(lastSyncTime, currentSyncTime);
			orientations = Orientation.getData(lastSyncTime, currentSyncTime);
			positions = Position.getData(lastSyncTime, currentSyncTime);
			tracks = Track.getData(lastSyncTime, currentSyncTime);
			transactions = Transaction.getData(lastSyncTime, currentSyncTime);
		}
		
		public boolean hasData() {
			return !(
					devices.isEmpty()
					&& friends.isEmpty()
					&& orientations.isEmpty()
					&& positions.isEmpty()
					&& tracks.isEmpty()
					&& transactions.isEmpty()
					);
		}
	}
	
}
