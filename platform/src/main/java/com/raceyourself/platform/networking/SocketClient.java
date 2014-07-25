package com.raceyourself.platform.networking;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.raceyourself.platform.gpstracker.Helper;

public class SocketClient extends GlassFitServerClient {
    private final Thread looper;

    public SocketClient(final String accessToken) throws UnknownHostException, IOException {
        super(accessToken.getBytes(), "sockets.raceyourself.com");
        
        looper = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    loop();
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
    
    
}
