package com.glassfitgames.glassfitplatform.gpstracker;

import static com.roscopeco.ormdroid.Query.eql;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassfitgames.glassfitplatform.models.Action;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.Notification;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
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
		long lastSyncTime = getLastSync(Utils.SYNC_GPS_DATA);
		String response = syncBetween(lastSyncTime, currentSyncTime);
		boolean syncTimeUpdateFlag = applyChanges(response);
		if (syncTimeUpdateFlag) {
			saveLastSync(Utils.SYNC_GPS_DATA, currentSyncTime);
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

	public String syncBetween(long from, long to)  {
		UserDetail ud = UserDetail.get();
		if (ud == null || ud.getApiAccessToken() == null) {
			return UNAUTHORIZED;
		}
		
		ObjectMapper om = new ObjectMapper();
		om.setSerializationInclusion(Include.NON_NULL);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

		// Receive data from:
		String url = Utils.POSITION_SYNC_URL + (from/1000);
		// Transmit data up to:
		Data data = new Data(to);
		
		HttpResponse response = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();			
			HttpPost httppost = new HttpPost(url);
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
				StatusLine status = response.getStatusLine();
				if (status.getStatusCode() == 200) {
					Response newdata = om.readValue(response.getEntity().getContent(), Response.class);
					// Flush transient data from db
					data.flush();
					// Save new data to local db
					newdata.save();
				}				
				return status.getStatusCode()+" "+status.getReasonPhrase();
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
	@JsonTypeName("response")
	public static class Response {
		public long sync_timestamp;
		public List<Device> devices;
		public List<Friend> friends;
		public List<Track> tracks;
		public List<Position> positions;
		public List<Orientation> orientations;
		public List<Transaction> transactions;
		public List<Notification> notifications;
		
		public void save() {
			// NOTE: Race condition with objects dirtied after sync start
			// TODO: Assume dirtied take precedence or merge manually.
			
			if (devices != null) for (Device device : devices) {
				device.save();
			}
			if (friends != null) for (Friend friend : friends) {
				// TODO
				friend.save();
			}
			if (positions != null) for (Position position : positions) {
				// Persist, then flush deleted if needed.
				position.save();
				position.flush();
			}
			if (orientations != null) for (Orientation orientation : orientations) {
				// Persist, then flush deleted if needed.
				orientation.save();
				orientation.flush();
			}
			if (tracks != null) for (Track track : tracks) {
				// Persist, then flush deleted if needed.
				track.save();			
				track.flush();
			}
			if (transactions != null) for (Transaction transaction : transactions) {
				// TODO
				transaction.save();
			}
			if (notifications != null) for (Notification notification : notifications) {
				notification.save();
			}
		}
		
	}
	
	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
	@JsonTypeName("data")
	public static class Data {
		public long sync_timestamp;
		public List<Device> devices;
		public List<Friend> friends;	
		public List<Track> tracks;
		public List<Position> positions;
		public List<Orientation> orientations;
		public List<Transaction> transactions;
		public List<Notification> notifications;
		public List<Action> actions;
		
		public Data(long timestamp) {
			this.sync_timestamp = timestamp;
			// TODO: Generate device_id server-side?
			devices = new ArrayList<Device>();
			devices.add(Device.self());
			// TODO: Send add/deletes where provider = glassfit
			friends = new ArrayList<Friend>();
			// Add/delete
			tracks = Entity.query(Track.class).where(eql("dirty", true)).executeMulti();
			// Add/delete
			positions = Entity.query(Position.class).where(eql("dirty", true)).executeMulti();
			// Add/delete
			orientations = Entity.query(Orientation.class).where(eql("dirty", true)).executeMulti();
			// TODO
			transactions = new ArrayList<Transaction>();
			// Marked read
			notifications = Entity.query(Notification.class).where(eql("dirty", true)).executeMulti();
			// Transmit all actions
			actions = Entity.query(Action.class).executeMulti();
		}
		
		public void flush() {
			// TODO: Friends, delete/replace?
			// TODO: Transactions, delete/replace?
			// Flush dirty objects
			for (Track track : tracks) track.flush();
			for (Position position : positions) position.flush();
			for (Orientation orientation : orientations) orientation.flush();
			// Delete all synced actions
			for (Action action : actions) action.delete();			
		}
		
		public boolean hasData() {
			return !(
					devices.isEmpty()
					&& friends.isEmpty()
					&& tracks.isEmpty()
					&& positions.isEmpty()
					&& orientations.isEmpty()
					&& transactions.isEmpty()
					&& actions.isEmpty()
					);
		}
	}
	
}
