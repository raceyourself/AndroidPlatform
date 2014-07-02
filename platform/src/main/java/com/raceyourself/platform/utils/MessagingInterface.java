package com.raceyourself.platform.utils;

import android.util.Log;

import com.google.android.gms.games.internal.api.NotificationsImpl;

import java.util.LinkedList;
import java.util.List;

public final class MessagingInterface {

    private static List<MessageHandler> handlers = new LinkedList<MessageHandler>();

    public static void addHandler(MessageHandler handler) {
        handlers.add(handler);
    }

    public static void removeHandler(MessageHandler handler) {
        handlers.remove(handler);
    }

    public static void clearHandlers() {
        handlers.clear();
    }

    /**
     * Helper method to send a message to another component, eg. unity
     * 
     * @param message to send
     */
    public static void sendMessage(String target, String method, String message) {
        for(MessageHandler handler : handlers) handler.sendMessage(target, method, message);
    }

}
