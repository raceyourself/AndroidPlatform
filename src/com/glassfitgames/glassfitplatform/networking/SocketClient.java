package com.glassfitgames.glassfitplatform.networking;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.gpstracker.PositionListener;
import com.glassfitgames.glassfitplatform.gpstracker.StreamedTargetTracker;
import com.glassfitgames.glassfitplatform.models.Position;

public class SocketClient extends GlassFitServerClient implements PositionListener {
    private final Thread looper;
    private final Map<Integer, Map<Integer, StreamedTargetTracker>> racegroups = new HashMap<Integer, Map<Integer, StreamedTargetTracker>>();
    private final List<Integer> streamgroups = new LinkedList<Integer>();
    private final ObjectMapper om;

    public SocketClient(final String accessToken) throws UnknownHostException, IOException {
        super(accessToken.getBytes(), "sockets.raceyourself.com");
     
        om = new ObjectMapper();
        om.setSerializationInclusion(Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        
        looper = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    loop();
                    // TODO: Auto reconnect?
                    // TODO: onDisconnect?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }            
        });
        looper.start();
    }

    @Override
    protected void onUserMessage(int fromUid, ByteBuffer data) {
        String message = new String(data.array(), data.position(), data.remaining());
        JSONObject json = new JSONObject();
        try {
            json.put("from", fromUid);
            try {
                json.put("data", new JSONObject(message));
            } catch (JSONException e) {
                json.put("data", message);
            }
            Helper.message("OnUserMessage", json.toString());
        } catch (JSONException ex) {
            Log.e("SocketClient", "Unexpected error", ex);
        }
    }

    @Override
    protected void onGroupMessage(int fromUid, int fromGid, ByteBuffer data) {
        // In-band data sync
        if (data.get(data.position()) == 'S' && data.get(data.position()+1) == 'Y' && data.get(data.position()+2) == 'N') {
            data.position(data.position()+3);
            try {
                Position position = om.readValue(data.array(), data.position(), data.remaining(), Position.class);
                
                Map<Integer, StreamedTargetTracker> racegroup = racegroups.get(fromGid);
                if (racegroup == null) racegroup = new HashMap<Integer, StreamedTargetTracker>();
                boolean newracer = false;
                StreamedTargetTracker racer = racegroup.get(fromUid);
                if (racer == null) {
                    racer = new StreamedTargetTracker(position);
                    newracer = true;
                } else {
                    long lag = racer.addPosition(position);
                    Log.i("SocketClient", lag + "ms lag for " + fromUid + " #" + fromGid);
                }
                racegroup.put(fromUid, racer);
                racegroups.put(fromGid, racegroup);
                
                if (newracer) {
                    JSONObject json = new JSONObject();
                    json.put("user", fromUid);
                    json.put("group", fromGid);
                    Helper.message("OnRacerConnected", json.toString());                    
                }
            } catch (Exception e) {
                Log.e("SocketClient", "Unexpected error", e);
            }
            return;
        }
        // Unity message
        String message = new String(data.array(), data.position(), data.remaining());
        JSONObject json = new JSONObject();
        try {
            json.put("from", fromUid);
            json.put("group", fromGid);
            try {
                json.put("data", new JSONObject(message));
            } catch (JSONException e) {
                json.put("data", message);
            }
            Helper.message("OnGroupMessage", json.toString());
        } catch (JSONException ex) {
            Log.e("SocketClient", "Unexpected error", ex);
        }
    }

    @Override
    protected void onGroupCreated(int groupId) {
        Helper.message("OnGroupCreation", String.valueOf(groupId));
    }

    @Override
    protected void onPing() {
    }
    
    @Override
    public void leaveGroup(int groupId) {
        super.leaveGroup(groupId);
        stopStreamingToGroup(groupId);
        Map<Integer, StreamedTargetTracker> racegroup = racegroups.remove(groupId);
        if (racegroup != null) racegroup.clear();
    }

    public void startStreamingToGroup(int groupId) {
        streamgroups.add(groupId);
    }

    public void stopStreamingToGroup(int groupId) {
        streamgroups.remove(Integer.valueOf(groupId));
    }
    
    public List<StreamedTargetTracker> getTargetTrackers(int groupId) {
        Map<Integer, StreamedTargetTracker> racegroup = racegroups.get(groupId);
        if (racegroup == null) return new LinkedList<StreamedTargetTracker>();
        return new ArrayList<StreamedTargetTracker>(racegroup.values());
    }
    
    public StreamedTargetTracker getTargetTracker(int groupId, int racerId) {
        Map<Integer, StreamedTargetTracker> racegroup = racegroups.get(groupId);
        if (racegroup == null) return null;
        return racegroup.get(racerId);
    }
    
    public void resetTargetTracker(int groupId, int racerId) {
        Map<Integer, StreamedTargetTracker> racegroup = racegroups.get(groupId);
        if (racegroup != null) racegroup.remove(Integer.valueOf(racerId));
    }

    @Override
    public void onPositionChanged(Position position) {
        if (position.getStateId() < 0) return;
        if (streamgroups.isEmpty()) return;
        try {
            byte[] data = new String("SYN"+om.writeValueAsString(position)).getBytes();
            for (Integer groupId : streamgroups) {
                messageGroup(groupId, data);
            }
        } catch (JsonProcessingException e) {
            Log.e("SocketClient", "Unexpected error", e);
        }
    }
        
}
