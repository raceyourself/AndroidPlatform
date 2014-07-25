package com.raceyourself.platform.utils;

import android.util.Log;

/**
 * Interface for a message handler
 */
public interface MessageHandler {
    
    public void sendMessage(String target, String method, String message);

}
