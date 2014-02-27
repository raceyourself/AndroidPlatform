package com.glassfitgames.glassfitplatform.utils;

import android.util.Log;

public class ScreenLog {
    
    private String tag = null;
    private StringBuilder log = new StringBuilder();
    
    /**
     * New ScreenLog.
     * 
     * @param tag
     *            The tag to use when echoing messages to the system log. Often
     *            the name of the class to which this ScreenLog belongs.
     */
    public ScreenLog(String tag) {
        this.tag = tag;
    }
     
    public void log(String msg) {
        log.append("\n" + msg);  // append to screenlog
        Log.i(tag, msg);  // echo to system log
    }
    
    public void progress() {
        log.append(".");  // append to screenlog
    }

    public void clear() {
        log = new StringBuilder();
    }
    
    public String get() {
        return log.toString();
    }
}
