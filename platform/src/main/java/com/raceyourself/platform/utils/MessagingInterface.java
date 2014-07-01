package com.raceyourself.platform.utils;

import android.util.Log;

import com.google.android.gms.games.internal.api.NotificationsImpl;

public final class MessagingInterface {
    
    /**
     * Helper method to send a message to another component, eg. unity
     * 
     * @param message to send
     */
    public static void sendMessage(String gameObject, String method, String message) {
        try {
            throw new RuntimeException("TODO: Implement");
            //UnityPlayer.UnitySendMessage(gameObject, method, message);
            //Log.d("GestureHelper", "Sent Unity message: " + gameObject + "." + method + "('" + message + "')");
        } catch (Throwable e) {
            Log.w("GestureHelper",
                    "Failed to send message "
                            + gameObject + "." + method + "('" + message + "')"
                            + " to unity, probably because Unity native libraries aren't available (e.g. you are not running this from Unity).");
        }
    }

}
