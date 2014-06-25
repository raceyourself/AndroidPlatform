package com.glassfitgames.glassfitplatform.utils;

import android.util.Log;

import com.unity3d.player.UnityPlayer;

public final class UnityInterface {
    
    /**
     * Helper method to send a message to unity
     * 
     * @param message
     *            to send
     */
    public static void unitySendMessage(String gameObject, String method, String message) {
        try {
            UnityPlayer.UnitySendMessage(gameObject, method, message);
            Log.d("GestureHelper", "Sent Unity message: " + gameObject + "." + method + "('" + message + "')");
        } catch (UnsatisfiedLinkError e) {
            Log.w("GestureHelper",
                    "Failed to send message "
                            + gameObject + "." + method + "('" + message + "')"
                            + " to unity, probably because Unity native libraries aren't available (e.g. you are not running this from Unity).");
        }
    }

}
