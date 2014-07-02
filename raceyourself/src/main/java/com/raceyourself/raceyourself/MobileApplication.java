package com.raceyourself.raceyourself;

import android.app.Application;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.raceyourself.platform.utils.MessageHandler;
import com.raceyourself.platform.utils.MessagingInterface;
import com.roscopeco.ormdroid.ORMDroidApplication;

import java.util.LinkedList;
import java.util.List;

public class MobileApplication extends Application implements MessageHandler {

    // TODO: Typed callbacks. The platform itself should probably not be sending messages as strings if we aren't using unity.
    private Table<String, String, List<Callback<String>>> callbacks = HashBasedTable.create();

    @Override
    public void onCreate() {
        super.onCreate();
        ORMDroidApplication.initialize(this);
        MessagingInterface.addHandler(this);
    }

    @Override
    public void onTerminate() {
        MessagingInterface.removeHandler(this);
        super.onTerminate();
    }

    @Override
    public void sendMessage(String target, String method, String message) {
        List<Callback<String>> list = callbacks.get(target, method);
        if (list == null) return;
        List<Callback<String>> completed = new LinkedList<Callback<String>>();
        for (Callback<String> callback : list) {
            if (callback.call(message)) {
                completed.add(callback);
            }
        }
        list.removeAll(completed);
    }

    public void addCallback(String target, String method, Callback<String> callback) {
        List<Callback<String>> list = callbacks.get(target, method);
        if (list == null) {
            synchronized(callbacks) {
                if (callbacks.get(target, method) == null) {
                    callbacks.put(target, method, new LinkedList<Callback<String>>());
                }
                list = callbacks.get(target, method);
            }
        }
        list.add(callback);
    }

    public static interface Callback<T> {
        // Call callback
        // a return value of true removes the callback from the handler
        public boolean call(T t);
    }
}
