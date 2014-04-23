package com.glassfitgames.glassfitplatform.utils;

import android.util.Log;

public class RaceYourselfLog {

    private String tag;
    
    public RaceYourselfLog(String tag) {
        this.tag = tag;
    }
    
    public void verbose(String message) {
        Log.v(tag, message);
    }
    
    public void debug(String message) {
        Log.d(tag, message);
    }
    
    public void info(String message) {
        Log.i(tag, message);
    }
    
    public void warn(String message) {
        Log.w(tag, message);
    }
    
    public void error(String message) {
        Log.e(tag, message);
    }
    
    public void error(String message, Exception e) {
        Log.e(tag, message + "\n" + e.getMessage() + "\n" + e.getStackTrace());
    }
}