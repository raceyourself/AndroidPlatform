package com.glassfitgames.glassfitplatform.gpstracker;

import static com.roscopeco.ormdroid.Query.eql;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassfitgames.glassfitplatform.models.Action;
import com.glassfitgames.glassfitplatform.models.Challenge;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.EntityCollection;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;
import com.glassfitgames.glassfitplatform.models.Event;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.Notification;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Preference;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

public class SyncHelper extends Thread {
    private static SyncHelper singleton = null;

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";
    private static final String UNAUTHORIZED = "unauthorized";

    public static synchronized SyncHelper getInstance(Context context) {
        if (singleton == null || !singleton.isAlive())
            singleton = new SyncHelper(context);
        return singleton;
    }

    @Override
    public void start() {
        if (singleton.isAlive())
            return;
        super.start();
    }

    protected SyncHelper(Context context) {
        ORMDroidApplication.initialize(context);
    }

    public void run() {
        Long lastSyncTime = getLastSync(Utils.SYNC_GPS_DATA);
        Long syncTailTime = getLastSync(Utils.SYNC_TAIL_TIME);
        Long syncTailSkip = getLastSync(Utils.SYNC_TAIL_SKIP);
        if ((lastSyncTime == null || lastSyncTime == 0) && syncTailTime == null) { 
            // New full sync: today first, rest later
            lastSyncTime = -1l; //-24l * 60 * 60; // Sync forwards from yesterday 
            syncTailTime = -1l; //-24l * 60 * 60; // Sync backwards from yesterday
        } else if (syncTailTime == null) {
            // Old full sync done: no tail
            syncTailTime = 0l; // Sync backwards from genesis
        }
        if (syncTailSkip == null) syncTailSkip = 0l;
        String result = syncWithServer(lastSyncTime, syncTailTime, syncTailSkip);
        if (!SUCCESS.equals(result)) {
            try {
                UnityPlayer.UnitySendMessage("Platform", "OnSynchronization", "failure");
            } catch (UnsatisfiedLinkError e) {
                Log.i("GlassFitPlatform",
                        "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                Log.i("GlassFitPlatform", e.getMessage());
            }
        }
        Log.i("SyncHelper", "Sync result: " + result);
    }

    public Long getLastSync(String storedVariableName) {
        return Preference.getLong(storedVariableName);
    }

    public boolean saveLastSync(String storedVariableName, long currentSyncTime) {
        return Preference.setLong(storedVariableName, Long.valueOf(currentSyncTime));
    }

    public String syncWithServer(long head, long tail_time, long tail_skip) {
        UserDetail ud = UserDetail.get();
        if (ud == null || ud.getApiAccessToken() == null) {
            if (ud == null)
                Log.i("SyncHelper", "Null user");
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

        Log.i("SyncHelper", "Syncing with server, head: " + new Date(head*1000).toString() + ", tail: " + new Date(tail_time*1000).toString() + "::" + tail_skip);

        // Receive data from:
        String url = Utils.POSITION_SYNC_URL + head;
        // Transmit data up to:
        long stopwatch = System.currentTimeMillis();
        Request request = new Request(head, tail_time, tail_skip);
        Log.i("SyncHelper", "Read from local database in "
                + (System.currentTimeMillis() - stopwatch) + "ms.");

        int connectionTimeoutMillis = 15000;
        int socketTimeoutMillis = 3*60000;

        HttpResponse response = null;
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            try {
                stopwatch = System.currentTimeMillis();
                HttpParams httpParams = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
                HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
                HttpPost httppost = new HttpPost(url);
                StringEntity se = new StringEntity(om.writeValueAsString(request));
                Log.i("SyncHelper", "Uploading " + se.getContentLength() / 1000 + "kB");
                try {
                    UnityPlayer.UnitySendMessage("Platform", "OnSynchronizationProgress", "Uploading "
                            + se.getContentLength() / 1000 + "kB");
                } catch (UnsatisfiedLinkError e) {
                    Log.i("GlassFitPlatform",
                            "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                    Log.i("GlassFitPlatform", e.getMessage());
                }
                // uncomment for debug, can be a very long string:
                // Log.d("SyncHelper","Pushing JSON to server: " +
                // om.writeValueAsString(data));
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httppost.setEntity(se);
                // Content-type is sent twice and defaults to text/plain, TODO: fix?
                httppost.setHeader(HTTP.CONTENT_TYPE, "application/json");
                httppost.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
                Log.i("SyncHelper", "Created HTTP request in "
                        + (System.currentTimeMillis() - stopwatch) + "ms.");
                stopwatch = System.currentTimeMillis();
                AndroidHttpClient.modifyRequestToAcceptGzipResponse(httppost);
                response = httpclient.execute(httppost);
                Log.i("SyncHelper", "Pushed data in " + (System.currentTimeMillis() - stopwatch)
                        + "ms.");
                Log.i("SyncHelper", "Pushed " + request.data.toString());
                try {
                    UnityPlayer
                            .UnitySendMessage("Platform", "OnSynchronizationProgress", "Pushed data");
                } catch (UnsatisfiedLinkError e) {
                    Log.i("GlassFitPlatform",
                            "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                    Log.i("GlassFitPlatform", e.getMessage());
                }
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
                        stopwatch = System.currentTimeMillis();
                        if (response.getEntity().getContentLength() > 0) {
                            Log.i("SyncHelper", "Downloading "
                                    + response.getEntity().getContentLength() / 1000 + "kB");
                            try {
                                UnityPlayer.UnitySendMessage("Platform", "OnSynchronizationProgress",
                                        "Downloading " + response.getEntity().getContentLength() / 1000
                                                + "kB");
                            } catch (UnsatisfiedLinkError e) {
                                Log.i("GlassFitPlatform",
                                        "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                                Log.i("GlassFitPlatform", e.getMessage());
                            }
                        }
    
                        Response newdata = om.readValue(AndroidHttpClient.getUngzippedContent(response.getEntity()),
                                Response.class);
                        Log.i("SyncHelper", "Received " + newdata.toString());
                        Log.i("SyncHelper", "Received data in "
                                + (System.currentTimeMillis() - stopwatch) + "ms.");
                        try {
                            UnityPlayer.UnitySendMessage("Platform", "OnSynchronizationProgress",
                                    "Received data");
                        } catch (UnsatisfiedLinkError e) {
                            Log.i("GlassFitPlatform",
                                    "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                            Log.i("GlassFitPlatform", e.getMessage());
                        }
    
                        // Flush transient data from db
                        stopwatch = System.currentTimeMillis();
                        request.data.flush();
                        Log.i("SyncHelper",
                                "Deleted transient data from local DB in "
                                        + (System.currentTimeMillis() - stopwatch) + "ms.");
    
                        // Save new data to local db
                        stopwatch = System.currentTimeMillis();
                        newdata.save();
                        Log.i("SyncHelper",
                                "Saved remote data to local DB in "
                                        + (System.currentTimeMillis() - stopwatch) + "ms.");
    
                        saveLastSync(Utils.SYNC_GPS_DATA, newdata.sync_timestamp);
                        if (newdata.tail_timestamp != null) {
                            saveLastSync(Utils.SYNC_TAIL_TIME, newdata.tail_timestamp);
                        }
                        if (newdata.tail_skip != null) {
                            saveLastSync(Utils.SYNC_TAIL_SKIP, newdata.tail_skip);
                        }
                        Log.i("SyncHelper", "Stored " + newdata.toString());
                        try {
                            String type = "full";
                            if (newdata.tail_skip != null && newdata.tail_skip > 0) type = "partial";
                            UnityPlayer.UnitySendMessage("Platform", "OnSynchronization", type);
                        } catch (UnsatisfiedLinkError e) {
                            Log.i("GlassFitPlatform",
                                    "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                            Log.i("GlassFitPlatform", e.getMessage());
                        }
                        return SUCCESS;
                    }
                    if (status.getStatusCode() == 401) {
                        // Invalidate access token
                        ud.setApiAccessToken(null);
                        ud.save();
                    }
                    return status.getStatusCode() + " " + status.getReasonPhrase();
                } catch (IllegalStateException e) {
                    Log.e("SyncHelper", "Sync threw an IllegalStateException:", e);
                    return FAILURE;
                } catch (IOException e) {
                    Log.e("SyncHelper", "Sync threw an IOException:", e);
                    return FAILURE;
                }
            } else {
                Log.w("SyncHelper", "No response from API during sync");
                return FAILURE;
            }
        } finally {
            if (httpclient != null) httpclient.close();
        }
    }

    /**
     * Object representation of the JSON data that comes back from the server.
     * 
     * @author Janne Husberg
     * 
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonTypeName("response")
    public static class Response {
        public long sync_timestamp;
        public Long tail_timestamp;
        public Long tail_skip;
        public List<Device> devices;
        public List<Friend> friends;
        public List<Track> tracks;
        public List<Position> positions;
        public List<Orientation> orientations;
        public List<Transaction> transactions;
        public List<Notification> notifications;
        public List<Challenge> challenges;

        /**
         * For each record
         */
        public void save() {
            // NOTE: Race condition with objects dirtied after sync start
            // TODO: Assume dirtied take precedence or merge manually.

            int localDeviceId = Device.self().getId(); // can't query this
                                                       // within transaction
                                                       // below

            try {
                ORMDroidApplication.getInstance().beginTransaction();
                if (devices != null)
                    for (Device device : devices) {
                        if (device.getId() != localDeviceId)
                            device.save();
                    }
                if (friends != null)
                    for (Friend friend : friends) {
                        // TODO
                        friend.save();
                    }
                if (positions != null)
                    for (Position position : positions) {
                        // Persist, then flush deleted if needed.
                        position.save();
                        position.flush();
                    }
                if (orientations != null)
                    for (Orientation orientation : orientations) {
                        // Persist, then flush deleted if needed.
                        orientation.save();
                        orientation.flush();
                    }
                if (tracks != null)
                    for (Track track : tracks) {
                        // Persist, then flush deleted if needed.
                        track.save();
                        track.flush();
                    }
                if (transactions != null)
                    for (Transaction transaction : transactions) {
                        // Persist
                        transaction.store();
                    }
                if (notifications != null)
                    for (Notification notification : notifications) {
                        // Persist, then flush dirty state.
                        notification.save();
                        notification.flush();
                    }
                if (challenges != null)
                    for (Challenge challenge : challenges) {
                        challenge.save();
                    }
                ORMDroidApplication.getInstance().setTransactionSuccessful();
            } finally {
                ORMDroidApplication.getInstance().endTransaction();
            }
        }

        /**
         * String representation of all data held by this class, suitable for
         * log messages / debugging.
         */
        public String toString() {
            StringBuffer buff = new StringBuffer();
            if (devices != null)
                join(buff, devices.size() + " devices");
            if (friends != null)
                join(buff, friends.size() + " friends");
            if (tracks != null)
                join(buff, tracks.size() + " tracks");
            if (positions != null)
                join(buff, positions.size() + " positions");
            if (orientations != null)
                join(buff, orientations.size() + " orientations");
            if (transactions != null)
                join(buff, transactions.size() + " transactions");
            if (notifications != null)
                join(buff, notifications.size() + " notifications");
            if (challenges != null)
                join(buff, challenges.size() + " challenges");
            return buff.toString();
        }

    }

    public static void join(StringBuffer buff, String string) {
        if (buff.length() > 0)
            buff.append(", ");
        buff.append(string);
    }

    public static class Request {
        public long head_ts;
        public long tail_ts;
        public long tail_skip;
        public Data data;
        
        public Request(long head_timestamp, long tail_timestamp, long tail_skip) {
            this.head_ts = head_timestamp;
            this.tail_ts = tail_timestamp;
            this.tail_skip = tail_skip;            
            this.data = new Data();
        }
    }
    
    public static class Data {
        public List<Device> devices;
        public List<Friend> friends;
        public List<Track> tracks;
        public List<Position> positions;
        public List<Orientation> orientations;
        public List<Transaction> transactions;
        public List<Notification> notifications;
        public List<Action> actions;
        public List<Event> events;

        public Data() {
            // NOTE: We assume that any dirtied object is local/in the default
            // entity collection.
            // If this is not the case, we need to filter on entitycollection
            // too.

            devices = new ArrayList<Device>();
            Device self = Device.self();
            devices.add(self);
            // TODO: Send add/deletes where provider = glassfit
            friends = new ArrayList<Friend>();
            // Add/delete
            tracks = Entity.query(Track.class).where(eql("dirty", true)).executeMulti();
            // Add/delete
            positions = Entity.query(Position.class).where(eql("dirty", true)).executeMulti();
            // Add/delete
            orientations = Entity.query(Orientation.class).where(eql("dirty", true)).executeMulti();
            // Add
            transactions = Entity.query(Transaction.class).where(eql("dirty", true)).executeMulti();
            // Marked read
            notifications = Entity.query(Notification.class).where(eql("dirty", true))
                    .executeMulti();
            // Transmit all actions
            actions = Entity.query(Action.class).executeMulti();
            // Transmit all events
            events = Entity.query(Event.class).executeMulti();
            for (Event event : events) {
                if (event.device_id <= 0) event.device_id = self.getId();
            }
        }

        public void flush() {
            // wrapping this in a transaction would make it faster, but
            // potentially block the synchronous db calls from the game for a
            // while
            //
            // TODO: Friends, delete/replace?
            // Flush dirty objects
            for (Track track : tracks)
                track.flush();
            for (Position position : positions)
                position.flush();
            for (Orientation orientation : orientations)
                orientation.flush();
            // Flush client-side transactions. Server will replace them with a verified transaction.
            for (Transaction transaction : transactions) {
                transaction.flush();
            }
            for (Notification notification : notifications)
                notification.flush();
            // Delete all synced actions
            for (Action action : actions)
                action.delete();
            // Delete all synced events
            for (Event event : events)
                event.delete();
        }

        public String toString() {
            StringBuffer buff = new StringBuffer();
            if (devices != null)
                join(buff, devices.size() + " devices");
            if (friends != null)
                join(buff, friends.size() + " friends");
            if (tracks != null)
                join(buff, tracks.size() + " tracks");
            if (positions != null)
                join(buff, positions.size() + " positions");
            if (orientations != null)
                join(buff, orientations.size() + " orientations");
            if (transactions != null)
                join(buff, transactions.size() + " transactions");
            if (notifications != null)
                join(buff, notifications.size() + " notifications");
            if (actions != null)
                join(buff, actions.size() + " actions");
            if (events != null)
                join(buff, events.size() + " events");
            return buff.toString();
        }
    }

    protected static long getMaxAge(final Header[] headers) {
        long maxage = -1;
        for (Header hdr : headers) {
            for (HeaderElement elt : hdr.getElements()) {
                if ("max-age".equals(elt.getName()) || "s-maxage".equals(elt.getName())) {
                    try {
                        long currMaxAge = Long.parseLong(elt.getValue());
                        if (maxage == -1 || currMaxAge < maxage) {
                            maxage = currMaxAge;
                        }
                    } catch (NumberFormatException nfe) {
                        // be conservative if can't parse
                        maxage = 0;
                    }
                }
            }
        }
        return maxage;
    }

    public static <T extends CollectionEntity> T get(String route, Class<T> clz) {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        int connectionTimeoutMillis = 15000;
        int socketTimeoutMillis = 15000;

        EntityCollection cache = EntityCollection.get(route);
        if (!cache.hasExpired() && cache.ttl != 0) {
            Log.i("SyncHelper", "Returning " + clz.getSimpleName() + " from /" + route
                    + " from cache (ttl: " + (cache.ttl - System.currentTimeMillis()) / 1000 + "s)");
            return cache.getItem(clz);
        }

        Log.i("SyncHelper", "Fetching " + clz.getSimpleName() + " from /" + route);
        String url = Utils.API_URL + route;

        HttpResponse response = null;
        UserDetail ud = UserDetail.get();
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            try {
                HttpParams httpParams = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
                HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
                HttpGet httpget = new HttpGet(url);
                if (ud != null && ud.getApiAccessToken() != null) {
                    httpget.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
                }
                response = httpclient.execute(httpget);
            } catch (IOException exception) {
                exception.printStackTrace();
                Log.e("SyncHelper", "GET /" + route + " threw " + exception.getClass().toString() + "/"
                        + exception.getMessage());
                // Return stale value
                return cache.getItem(clz);
            }
            if (response != null) {
                try {
                    StatusLine status = response.getStatusLine();
                    if (status.getStatusCode() == 200) {
                        SingleResponse<T> data = om.readValue(response.getEntity().getContent(), om
                                .getTypeFactory().constructParametricType(SingleResponse.class, clz));
                        long maxAge = getMaxAge(response.getHeaders("Cache-Control"));
                        if (maxAge < 60)
                            maxAge = 60; // TODO: remove?
                        cache.expireIn((int) maxAge);
                        cache.replace(data.response, clz);
                        Log.i("SyncHelper", "Cached /" + route + " for " + maxAge + "s");
                        return data.response;
                    } else {
                        Log.e("SyncHelper", "GET /" + route + " returned " + status.getStatusCode()
                                + "/" + status.getReasonPhrase());
                        if (status.getStatusCode() == 401) {
                            // Invalidate access token
                            ud.setApiAccessToken(null);
                            ud.save();
                        }
                        // Return stale value
                        return cache.getItem(clz);
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                    Log.e("SyncHelper", "GET /" + route + " threw " + exception.getClass().toString()
                            + "/" + exception.getMessage());
                    // Return stale value
                    return cache.getItem(clz);
                }
            } else {
                Log.e("SyncHelper", "No response from API route " + route);
                // Return stale value
                return cache.getItem(clz);
            }
        } finally {
            if (httpclient != null) httpclient.close();
        }
    }

    private static class SingleResponse<T> {
        public T response;
    }

    public static <T extends CollectionEntity> List<T> getCollection(String route, Class<T> clz) {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        int connectionTimeoutMillis = 15000;
        int socketTimeoutMillis = 15000;

        EntityCollection cache = EntityCollection.get(route);
        if (!cache.hasExpired() && cache.ttl != 0) {
            Log.i("SyncHelper", "Fetching " + clz.getSimpleName() + "s from /" + route
                    + " from cache (ttl: " + (cache.ttl - System.currentTimeMillis()) / 1000 + "s)");
            return cache.getItems(clz);
        }

        Log.i("SyncHelper", "Fetching " + clz.getSimpleName() + "s from /" + route);
        String url = Utils.API_URL + route;

        HttpResponse response = null;
        UserDetail ud = UserDetail.get();
        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            try {
                HttpParams httpParams = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
                HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
                HttpGet httpget = new HttpGet(url);
                if (ud != null && ud.getApiAccessToken() != null) {
                    httpget.setHeader("Authorization", "Bearer " + ud.getApiAccessToken());
                }
                response = httpclient.execute(httpget);
            } catch (IOException exception) {
                exception.printStackTrace();
                Log.e("SyncHelper", "GET /" + route + " threw " + exception.getClass().toString() + "/"
                        + exception.getMessage());
                // Return stale value
                return cache.getItems(clz);
            }
            if (response != null) {
                try {
                    StatusLine status = response.getStatusLine();
                    if (status.getStatusCode() == 200) {
                        ListResponse<T> data = om.readValue(response.getEntity().getContent(), om
                                .getTypeFactory().constructParametricType(ListResponse.class, clz));
                        long maxAge = getMaxAge(response.getHeaders("Cache-Control"));
                        if (maxAge < 60)
                            maxAge = 60; // TODO: remove?
                        cache.expireIn((int) maxAge);
                        cache.replace(data.response, clz);
                        Log.i("SyncHelper", "Cached " + data.response.size() + " " + clz.getSimpleName() + "s from /" + route + " for " + maxAge + "s");
                        return data.response;
                    } else {
                        Log.e("SyncHelper", "GET /" + route + " returned " + status.getStatusCode()
                                + "/" + status.getReasonPhrase());
                        if (status.getStatusCode() == 401) {
                            // Invalidate access token
                            ud.setApiAccessToken(null);
                            ud.save();
                        }
                        // Return stale value
                        return cache.getItems(clz);
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                    Log.e("SyncHelper", "GET /" + route + " threw " + exception.getClass().toString()
                            + "/" + exception.getMessage());
                    // Return stale value
                    return cache.getItems(clz);
                }
            } else {
                Log.e("SyncHelper", "No response from API route " + route);
                // Return stale value
                return cache.getItems(clz);
            }
        } finally {
            if (httpclient != null) httpclient.close();
        }
    }

    private static class ListResponse<T> {
        public List<T> response;
    }

    public static Device registerDevice() throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        int connectionTimeoutMillis = 30000;
        int socketTimeoutMillis = 30000;

        Log.i("SyncHelper", "Posting device details to /devices");
        String url = Utils.API_URL + "devices";

        AndroidHttpClient httpclient = AndroidHttpClient.newInstance("GlassfitPlatform/v"+Utils.PLATFORM_VERSION);
        try {
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
            HttpConnectionParams.setSoTimeout(httpParams, socketTimeoutMillis);
            HttpPost httppost = new HttpPost(url);
            // POST device details
            StringEntity se = new StringEntity(om.writeValueAsString(new Device()));
            httppost.setEntity(se);
            // Content-type is sent twice and defaults to text/plain, TODO: fix?
            httppost.setHeader(HTTP.CONTENT_TYPE, "application/json");
            HttpResponse response = httpclient.execute(httppost);
    
            if (response == null)
                throw new IOException("Null response");
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200)
                throw new IOException(status.getStatusCode() + "/" + status.getReasonPhrase());
    
            // Get registered device with guid
            SingleResponse<Device> data = om.readValue(response.getEntity().getContent(), om
                    .getTypeFactory().constructParametricType(SingleResponse.class, Device.class));
    
            if (data == null || data.response == null)
                throw new IOException("Bad response");
    
            return data.response;
        } finally {
            if (httpclient != null) httpclient.close();
        }
    }

    public static synchronized void reset() {
        Log.i("SyncHelper", "Resetting database!");
        Device self = Device.self();
        Context context = ORMDroidApplication.getInstance().getApplicationContext();

        if (singleton != null) {
            try {
                // TODO: Attempt to abort?
                singleton.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ORMDroidApplication.getInstance().resetDatabase();
        ORMDroidApplication.initialize(context);
        Editor editor = context.getSharedPreferences(Utils.SYNC_PREFERENCES, Context.MODE_PRIVATE)
                .edit();
        editor.putLong(Utils.SYNC_GPS_DATA, 0);
        editor.commit();
        if (self != null)
            self.save();
    }
}
